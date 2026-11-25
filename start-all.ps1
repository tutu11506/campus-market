# 校园二手交易平台 - 一键启动脚本
# 请双击同目录的「一键启动.bat」运行本文件

# ========== 可配置区（按需修改）==========
$dbPassword      = "luolei_2005"   # MySQL root 密码，需与后端 application.yml 一致
$mysqlService    = "MySQL80"       # MySQL 服务名（服务列表里的名字）
$backendPort     = 8081
$frontendPort    = 5173
$autoOpenBrowser = $true           # 后端就绪后自动打开浏览器
$autoKillPort    = $true           # 自动结束占用端口的 java/node 进程
$waitTimeoutSec  = 360             # 等待后端就绪的最长秒数
# ======================================

$root        = $PSScriptRoot
$backendDir  = Join-Path $root "campus-market"
$frontendDir = Join-Path $root "campus-market-frontend"

function Say-Step($m) { Write-Host ""; Write-Host "==> $m" -ForegroundColor Cyan }
function Say-Ok($m)   { Write-Host "    [OK]   $m" -ForegroundColor Green }
function Say-Note($m) { Write-Host "    [注意] $m" -ForegroundColor Yellow }
function Say-Err($m)  { Write-Host "    [错误] $m" -ForegroundColor Red }

# 1. 运行时检查
Say-Step "检查运行环境（Java / Node.js / npm）"
$missing = @()
try { java -version 2>&1 | Out-Null } catch { $missing += "Java" }
try { node -v | Out-Null } catch { $missing += "Node.js" }
try { npm.cmd -v | Out-Null } catch { $missing += "npm" }
if ($missing.Count -gt 0) {
    Say-Err "缺少运行时：$($missing -join '、')。请先安装并加入系统 PATH。"
    Read-Host "按回车键关闭窗口"; exit 1
}
Say-Ok "Java / Node.js / npm 均可用"

# 2. 目录检查
if (-not (Test-Path (Join-Path $backendDir "mvnw.cmd"))) {
    Say-Err "找不到后端目录：$backendDir"
    Read-Host "按回车键关闭窗口"; exit 1
}
if (-not (Test-Path (Join-Path $frontendDir "package.json"))) {
    Say-Err "找不到前端目录：$frontendDir"
    Read-Host "按回车键关闭窗口"; exit 1
}
Say-Ok "项目目录正常"

# 3. MySQL 服务
Say-Step "检查 MySQL 服务"
$svc = Get-Service -Name $mysqlService -ErrorAction SilentlyContinue
if (-not $svc) {
    $svc = Get-Service -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -like "*mysql*" -or $_.DisplayName -like "*mysql*" } |
           Select-Object -First 1
}
if ($svc) {
    if ($svc.Status -ne "Running") {
        Say-Note "MySQL 未运行，尝试启动（若失败请以管理员身份运行本脚本）..."
        try { Start-Service -Name $svc.Name -ErrorAction Stop; Say-Ok "MySQL 已启动：$($svc.Name)" }
        catch { Say-Err "启动 MySQL 失败：$($_.Exception.Message)" }
    } else { Say-Ok "MySQL 正在运行：$($svc.Name)" }
} else {
    Say-Note "未检测到 MySQL 服务，若后端连不上数据库请手动确认 MySQL 是否已安装并启动"
}

# 4. 释放端口（仅结束 java / node 进程）
function Free-Port($port) {
    $conns = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    foreach ($c in $conns) {
        $procId = $c.OwningProcess
        if (-not $procId -or $procId -eq 0) { continue }
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { "未知" }
        if ($autoKillPort -and ($name -match "java|node")) {
            Say-Note "端口 $port 被 $name (PID=$procId) 占用，自动结束..."
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Start-Sleep -Milliseconds 600
            Say-Ok "已结束进程 $name (PID=$procId)"
        } else {
            Say-Note "端口 $port 被 $name (PID=$procId) 占用（非 java/node，不自动结束），请手动关闭后再启动"
        }
    }
}
Say-Step "检查端口占用（$backendPort / $frontendPort）"
Free-Port $backendPort
Free-Port $frontendPort

# 5. 启动后端（独立窗口，注入数据库密码环境变量）
Say-Step "启动后端（端口 $backendPort）"
$backendCmd = "`$env:DB_PASSWORD='$dbPassword'; Set-Location '$backendDir'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit","-Command",$backendCmd
Say-Ok "后端启动命令已发出（首次运行需下载依赖，可能需要几分钟）"

# 6. 启动前端（独立窗口）
Say-Step "启动前端（端口 $frontendPort）"
$frontendCmd = "Set-Location '$frontendDir'; npm.cmd run dev"
Start-Process powershell -ArgumentList "-NoExit","-Command",$frontendCmd
Say-Ok "前端启动命令已发出"

# 7. 等待后端就绪
Say-Step "等待后端就绪（最长 $waitTimeoutSec 秒）..."
$ready = $false
$maxLoops = [math]::Ceiling($waitTimeoutSec / 2)
for ($i = 0; $i -lt $maxLoops; $i++) {
    Start-Sleep -Seconds 2
    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect("127.0.0.1", $backendPort, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne(1500, $false) -and $client.Connected) { $ready = $true }
    } catch {}
    if ($client) { try { $client.Close() } catch {} }
    if ($ready) { break }
    if (($i % 5) -eq 0 -and $i -gt 0) { Write-Host "    仍在等待后端... ($($i*2)s)" -ForegroundColor DarkGray }
}

# 8. 结果
Write-Host ""
if ($ready) {
    Say-Ok "后端已就绪！"
    if ($autoOpenBrowser) {
        Start-Process "http://localhost:$frontendPort"
        Say-Ok "已打开浏览器：http://localhost:$frontendPort"
    } else {
        Write-Host "    请访问：http://localhost:$frontendPort" -ForegroundColor Green
    }
} else {
    Say-Note "等待后端超时（$waitTimeoutSec 秒），请查看【后端窗口】的日志排查（多半是数据库密码不对或依赖下载失败）"
    Write-Host "    前端窗口已启动，前端就绪后也可访问：http://localhost:$frontendPort" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host " 启动完成：后端、前端分别在独立窗口运行，请勿关闭那些窗口" -ForegroundColor Green
Write-Host " 停止服务：在对应窗口按 Ctrl+C，或直接关闭该窗口" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Read-Host "按回车键关闭本窗口（不影响后端/前端窗口）"