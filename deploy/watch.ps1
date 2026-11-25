# 实时查看构建日志（只读，关闭此窗口不会影响构建）
$log = Join-Path $PSScriptRoot 'build.log'
Write-Host '=== 构建日志实时输出（关闭窗口不影响构建）===' -ForegroundColor Cyan
if (-not (Test-Path $log)) {
    Write-Host '日志文件还没生成，等待中...' -ForegroundColor Yellow
    while (-not (Test-Path $log)) { Start-Sleep -Seconds 2 }
}
Get-Content -Path $log -Wait -Tail 30
