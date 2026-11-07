param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
Push-Location $projectDir
try {
    if (-not $env:JAVA_HOME) {
        $bundledJava = Get-ChildItem -LiteralPath "$projectDir/.tools/java" -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
        if ($bundledJava) { $env:JAVA_HOME = $bundledJava.FullName }
    }
    if (-not $env:JAVA_HOME) { throw '请设置 JAVA_HOME 为 JDK 17 的安装目录。' }
    if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath "$projectDir/.tools/android-sdk/platforms/android-35")) {
        $env:ANDROID_HOME = "$projectDir/.tools/android-sdk"
    }
    $gradlePath = "$projectDir/gradlew.bat"
    if (Test-Path -LiteralPath "$projectDir/.tools/gradle-8.11.1/bin/gradle.bat") { $gradlePath = "$projectDir/.tools/gradle-8.11.1/bin/gradle.bat" }
    $tasks = @('assembleDebug')
    if (-not $SkipTests) { $tasks += @('testDebugUnitTest', 'lintDebug') }
    & $gradlePath @tasks '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw '构建或检查失败，请查看上面的输出。' }
    Write-Output "APK: $projectDir/app/build/outputs/apk/debug/app-debug.apk"
} finally { Pop-Location }
