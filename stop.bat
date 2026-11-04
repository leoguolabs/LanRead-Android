@echo off
setlocal enabledelayedexpansion
title LanRead Debug Stopper
chcp 65001 >nul

echo ============================================
echo   LanRead Debug Stopper
echo ============================================
echo.

:: ---- Resolve paths ----
set "ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_SDK%\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [ERROR] adb not found at "%ADB%"
    pause & exit /b 1
)

:: ---- Check for running devices ----
"%ADB%" devices | findstr "emulator" >nul
if %errorlevel% neq 0 (
    echo [INFO] No emulator is running. Nothing to stop.
    pause & exit /b 0
)

:: ---- Graceful shutdown: ask emulator to exit ----
echo [1/2] Sending shutdown command to emulator ...
"%ADB%" emu kill
echo       Waiting for emulator to exit ...

:: Wait up to 30s for the device to disappear
set /a COUNT=0
:WAIT_OFF
"%ADB%" devices | findstr "emulator" >nul
if %errorlevel% neq 0 goto STOPPED
set /a COUNT+=1
if %COUNT% geq 15 (
    echo [WARN] Emulator still shutting down, it will exit on its own.
    goto DONE
)
timeout /t 2 /nobreak >nul
goto WAIT_OFF

:STOPPED
echo [OK] Emulator stopped cleanly.

:DONE
echo.
echo [2/2] Cleaning up adb forwards ...
"%ADB%" forward --remove-all >nul 2>&1
echo [OK] Port forwards removed.
echo.
echo ============================================
echo   All stopped. Bye!
echo ============================================
pause
