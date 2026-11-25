# MySQL 每日备份：保留最近 7 天，文件名带时间戳
# 用法：powershell -ExecutionPolicy Bypass -File deploy\backup.ps1
# 建议在「任务计划程序」中设为每天 03:00 执行一次
param([int]$KeepDays = 7)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backupDir = Join-Path $PSScriptRoot 'mysql\backup'
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$file = Join-Path $backupDir "campus_market-$stamp.sql"

Push-Location $root
try {
    docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --databases campus_market' |
        Out-File -FilePath $file -Encoding utf8
    Write-Host "备份完成: $file"
}
finally {
    Pop-Location
}

# 清理超过保留期的旧备份
Get-ChildItem $backupDir -Filter '*.sql' |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$KeepDays) } |
    Remove-Item -Force
