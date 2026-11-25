@echo off
chcp 65001 >nul
title 安装 WSL2 + Docker Desktop
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-docker-env.ps1"
pause
