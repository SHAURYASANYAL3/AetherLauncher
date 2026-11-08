# Aether Launcher Automated Build & Deploy Toolchain
param(
    [switch]$Install,
    [switch]$Launch
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.Encoding]::UTF8

$sdk = if (Test-Path "C:\Android\Sdk") { "C:\Android\Sdk" } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$buildTools = "$sdk\build-tools\34.0.0"
$platform = "$sdk\platforms\android-34\android.jar"
$jdk = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin" } else { "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin" }

Set-Location $PSScriptRoot

Write-Host " [1/6] Compiling resources..." -ForegroundColor Cyan
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "build\gen", "build\obj", "build\dex" -Force | Out-Null

& "$buildTools\aapt2.exe" compile --dir res -o build\compiled_res.zip
if ($LASTEXITCODE -ne 0) { throw "AAPT2 compile failed" }

Write-Host " [2/6] Linking Android binary package & generating R.java..." -ForegroundColor Cyan
& "$buildTools\aapt2.exe" link -I $platform --manifest AndroidManifest.xml -o build\unaligned.apk --java build\gen build\compiled_res.zip --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "AAPT2 link failed" }

Write-Host " [3/6] Compiling Java sources..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "src", "build\gen" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" -encoding UTF-8 -cp "$platform;build\gen" -d "build\obj" $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac compilation failed" }

Write-Host " [4/6] Transpiling bytecode to Dalvik Executable (D8)..." -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path "build\obj" -Filter "*.class" -Recurse | ForEach-Object { $_.FullName }
& "$buildTools\d8.bat" --output build\dex --lib $platform $classFiles
if ($LASTEXITCODE -ne 0) { throw "D8 dexing failed" }

Write-Host " [5/6] Packaging classes.dex..." -ForegroundColor Cyan
& "$jdk\jar.exe" -uf build\unaligned.apk -C build\dex classes.dex
if ($LASTEXITCODE -ne 0) { throw "JAR package update failed" }

Write-Host " [6/6] Zipaligning and cryptographically signing APK..." -ForegroundColor Cyan
& "$buildTools\zipalign.exe" -p -f -v 4 build\unaligned.apk build\AetherLauncher.apk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

& "$buildTools\apksigner.bat" sign --ks "C:\Users\shaur\.android\debug.keystore" --ks-pass "pass:android" --key-pass "pass:android" --ks-key-alias "androiddebugkey" build\AetherLauncher.apk
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host " SUCCESS! APK built at: $PSScriptRoot\build\AetherLauncher.apk" -ForegroundColor Green

if ($Install) {
    Write-Host " Installing APK to connected device..." -ForegroundColor Yellow
    adb install -r "build\AetherLauncher.apk"
    if ($LASTEXITCODE -ne 0) { throw "ADB install failed" }
    Write-Host " Installed successfully." -ForegroundColor Green
}

if ($Launch) {
    Write-Host " Starting Aether Launcher on device..." -ForegroundColor Yellow
    adb shell am start -n io.github.shauryasanyal3.aetherlauncher/.MainActivity
}
