param(
    [switch]$Software,
    [switch]$Headless,
    [switch]$NoInstall,
    [int]$BootTimeoutSeconds = 300
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'android-env.ps1')
$bodyRecordAvdName = 'BodyRecord_API35'
$bodyRecordSerial = 'emulator-5554'
if (-not (Test-Path -LiteralPath $bodyRecordEmulator)) { throw '安卓模拟器尚未安装。' }
if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_AVD_HOME "$bodyRecordAvdName.ini"))) { throw '虚拟手机尚未创建。' }

$bodyRecordDeviceState = & { $ErrorActionPreference = 'SilentlyContinue'; & $bodyRecordAdb -s $bodyRecordSerial get-state 2>$null }
$bodyRecordEmulatorProcess = $null
$bodyRecordExistingProcess = Get-CimInstance Win32_Process -Filter "Name='emulator.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.ExecutablePath -eq $bodyRecordEmulator -and $_.CommandLine -match '-avd\s+BodyRecord_API35(?:\s|$)' } |
    Select-Object -First 1
if ($bodyRecordDeviceState -eq 'device') {
    $bodyRecordExistingName = & $bodyRecordAdb -s $bodyRecordSerial emu avd name 2>$null
    if ($bodyRecordExistingName -notcontains $bodyRecordAvdName) { throw '5554 端口已被其他虚拟手机占用，请先关闭它再运行。' }
    Write-Output '轻记的虚拟手机已运行，正在连接。'
} elseif ($bodyRecordExistingProcess) {
    $bodyRecordEmulatorProcess = Get-Process -Id $bodyRecordExistingProcess.ProcessId
    Write-Output '虚拟手机正在启动，将等待现有窗口就绪。'
} else {
    if (-not $Software) {
        $bodyRecordAcceleration = & $bodyRecordEmulator -accel-check 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Output ($bodyRecordAcceleration -join "`n")
            throw '尚未启用 Windows Hypervisor Platform。请按 docs/EMULATOR.md 启用并重启；也可添加 -Software 使用较慢的软件模拟模式。'
        }
    }
    $bodyRecordEmulatorArguments = @('-avd',$bodyRecordAvdName,'-port','5554','-gpu','software','-no-snapshot','-no-boot-anim','-no-audio','-camera-back','none','-camera-front','none','-memory','3072','-scale','0.75')
    if ($Software) {
        $bodyRecordEmulatorArguments += @('-accel','off','-cores','1')
        if (-not $PSBoundParameters.ContainsKey('BootTimeoutSeconds')) { $BootTimeoutSeconds = 900 }
    } else { $bodyRecordEmulatorArguments += @('-cores','2') }
    if ($Headless) { $bodyRecordEmulatorArguments += '-no-window' }
    $bodyRecordProcessOptions = @{
        FilePath = $bodyRecordEmulator
        ArgumentList = $bodyRecordEmulatorArguments
        WorkingDirectory = $bodyRecordTools
        RedirectStandardOutput = (Join-Path $env:ANDROID_USER_HOME 'emulator-stdout.log')
        RedirectStandardError = (Join-Path $env:ANDROID_USER_HOME 'emulator-stderr.log')
        PassThru = $true
        WindowStyle = $(if ($Headless) { 'Hidden' } else { 'Normal' })
    }
    $bodyRecordEmulatorProcess = Start-Process @bodyRecordProcessOptions
    Write-Output "已启动安卓模拟器，正在等待系统启动（日志位于 $env:ANDROID_USER_HOME）。"
}

$bodyRecordBootDeadline = [DateTime]::UtcNow.AddSeconds($BootTimeoutSeconds)
$bodyRecordBooted = $false
while ([DateTime]::UtcNow -lt $bodyRecordBootDeadline) {
    if ($bodyRecordEmulatorProcess) {
        $bodyRecordEmulatorProcess.Refresh()
        if ($bodyRecordEmulatorProcess.HasExited) {
            Get-Content -LiteralPath (Join-Path $env:ANDROID_USER_HOME 'emulator-stderr.log') -Tail 20 -ErrorAction SilentlyContinue
            throw '模拟器启动失败，请查看 E 盘中的模拟器日志。'
        }
    }
    $bodyRecordBootState = & { $ErrorActionPreference = 'SilentlyContinue'; & $bodyRecordAdb -s $bodyRecordSerial shell getprop sys.boot_completed 2>$null }
    if ($bodyRecordBootState -eq '1') { $bodyRecordBooted = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $bodyRecordBooted) { throw '模拟器启动超时。软件模拟速度较慢，建议启用 Windows Hypervisor Platform 后重新运行。' }
& $bodyRecordAdb -s $bodyRecordSerial shell input keyevent 82 | Out-Null
if (-not $NoInstall) {
    $bodyRecordApk = Join-Path $bodyRecordRoot 'app/build/outputs/apk/debug/app-debug.apk'
    if (-not (Test-Path -LiteralPath $bodyRecordApk)) { $bodyRecordApk = Join-Path $bodyRecordRoot 'dist/BodyRecord-1.3.0-debug.apk' }
    if (-not (Test-Path -LiteralPath $bodyRecordApk)) { throw '未找到 APK，请先执行 scripts/build.ps1。' }
    & $bodyRecordAdb -s $bodyRecordSerial install -r $bodyRecordApk
    if ($LASTEXITCODE -ne 0) { throw '安装 APK 失败。' }
    & $bodyRecordAdb -s $bodyRecordSerial shell am start -W -n com.bodyrecord.app/.MainActivity
    if ($LASTEXITCODE -ne 0) { throw '打开轻记失败。' }
}
Write-Output '安卓虚拟手机已就绪。可以在模拟器窗口中操作轻记，关闭窗口即可退出。'
