# Run from an elevated PowerShell. This enables one Windows component and never reboots automatically.
$ErrorActionPreference = 'Stop'
$bodyRecordIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
$bodyRecordPrincipal = [Security.Principal.WindowsPrincipal]::new($bodyRecordIdentity)
if (-not $bodyRecordPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw '需要管理员权限：请右键 PowerShell，选择“以管理员身份运行”，再执行此脚本。'
}
$bodyRecordFeature = Get-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform
if ($bodyRecordFeature.State -eq 'Enabled') {
    Write-Output 'Windows Hypervisor Platform 已启用。如果模拟器仍提示不可用，请重启电脑。'
    return
}
Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All -NoRestart
Write-Output '加速组件已启用。请保存工作并自行重启电脑，之后执行 E:\BodyRecord\scripts\start-emulator.ps1。'
