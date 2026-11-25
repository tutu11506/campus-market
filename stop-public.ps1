$ErrorActionPreference = 'Continue'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

function Stop-Port([int]$port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($c) {
        $c | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
        return $true
    }
    return $false
}

Write-Host ''
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host '  校园二手交易平台 - 停止公网访问' -ForegroundColor Red
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host ''

$tunnel = Get-Process ngrok,cloudflared -ErrorAction SilentlyContinue
if ($tunnel) {
    $tunnel | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Host '[1/3] 已关闭公网隧道' -ForegroundColor Green
} else {
    Write-Host '[1/3] 没有正在运行的隧道' -ForegroundColor DarkGray
}

if (Stop-Port 5173) {
    Write-Host '[2/3] 已关闭前端服务 (5173)' -ForegroundColor Green
} else {
    Write-Host '[2/3] 前端服务未运行' -ForegroundColor DarkGray
}

if (Stop-Port 8081) {
    Write-Host '[3/3] 已关闭后端服务 (8081)' -ForegroundColor Green
} else {
    Write-Host '[3/3] 后端服务未运行' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host '公网地址已失效，别人无法再访问。' -ForegroundColor Yellow
Write-Host '需要再次演示时，双击「启动公网访问.bat」即可。' -ForegroundColor DarkGray
Write-Host ''
Read-Host '按回车关闭此窗口'
