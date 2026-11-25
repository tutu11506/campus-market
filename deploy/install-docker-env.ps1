# 一键安装 WSL2 + Docker Desktop（可重复运行，按阶段推进）
# 用法：双击同目录的「安装Docker环境.bat」，或右键以管理员身份运行本脚本
$ErrorActionPreference = 'Continue'
$log = Join-Path $PSScriptRoot 'install-docker-env.log'
Start-Transcript -Path $log -Append | Out-Null

function Test-Admin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    (New-Object Security.Principal.WindowsPrincipal($id)).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)
}

# 非管理员：自动提权后退出，外层窗口会等待
if (-not (Test-Admin)) {
    Write-Host '需要管理员权限，正在请求提权（请在弹出的窗口点“是”）...' -ForegroundColor Yellow
    Start-Process powershell -Verb RunAs -Wait -ArgumentList @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`""
    )
    Write-Host '提权窗口已结束。若提示需要重启，请重启后再次运行本脚本。'
    Stop-Transcript | Out-Null
    return
}

Write-Host '=== 步骤 1：检查 WSL 相关 Windows 功能 ===' -ForegroundColor Cyan
$wslInfo = (dism.exe /online /get-featureinfo /featurename:Microsoft-Windows-Subsystem-Linux) -join "`n"
$vmpInfo = (dism.exe /online /get-featureinfo /featurename:VirtualMachinePlatform) -join "`n"

if ($wslInfo -match 'State : Disabled' -or $vmpInfo -match 'State : Disabled') {
    Write-Host '正在启用「适用于 Linux 的 Windows 子系统」与「虚拟机平台」...' -ForegroundColor Yellow
    dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart | Out-Host
    dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart | Out-Host
    Write-Host ''
    Write-Host '功能已启用，需要重启电脑才能继续。' -ForegroundColor Green
    Write-Host '请重启后再次双击「安装Docker环境.bat」。' -ForegroundColor Green
    Stop-Transcript | Out-Null
    Read-Host '按回车关闭窗口'
    return
}

if ($wslInfo -match 'Enable Pending' -or $vmpInfo -match 'Enable Pending') {
    Write-Host 'Windows 功能已启用但尚未生效，请重启电脑后再次运行本脚本。' -ForegroundColor Green
    Stop-Transcript | Out-Null
    Read-Host '按回车关闭窗口'
    return
}

Write-Host 'WSL 功能已就绪。' -ForegroundColor Green

Write-Host ''
Write-Host '=== 步骤 2：安装 / 更新 WSL2 运行时 ===' -ForegroundColor Cyan
$wslOk = $false
try {
    [Console]::OutputEncoding = [Text.Encoding]::Unicode
    $status = (wsl.exe --status 2>&1 | Out-String)
    if ($status -match '默认版本|Default Version') { $wslOk = $true }
} catch { }

if (-not $wslOk) {
    Write-Host '正在安装 WSL 运行时（需要下载，请保持窗口打开）...' -ForegroundColor Yellow
    wsl.exe --install --web-download 2>&1 | Out-Host
}

# 默认使用 WSL2
try {
    wsl.exe --set-default-version 2 2>&1 | Out-Host
} catch { }

Write-Host ''
Write-Host '=== 步骤 3：安装 Docker Desktop ===' -ForegroundColor Cyan
$installer = Join-Path $env:TEMP 'DockerDesktopInstaller.exe'
if (-not (Test-Path $installer)) {
    Write-Host '未找到安装包，正在下载（约 600MB）...' -ForegroundColor Yellow
    Invoke-WebRequest -Uri 'https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe' -OutFile $installer -UseBasicParsing
}

Write-Host '正在静默安装 Docker Desktop（WSL2 后端，需要几分钟）...' -ForegroundColor Yellow
$p = Start-Process -FilePath $installer -Wait -PassThru -ArgumentList @(
    'install', '--quiet', '--accept-license', '--backend=wsl-2'
)
Write-Host "安装进程退出码：$($p.ExitCode)"

$dockerExe = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
if (Test-Path $dockerExe) {
    Write-Host '启动 Docker Desktop...' -ForegroundColor Green
    Start-Process -FilePath $dockerExe
    Write-Host ''
    Write-Host '安装完成。等托盘图标变绿后，回到项目目录执行：' -ForegroundColor Green
    Write-Host '  docker compose up -d' -ForegroundColor White
} else {
    Write-Host '未找到 Docker Desktop 可执行文件，安装可能中断，请查看日志。' -ForegroundColor Red
}

Write-Host ''
Write-Host "日志：$log"
Stop-Transcript | Out-Null
Read-Host '按回车关闭窗口'
