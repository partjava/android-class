@echo off
chcp 65001 >nul
title 校园新闻 Flask 本地后端服务

echo ========================================================
echo        今日头条课程设计 - 校园新闻 Flask 后端
echo ========================================================
echo 正在尝试启动 Python 服务...
echo.

where python >nul 2>nul
if %errorlevel% equ 0 (
    python campus_news_server.py
    goto end
)

if exist "D:\anaconda\anaconda\python.exe" (
    "D:\anaconda\anaconda\python.exe" campus_news_server.py
    goto end
)

echo [错误] 未在系统 PATH 或常见路径中找到 python.exe。
echo 请先安装 Python 或将 Python 添加至环境变量。
pause

:end
