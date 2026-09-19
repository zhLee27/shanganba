@echo off
chcp 65001 >nul
title 上岸吧 - 抓崩溃日志

set ADB=C:\Users\Administrator\Documents\Codex\tools\dev\android-sdk\platform-tools\adb.exe
set OUT=%~dp0崩溃日志.txt

echo ============================================
echo   上岸吧 - 抓崩溃日志
echo ============================================
echo.
echo 先确认手机已经用数据线连上电脑、USB 调试已打开。
echo.
"%ADB%" devices
echo.
echo 按任意键清空手机日志缓冲（清空后马上进行下一步）...
pause >nul
"%ADB%" logcat -c

echo.
echo 现在请拿起手机，打开「上岸吧」，点那些会闪退的按钮，复现一两次。
echo 复现完回来按任意键，我就把日志抓下来。
echo.
pause >nul

"%ADB%" logcat -d -v time > "%OUT%"
echo.
echo 日志已保存到：
echo %OUT%
echo.
echo 正在用记事本打开，把里面带 FATAL EXCEPTION 的那几段发给我。
start "" notepad "%OUT%"
echo.
pause
