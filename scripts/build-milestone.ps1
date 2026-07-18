[CmdletBinding()]
param(
    [ValidateSet('fallback8', 'mvp14', 'release24')][string]$Gate = 'release24',
    [switch]$SkipConnected
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifacts = Join-Path $repoRoot 'artifacts'
$deviceEvidencePath = Join-Path $artifacts 'android-test-status.json'

. (Join-Path $PSScriptRoot 'android-env.ps1')
& (Join-Path $PSScriptRoot 'init-signing.ps1')
foreach ($name in 'GILNUN_KEYSTORE','GILNUN_STORE_PASSWORD','GILNUN_KEY_ALIAS','GILNUN_KEY_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing signing environment variable: $name"
    }
}

New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
$deviceTestStatus = 'SKIPPED'
$devicePreflightStatus = 'NOT_RUN'
$connectedTestsRun = $false
$gradle = Join-Path $repoRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradle)) { throw 'gradlew.bat is missing. Run scripts/bootstrap-android.ps1 first.' }

Push-Location $repoRoot
try {
    & $gradle clean testDebugUnitTest lintRelease assembleDebug assembleDebugAndroidTest assembleRelease --no-daemon
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) { throw "Gradle release gate failed with exit code $gradleExitCode." }
    if (-not $SkipConnected) {
        $device = & (Join-Path $PSScriptRoot 'preflight-device.ps1') -AsJson | ConvertFrom-Json
        $devicePreflightStatus = [string]$device.status
        if ($device.status -eq 'READY') {
            & $gradle connectedDebugAndroidTest --no-daemon
            $connectedExitCode = $LASTEXITCODE
            if ($connectedExitCode -ne 0) {
                throw "Connected Android tests failed with exit code $connectedExitCode."
            }
            $connectedTestsRun = $true
            $deviceTestStatus = 'PASSED'
        } else {
            $deviceTestStatus = 'BLOCKED'
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
$temporaryApk = "$destination.pending"
$destinationHash = "$destination.sha256"
$temporaryHash = "$destinationHash.pending"
Remove-Item -LiteralPath $temporaryApk,$temporaryHash -Force -ErrorAction SilentlyContinue
$apksigner = Join-Path $env:ANDROID_SDK_ROOT 'build-tools\36.0.0\apksigner.bat'
$zipalign = Join-Path $env:ANDROID_SDK_ROOT 'build-tools\36.0.0\zipalign.exe'
if (-not (Test-Path -LiteralPath $apksigner)) {
    throw "apksigner was not found at $apksigner"
}
if (-not (Test-Path -LiteralPath $zipalign)) {
    throw "zipalign was not found at $zipalign"
}
& $zipalign -c -P 16 -v 4 $release | Out-Null
$zipalignExitCode = $LASTEXITCODE
if ($zipalignExitCode -ne 0) {
    throw "APK zip alignment verification failed with exit code $zipalignExitCode."
}
$signerOutput = & $apksigner verify --verbose --print-certs $release 2>&1
$signatureExitCode = $LASTEXITCODE
if ($signatureExitCode -ne 0) {
    throw "APK signature verification failed with exit code $signatureExitCode."
}
$expectedSigner = '9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991'
$signerMatches = [regex]::Matches(
    ($signerOutput | Out-String),
    'certificate SHA-256 digest:\s*([0-9A-Fa-f:]+)',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)
if ($signerMatches.Count -eq 0) {
    throw 'APK signer SHA-256 digest was not present in apksigner output.'
}
$actualSigners = @(
    $signerMatches |
        ForEach-Object { $_.Groups[1].Value.Replace(':', '').ToLowerInvariant() } |
        Sort-Object -Unique
)
if ($actualSigners.Count -ne 1 -or $actualSigners[0] -ne $expectedSigner) {
    throw "APK signer certificate set changed. Expected only $expectedSigner, found $($actualSigners -join ',')."
}
Copy-Item -LiteralPath $release -Destination $temporaryApk -Force
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $temporaryApk).Hash.ToLowerInvariant()
Set-Content -LiteralPath $temporaryHash -Value "$hash  $destinationName" -Encoding ascii
Move-Item -LiteralPath $temporaryApk -Destination $destination -Force
Move-Item -LiteralPath $temporaryHash -Destination $destinationHash -Force

$deviceEvidence = [ordered]@{
    status = $deviceTestStatus
    preflightStatus = $devicePreflightStatus
    connectedTestsRun = $connectedTestsRun
}
$deviceEvidenceTemporary = "$deviceEvidencePath.pending"
$deviceEvidence | ConvertTo-Json | Set-Content -LiteralPath $deviceEvidenceTemporary -Encoding utf8
Move-Item -LiteralPath $deviceEvidenceTemporary -Destination $deviceEvidencePath -Force

Write-Host "Built $destinationName"
Write-Host "SHA-256 $hash"
Write-Host "Signer SHA-256 $($actualSigners[0])"
Write-Host "Connected test status $deviceTestStatus"
