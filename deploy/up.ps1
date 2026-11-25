# 启动整套服务（构建 + 运行），可直接双击或右键运行以查看实时进度
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$dockerDir = 'C:\Program Files\Docker\Docker\resources\bin'
if (Test-Path $dockerDir) { $env:PATH = "$dockerDir;$env:PATH" }

# 确保 Docker Desktop 已启动
if (-not (Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue)) {
    Write-Host '正在启动 Docker Desktop...'
    Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $v = & docker version --format '{{.Server.Version}}' 2>$null
        if ($v) { Write-Host "Docker 引擎已就绪：$v"; break }
    }
}

Write-Host ''
Write-Host '开始构建并启动容器（首次构建需要下载依赖，请耐心等待）...' -ForegroundColor Cyan
Set-Location $root
docker compose up -d --build

Write-Host ''
docker compose ps
Write-Host ''
Write-Host '访问地址：http://localhost:8080' -ForegroundColor Green
Read-Host '按回车关闭窗口'
