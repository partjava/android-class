@echo off
chcp 65001 >nul
title 晴川 - FastAPI 校园综合服务 (8000端口)
cd /d "%~dp0"
echo 晴川校园 FastAPI 综合服务一键启动 (端口: 8000)
echo 接口文档: http://localhost:8000/docs
echo API 根路径: http://localhost:8000/api/v1
echo 正在检查 Python 环境与依赖，首次缺少依赖时会自动下载，请保持网络连接。
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-campus-fastapi.ps1" %*
set "CAMPUS_EXIT=%errorlevel%"
echo.
echo 启动流程已结束，退出码：%CAMPUS_EXIT%
pause
exit /b %CAMPUS_EXIT%
