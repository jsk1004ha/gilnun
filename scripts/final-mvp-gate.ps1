[CmdletBinding()]
param([switch]$SkipConnected)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$evidenceDir = Join-Path $repoRoot '.omo\evidence\gilnun-android-mvp\final'
$outputsDir = Join-Path $repoRoot 'outputs'
New-Item -ItemType Directory -Force -Path $evidenceDir, $outputsDir | Out-Null
. (Join-Path $PSScriptRoot 'android-env.ps1')
$steps = New-Object System.Collections.Generic.List[string]

$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
Push-Location (Join-Path $repoRoot 'web')
try {
    & $npm run build
    $webExitCode = $LASTEXITCODE
    if ($webExitCode -ne 0) { throw "Web build failed with exit code $webExitCode." }
    & $npm test
    $webTestExitCode = $LASTEXITCODE
    if ($webTestExitCode -ne 0) { throw "Web rendered-product tests failed with exit code $webTestExitCode." }
    $steps.Add('PASS — web production build and rendered-product tests')
} finally { Pop-Location }

& (Join-Path $PSScriptRoot 'build-milestone.ps1') -Gate release24 -SkipConnected:$SkipConnected
$steps.Add('PASS — Android unit tests, lint, debug/release build and signature')

$apk = Join-Path $repoRoot 'artifacts\gilnun-mvp.apk'
& (Join-Path $PSScriptRoot 'verify-boundaries.ps1') -ReleaseApk $apk
$steps.Add('PASS — offline/privacy/static APK boundaries')

$device = & (Join-Path $PSScriptRoot 'preflight-device.ps1') -AsJson | ConvertFrom-Json
if ($device.status -eq 'READY' -and -not $SkipConnected) {
    $steps.Add('PASS — connected tests ran on one authorized Android device')
} else {
    $steps.Add('DEVICE_BLOCKED — physical-device QA remains required')
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash.ToLowerInvariant()
$summary = @(
    '# Gilnun MVP final verification'
    ''
    "Generated: $([DateTimeOffset]::Now.ToString('o'))"
    ''
    ($steps | ForEach-Object { "- $_" })
    ''
    "- APK SHA-256: $hash"
    '- APK: outputs/gilnun-mvp.apk'
)
$evidenceSummary = Join-Path $evidenceDir 'f1-plan-compliance.md'
$outputSummary = Join-Path $outputsDir 'verification-summary.md'
Copy-Item -LiteralPath $apk -Destination (Join-Path $outputsDir 'gilnun-mvp.apk') -Force
Copy-Item -LiteralPath "$apk.sha256" -Destination (Join-Path $outputsDir 'gilnun-mvp.apk.sha256') -Force
Set-Content -LiteralPath $evidenceSummary -Value $summary -Encoding utf8
Set-Content -LiteralPath $outputSummary -Value $summary -Encoding utf8
Set-Content -LiteralPath (Join-Path $evidenceDir 'sha256.txt') -Value $hash -Encoding ascii
Write-Host 'FINAL_MVP_GATE_PASS'
Write-Host "APK SHA-256 $hash"
Write-Host "Deliverables: $outputsDir"
