# Dot-source this file; all emulator tools, state, and temporary files stay on E:.
$bodyRecordRoot = Split-Path -Parent $PSScriptRoot
$bodyRecordTools = Join-Path $bodyRecordRoot '.tools'
$env:ANDROID_HOME = Join-Path $bodyRecordTools 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $bodyRecordTools 'android-user'
$env:ANDROID_EMULATOR_HOME = $env:ANDROID_USER_HOME
$env:ANDROID_AVD_HOME = Join-Path $bodyRecordTools 'avd'
Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue
$env:TEMP = Join-Path $bodyRecordTools 'tmp'
$env:TMP = $env:TEMP
foreach ($bodyRecordDirectory in @($env:ANDROID_USER_HOME,$env:ANDROID_AVD_HOME,$env:TEMP)) {
    New-Item -ItemType Directory -Path $bodyRecordDirectory -Force | Out-Null
}
$bodyRecordJava = Get-ChildItem -LiteralPath (Join-Path $bodyRecordTools 'java') -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
if ($bodyRecordJava) { $env:JAVA_HOME = $bodyRecordJava.FullName }
$bodyRecordAdb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
$bodyRecordEmulator = Join-Path $env:ANDROID_HOME 'emulator/emulator.exe'
