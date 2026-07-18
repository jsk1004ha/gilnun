[CmdletBinding()]
param(
    [ValidateSet('fallback8', 'mvp14', 'release24')][string]$Gate = 'release24',
    [switch]$SkipConnected
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifacts = Join-Path $repoRoot 'artifacts'

. (Join-Path $PSScriptRoot 'android-env.ps1')
$signingEnv = Join-Path $HOME '.gilnun\signing.env.ps1'
& (Join-Path $PSScriptRoot 'init-signing.ps1')
if (-not (Test-Path -LiteralPath $signingEnv)) {
    throw "Signing initialization did not create $signingEnv"
}
. $signingEnv
foreach ($name in 'GILNUN_KEYSTORE','GILNUN_STORE_PASSWORD','GILNUN_KEY_ALIAS','GILNUN_KEY_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing signing environment variable: $name"
    }
}

New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
$gradle = Join-Path $repoRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradle)) { throw 'gradlew.bat is missing. Run scripts/bootstrap-android.ps1 first.' }

Push-Location $repoRoot
try {
    & $gradle clean testDebugUnitTest lintRelease assembleDebug assembleRelease --no-daemon
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) { throw "Gradle release gate failed with exit code $gradleExitCode." }
    if (-not $SkipConnected) {
        $device = & (Join-Path $PSScriptRoot 'preflight-device.ps1') -AsJson | ConvertFrom-Json
        if ($device.status -eq 'READY') {
            & $gradle connectedDebugAndroidTest --no-daemon
            $connectedExitCode = $LASTEXITCODE
            if ($connectedExitCode -ne 0) {
                throw "Connected Android tests failed with exit code $connectedExitCode."
            }
        } else {
            Write-Host 'DEVICE_BLOCKED - connected tests were not run.'
        }
    }
} finally {
    Pop-Location
}

$release = Join-Path $repoRoot 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path -LiteralPath $release)) { throw "Release APK was not produced at $release" }
$destinationName = switch ($Gate) {
    'fallback8' { 'gilnun-fallback8.apk' }
    'mvp14' { 'gilnun-mvp14.apk' }
    default { 'gilnun-mvp.apk' }
}
$destination = Join-Path $artifacts $destinationName
Copy-Item -LiteralPath $release -Destination $destination -Force
$apksigner = Join-Path $env:ANDROID_SDK_ROOT 'build-tools\36.0.0\apksigner.bat'
if (-not (Test-Path -LiteralPath $apksigner)) {
    throw "apksigner was not found at $apksigner"
}
& $apksigner verify --verbose --print-certs $destination
$signatureExitCode = $LASTEXITCODE
if ($signatureExitCode -ne 0) {
    throw "APK signature verification failed with exit code $signatureExitCode."
}
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $destination).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$destination.sha256" -Value "$hash  $destinationName" -Encoding ascii
Write-Host "Built $destinationName"
Write-Host "SHA-256 $hash"
