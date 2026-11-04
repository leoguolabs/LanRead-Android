# LanRead (澜阅) 端到端自动化辅助测试脚本
param(
    [switch]$PushEpubs,
    [switch]$LaunchApp,
    [switch]$CaptureState,
    [string]$Prefix = "e2e_state"
)

$ErrorActionPreference = "Continue"
$projectRoot = Split-Path -Parent $PSScriptRoot
$artifactsDir = Join-Path $PSScriptRoot "artifacts"
$epubsDir = Join-Path $PSScriptRoot "epubs"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adb = "adb.exe"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LanRead (澜阅) 端到端测试执行与现场归档工具" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 检查设备在线
$devices = & $adb devices | Out-String
if ($devices -notmatch "device\b") {
    Write-Host "[ERROR] 未检测到在线的 Android 设备或模拟器，请先启动 AVD！" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Android 模拟器/设备在线" -ForegroundColor Green

# 1. 推送测试 EPUB 样本至 /sdcard/Download/
if ($PushEpubs) {
    Write-Host "`n[1/3] 正在推送 EPUB 测试样本库到设备 /sdcard/Download/ ..." -ForegroundColor Yellow
    & $adb shell "mkdir -p /sdcard/Download/LanRead_Test_Epubs"
    $epubFiles = Get-ChildItem -Path $epubsDir -Filter "*.epub"
    foreach ($f in $epubFiles) {
        if ($f.Length -gt 1024) {
            Write-Host "  -> 推送: $($f.Name) ($([math]::Round($f.Length/1KB, 1)) KB)"
            & $adb push $f.FullName "/sdcard/Download/LanRead_Test_Epubs/$($f.Name)" | Out-Null
        }
    }
    Write-Host "[OK] 全部 EPUB 样本已推送至 /sdcard/Download/LanRead_Test_Epubs/" -ForegroundColor Green
}

# 2. 启动应用
if ($LaunchApp) {
    Write-Host "`n[2/3] 正在启动 LanRead 应用 ..." -ForegroundColor Yellow
    & $adb shell am start -n com.lanread.app/.MainActivity
    Start-Sleep -Seconds 2
    Write-Host "[OK] 应用已启动" -ForegroundColor Green
}

# 3. 现场快照捕获 (截屏 + uiautomator dump)
if ($CaptureState) {
    if (-not (Test-Path $artifactsDir)) {
        New-Item -ItemType Directory -Path $artifactsDir -Force | Out-Null
    }
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $pngFile = Join-Path $artifactsDir "${Prefix}_${timestamp}.png"
    $xmlFile = Join-Path $artifactsDir "${Prefix}_${timestamp}.xml"

    Write-Host "`n[3/3] 捕获现场快照: $Prefix ..." -ForegroundColor Yellow
    & $adb shell screencap -p "/sdcard/${Prefix}.png"
    & $adb pull "/sdcard/${Prefix}.png" $pngFile | Out-Null
    & $adb shell rm "/sdcard/${Prefix}.png"

    & $adb shell uiautomator dump "/sdcard/${Prefix}.xml" | Out-Null
    & $adb pull "/sdcard/${Prefix}.xml" $xmlFile | Out-Null
    & $adb shell rm "/sdcard/${Prefix}.xml"

    Write-Host "  -> 截图: $pngFile" -ForegroundColor Green
    Write-Host "  -> 界面树: $xmlFile" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " 执行完成。" -ForegroundColor Cyan
