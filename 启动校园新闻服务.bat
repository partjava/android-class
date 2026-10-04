@echo off
chcp 65001 >nul
title 晴川 - 校园新闻服务
cd /d "%~dp0"
echo 晴川校园新闻一键启动
echo 正在打开启动流程，首次缺少依赖时会自动下载，请保持网络连接。
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-campus-news.ps1" %*
set "CAMPUS_EXIT=%errorlevel%"
echo.
echo 启动流程已结束，退出码：%CAMPUS_EXIT%
pause
exit /b %CAMPUS_EXIT%
