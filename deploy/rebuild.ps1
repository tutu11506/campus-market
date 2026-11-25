# 按改动自动重建容器：只重建有变化的部分（前端 web / 后端 backend）
# 用法：powershell -ExecutionPolicy Bypass -File deploy\rebuild.ps1 [-Force]
param([switch]$Force)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stampFile = Join-Path $PSScriptRoot '.last-build'
$dockerDir = 'C:\Program Files\Docker\Docker\resources\bin'
if (Test-Path $dockerDir) { $env:PATH = "$dockerDir;$env:PATH" }

if (-not (docker version --format '{{.Server.Version}}' 2>$null)) {
    Write-Host 'Docker 引擎未运行，请先打开 Docker Desktop。' -ForegroundColor Red
    exit 1
}

function Get-NewestWrite {
    param([string[]]$Paths)
    $max = [datetime]'2000-01-01'
    foreach ($p in $Paths) {
        if (-not (Test-Path $p)) { continue }
        $item = Get-Item -LiteralPath $p
        if ($item.PSIsContainer) {
            $files = Get-ChildItem -LiteralPath $p -Recurse -File -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -notmatch '\\node_modules\\|\\target\\|\\dist\\' }
            foreach ($f in $files) { if ($f.LastWriteTime -gt $max) { $max = $f.LastWriteTime } }
        } else {
            if ($item.LastWriteTime -gt $max) { $max = $item.LastWriteTime }
        }
    }
    return $max
}

$backendDir = Join-Path $root 'campus-market'
$frontendDir = Join-Path $root 'campus-market-frontend'

$backendNewest = Get-NewestWrite @(
    (Join-Path $backendDir 'src'),
    (Join-Path $backendDir 'pom.xml'),
    (Join-Path $backendDir 'Dockerfile'),
    (Join-Path $backendDir 'maven-settings.xml')
)

$frontendNewest = Get-NewestWrite @(
    (Join-Path $frontendDir 'src'),
    (Join-Path $frontendDir 'public'),
    (Join-Path $frontendDir 'index.html'),
    (Join-Path $frontendDir 'package.json'),
    (Join-Path $frontendDir 'package-lock.json'),
    (Join-Path $frontendDir 'vite.config.js'),
    (Join-Path $frontendDir 'Dockerfile')
)

# 基准时间：优先取上次构建记录，否则取现有镜像的构建时间
if (Test-Path $stampFile) {
    $lastBuild = [datetime]::Parse((Get-Content $stampFile -Raw).Trim())
} else {
    $lastBuild = [datetime]'2000-01-01'
    foreach ($img in @('campus-market-web:latest', 'campus-market-backend:latest')) {
        $created = docker image inspect $img --format '{{.Created}}' 2>$null
        if ($created) {
            $t = [datetime]::Parse($created).ToLocalTime()
            if ($t -gt $lastBuild) { $lastBuild = $t }
        }
    }
}

$needBackend = $Force -or ($backendNewest -gt $lastBuild)
$needWeb = $Force -or ($frontendNewest -gt $lastBuild)

Write-Host ("后端最近改动：{0}" -f $backendNewest)
Write-Host ("前端最近改动：{0}" -f $frontendNewest)
Write-Host ("上次构建时间：{0}" -f $lastBuild)
Write-Host ''

if (-not $needBackend -and -not $needWeb) {
    Write-Host '没有检测到代码改动，无需重建。' -ForegroundColor Green
    Write-Host '如需强制重建，加参数 -Force。'
    exit 0
}

Set-Location $root
if ($needBackend -and $needWeb) {
    Write-Host '检测到前后端都有改动，全部重建...' -ForegroundColor Cyan
    docker compose up -d --build
} elseif ($needBackend) {
    Write-Host '检测到后端改动，重建 backend...' -ForegroundColor Cyan
    docker compose up -d --build backend
} else {
    Write-Host '检测到前端改动，重建 web...' -ForegroundColor Cyan
    docker compose up -d --build web
}

if ($LASTEXITCODE -eq 0) {
    (Get-Date).ToString('o') | Set-Content -Path $stampFile -Encoding ASCII
    Write-Host ''
    Write-Host '重建完成，访问 http://localhost:8080' -ForegroundColor Green
} else {
    Write-Host '重建失败，请查看上方输出。' -ForegroundColor Red
    exit 1
}
