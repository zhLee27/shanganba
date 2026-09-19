@echo off
chcp 65001 >nul
title 上岸吧 - 安装到手机

REM 用英文别名路径，避免中文路径在某些终端下乱码
set ADB=C:\Users\Administrator\Documents\Codex\tools\dev\android-sdk\platform-tools\adb.exe
set DIST=C:\Users\Administrator\Documents\Codex\build-links\shanganba\dist
REM 自动挑 dist 里版本号最大的那个 APK（1.2.1 排在 1.2.0 后面）
for %%f in ("%DIST%\*.apk") do set APK=%%~f
if not defined APK set APK=%DIST%\shanganba-1.2.1.apk

echo ============================================
echo   上岸吧 - 一键安装到手机
echo ============================================
echo.
echo 请先确认：
echo   1) 手机已用数据线连上电脑
echo   2) 手机已打开「开发者选项 - USB 调试」
echo   3) 手机上弹出的「允许 USB 调试」点了允许
echo.
echo 将要安装：%APK%
echo.
pause

"%ADB%" devices
echo.
echo 上面列表里如果显示设备（不是 unauthorized），就开始安装：
echo.
"%ADB%" install -r "%APK%"
echo.
echo 如果提示 INSTALL_FAILED_UPDATE_INCOMPATIBLE，
echo 说明手机上装过不同签名的版本，需要先卸载旧版再装。
echo.
pause
