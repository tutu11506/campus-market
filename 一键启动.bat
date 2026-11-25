@echo off
chcp 65001 >nul
title Campus Market - Start
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1"
if errorlevel 1 (
    echo.
    echo Script exited with error. Press any key to close...
    pause >nul
)