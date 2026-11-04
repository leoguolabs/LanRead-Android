@echo off
setlocal enabledelayedexpansion
title LanRead Debug Launcher
chcp 65001 >nul

echo ============================================
echo   LanRead (澜阅) Debug Launcher  v2
echo ============================================
echo.

:: ---- Resolve paths ----
set "ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_SDK%\platform-tools\adb.exe"
set "EMULATOR=%ANDROID_SDK%\emulator\emulator.exe"
set "QEMU_DIR=%ANDROID_SDK%\emulator\qemu\windows-x86_64"
set "QEMU_WIN=%QEMU_DIR%\qemu-system-x86_64.exe"
set "PROJECT=%~dp0"
if "%PROJECT:~-1%"=="\" set "PROJECT=%PROJECT:~0,-1%"
set "APK=%PROJECT%\app\build\outputs\apk\debug\app-debug.apk"
set "AVD_NAME=Medium_Phone_API_36.1"

if not exist "%ADB%" (
    echo [ERROR] adb not found at "%ADB%"
    pause & exit /b 1
)
if not exist "%APK%" (
    echo [ERROR] APK not found at "%APK%"
    echo         Run: gradle assembleDebug  first.
    pause & exit /b 1
)

:: ---- Kill any leftover emulator first (prevents "another AVD is running") ----
"%ADB%" devices 2>nul | findstr "emulator" >nul
if %errorlevel%==0 (
    echo [INFO] An emulator is already running. Stopping it first ...
    "%ADB%" emu kill >nul 2>&1
    timeout /t 6 /nobreak >nul
    taskkill /f /im qemu-system-x86_64-headless.exe >nul 2>&1
    taskkill /f /im qemu-system-x86_64.exe >nul 2>&1
    timeout /t 2 /nobreak >nul
)
echo [OK] Environment clean
echo.

:: ---- Step 1: launch emulator WITH a visible window ----
:: IMPORTANT: use emulator.exe WITHOUT -no-window. Verified: it spawns the windowed
:: qemu-system-x86_64.exe binary and a real Android window appears on screen.
:: (Do NOT call qemu-system-x86_64.exe directly - it fails to load Qt DLLs standalone.)
echo [1/4] Starting emulator WITH visible window (AVD: %AVD_NAME%) ...

start "LanRead Emulator" "%EMULATOR%" -avd %AVD_NAME% -gpu host -no-snapshot -no-boot-anim

echo       Waiting for device to come online ...
"%ADB%" wait-for-device

:: Wait for full boot (max 3 min)
set /a COUNT=0
:WAIT_BOOT
"%ADB%" shell getprop sys.boot_completed 2>nul | findstr "1" >nul
if %errorlevel%==0 goto BOOTED
set /a COUNT+=1
if %COUNT% geq 90 (
    echo [ERROR] Emulator boot timed out. Check the emulator window for errors.
    pause & exit /b 1
)
timeout /t 2 /nobreak >nul
goto WAIT_BOOT

:BOOTED
echo [OK] Emulator booted - you should now SEE the Android window on screen
echo.

:: ---- Step 2: install APK ----
echo [2/4] Installing APK ...
"%ADB%" install -r "%APK%"
if %errorlevel% neq 0 (
    echo [ERROR] Install failed.
    pause & exit /b 1
)
echo [OK] Installed
echo.

:: ---- Step 3: launch app ----
echo [3/4] Launching LanRead ...
"%ADB%" shell am start -n com.lanread.app/.MainActivity
timeout /t 3 /nobreak >nul
echo [OK] App launched - check the emulator window
echo.

:: ---- Step 4: debug helpers ----
echo [4/4] Debug session ready.
echo.
echo   App logs (another terminal):
echo     "%ADB%" logcat --pid=$("adb shell pidof com.lanread.app"^)
echo.
echo   Chrome DevTools for reader WebView:
echo     1. Open the reader in the app (tap book, then Start Reading)
echo     2. Chrome on this PC: chrome://inspect/#devices
echo     3. Click "inspect" next to "LanRead User Guide"
echo.
echo   To stop everything: run stop.bat
echo.
echo ============================================
echo   Emulator window is open. Minimize me.
echo ============================================
pause
