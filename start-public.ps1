$ErrorActionPreference = 'Continue'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

$root        = $PSScriptRoot
$frontendDir = Join-Path $root 'campus-market-frontend'
$backendDir  = Join-Path $root 'campus-market'
$ngrok       = Join-Path $env:USERPROFILE '.local\bin\ngrok.exe'
$ngrokCfg    = Join-Path $env:LOCALAPPDATA 'ngrok\ngrok.yml'
$tunnelLog   = Join-Path $env:TEMP 'campus-ngrok.log'
$viteLog     = Join-Path $env:TEMP 'campus-vite.log'

function Test-Port([int]$port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return [bool]$c
}

function Wait-Port([int]$port, [int]$timeoutSec) {
    $end = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $end) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Milliseconds 800
    }
    return (Test-Port $port)
}

# 从 ngrok 本地管理接口读取隧道地址
function Get-TunnelUrl([int]$timeoutSec) {
    $end = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $end) {
        try {
            $r = Invoke-RestMethod -Uri 'http://127.0.0.1:4040/api/tunnels' -TimeoutSec 3
            $u = ($r.tunnels | Where-Object { $_.public_url -like 'https://*' } | Select-Object -First 1).public_url
            if ($u) { return $u }
        } catch {}
        Start-Sleep -Milliseconds 800
    }
    return $null
}

Write-Host ''
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host '  校园二手交易平台 - 一键启动公网访问' -ForegroundColor Red
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host ''

if (!(Test-Path $ngrok)) {
    Write-Host '[1/5] 未找到 ngrok，正在下载...' -ForegroundColor Yellow
    $binDir = Split-Path $ngrok -Parent
    if (!(Test-Path $binDir)) { New-Item -ItemType Directory -Path $binDir | Out-Null }
    $zip = Join-Path $env:TEMP 'ngrok.zip'
    try {
        Invoke-WebRequest -Uri 'https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-windows-amd64.zip' `
            -OutFile $zip -UseBasicParsing -TimeoutSec 300
        Expand-Archive -Path $zip -DestinationPath $binDir -Force
    } catch {}
    if (!(Test-Path $ngrok)) {
        Write-Host '下载失败，请检查网络后重试。' -ForegroundColor Red
        Read-Host '按回车退出'
        exit 1
    }
    Write-Host '      ngrok 下载完成' -ForegroundColor Green
} else {
    Write-Host '[1/5] ngrok 已就绪' -ForegroundColor Green
}

# 首次使用需要写入 authtoken，输入内容只保存在本机 ngrok 配置中
Write-Host '[2/5] 检查 ngrok 账号配置...' -ForegroundColor Cyan
$hasToken = $false
if (Test-Path $ngrokCfg) {
    $hasToken = (Select-String -Path $ngrokCfg -Pattern '^\s*authtoken:' -Quiet -ErrorAction SilentlyContinue)
}
if (!$hasToken) {
    Write-Host ''
    Write-Host '      首次使用需要填写 ngrok 令牌（只输一次，之后自动记住）。' -ForegroundColor Yellow
    Write-Host '      令牌在 https://dashboard.ngrok.com/get-started/your-authtoken 复制。' -ForegroundColor DarkGray
    $token = Read-Host '      请粘贴令牌后按回车'
    if ([string]::IsNullOrWhiteSpace($token)) {
        Write-Host '      未输入令牌，已退出。' -ForegroundColor Red
        Read-Host '按回车退出'
        exit 1
    }
    & $ngrok config add-authtoken $token.Trim() | Out-Null
    Write-Host '      令牌已保存' -ForegroundColor Green
} else {
    Write-Host '      ngrok 账号已配置' -ForegroundColor Green
}

Write-Host '[3/5] 检查后端服务 (8081)...' -ForegroundColor Cyan
if (!(Test-Port 8081)) {
    Write-Host '      正在启动后端，首次启动约需 30-60 秒...' -ForegroundColor DarkGray
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvnw.cmd spring-boot:run' `
        -WorkingDirectory $backendDir -WindowStyle Minimized | Out-Null
    if (!(Wait-Port 8081 180)) {
        Write-Host '      后端启动失败，请查看后端窗口的报错信息。' -ForegroundColor Red
        Read-Host '按回车退出'
        exit 1
    }
}
Write-Host '      后端已就绪' -ForegroundColor Green

Write-Host '[4/5] 检查前端服务 (5173)...' -ForegroundColor Cyan
if (!(Test-Port 5173)) {
    Write-Host '      正在启动前端...' -ForegroundColor DarkGray
    if (Test-Path $viteLog) { Remove-Item $viteLog -Force -ErrorAction SilentlyContinue }
    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', "npm run dev > `"$viteLog`" 2>&1" `
        -WorkingDirectory $frontendDir -WindowStyle Minimized | Out-Null
    if (!(Wait-Port 5173 120)) {
        Write-Host '      前端启动失败，报错信息如下：' -ForegroundColor Red
        if (Test-Path $viteLog) {
            Get-Content $viteLog -Tail 15 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray }
        }
        Read-Host '按回车退出'
        exit 1
    }
}
Write-Host '      前端已就绪' -ForegroundColor Green

Write-Host '[5/5] 创建固定地址隧道...' -ForegroundColor Cyan
# 清理旧隧道，避免争抢同一域名
Get-Process ngrok,cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force -ErrorAction SilentlyContinue }
Start-Process -FilePath $ngrok `
    -ArgumentList 'http', 'http://127.0.0.1:5173', '--url=https://', '--log', $tunnelLog, '--log-format', 'logfmt' `
    -WindowStyle Hidden | Out-Null
$publicUrl = Get-TunnelUrl 40
if (!$publicUrl) {
    Write-Host '      隧道创建失败。' -ForegroundColor Red
    if (Test-Path $tunnelLog) {
        $err = Select-String -Path $tunnelLog -Pattern 'ERR_NGROK_\d+' -ErrorAction SilentlyContinue |
            Select-Object -Last 1
        if ($err) { Write-Host "      原因：$($err.Line)" -ForegroundColor Red }
    }
    Write-Host '      请确认这个域名没有被其他程序占用，或重新运行本脚本。' -ForegroundColor DarkGray
    Read-Host '按回车退出'
    exit 1
}

$ok = $false
$skip = @{ 'ngrok-skip-browser-warning' = '1' }
for ($i = 1; $i -le 8; $i++) {
    try {
        $r1 = Invoke-WebRequest -Uri "$publicUrl/" -Headers $skip -TimeoutSec 20 -UseBasicParsing
        $r2 = Invoke-WebRequest -Uri "$publicUrl/api/home/init?pageSize=1" -Headers $skip -TimeoutSec 20 -UseBasicParsing
        if ($r1.StatusCode -eq 200 -and $r2.StatusCode -eq 200 -and
            $r1.Content -match 'id="app"' -and $r2.Content -match '"code":200') { $ok = $true; break }
    } catch {
        Start-Sleep -Seconds 3
    }
}
if ($ok) {
    Write-Host '      公网访问正常（页面 + 接口）' -ForegroundColor Green
} else {
    Write-Host '      隧道可能还在连接，稍等 10 秒后刷新页面再试。' -ForegroundColor Yellow
}

try { Set-Clipboard -Value $publicUrl } catch {}

Write-Host ''
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host '  固定公网地址（已复制到剪贴板）' -ForegroundColor White
Write-Host "  $publicUrl" -ForegroundColor Yellow
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host ''
Write-Host '注意：' -ForegroundColor DarkGray
Write-Host '  1. 这是固定地址，以后每次启动都不变，可以收藏或分享；' -ForegroundColor DarkGray
Write-Host '  2. 本窗口关闭后服务和隧道仍会继续运行；' -ForegroundColor DarkGray
Write-Host '  3. 电脑关机、休眠或断网后，需要重新运行本脚本；' -ForegroundColor DarkGray
Write-Host '  4. 免费版首次访问会先看到一个英文提示页，点「Visit Site」即可进入；' -ForegroundColor DarkGray
Write-Host '  5. 要关闭访问，双击「停止公网访问.bat」。' -ForegroundColor DarkGray
Write-Host ''

try { Start-Process $publicUrl } catch {}
Read-Host '按回车关闭此窗口'
