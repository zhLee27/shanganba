@echo off
chcp 65001 >nul
title 上岸吧 - 上传到 GitHub Release

set PY=D:\python\python.exe
set SCRIPT=%~dp0upload_release.py

echo ============================================
echo   上岸吧 - 上传 APK 到 GitHub Release
echo ============================================
echo.
echo 需要的两样东西：
echo   1) 仓库地址，格式 用户名/仓库名，例如 lzh/shanganba
echo   2) token 请提前写进 tools\upload-token.txt（这个文件不会被提交）
echo      这样 token 不会出现在聊天记录和命令历史里
echo.
set /p REPO=请输入仓库（用户名/仓库名）:
set /p TOKEN=如需临时输入 Token 可粘贴（直接回车则用 upload-token.txt）:
echo.
set /p CREATEFLAG=仓库不存在时自动新建？(y/N):
set EXTRA=
if /i "%CREATEFLAG%"=="y" set EXTRA=--create-repo
set TOKENARG=
if not "%TOKEN%"=="" set TOKENARG=--token "%TOKEN%"
echo.
"%PY%" "%SCRIPT%" --repo "%REPO%" --tag v1.2.1 %TOKENARG% %EXTRA%
echo.
pause
