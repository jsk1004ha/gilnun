[CmdletBinding()]
param([string]$ReleaseApk = '')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'android-env.ps1')

if (-not $ReleaseApk) { $ReleaseApk = Join-Path $repoRoot 'artifacts\gilnun-mvp.apk' }
if (-not (Test-Path -LiteralPath $ReleaseApk)) { throw "Release APK not found: $ReleaseApk" }

$sourceRoot = Join-Path $repoRoot 'app\src\main'
$textFiles = Get-ChildItem -LiteralPath $sourceRoot -Recurse -File |
    Where-Object { $_.Extension -in '.kt','.java','.xml','.html','.js','.css' }
$forbiddenPatterns = @(
    'addJavascriptInterface',
    'android.permission.INTERNET',
    'android.permission.SYSTEM_ALERT_WINDOW',
    'android.permission.BIND_ACCESSIBILITY_SERVICE',
    'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION',
    'MediaProjectionManager'
)
foreach ($pattern in $forbiddenPatterns) {
    $hit = $textFiles | Select-String -Pattern $pattern -CaseSensitive:$false
    if ($hit) {
        $locations = $hit | ForEach-Object { "$($_.Path):$($_.LineNumber)" }
        throw "Forbidden source pattern '$pattern': $($locations -join ', ')"
    }
}

$urlMatches = $textFiles | Select-String -Pattern 'https?://[^"''\s<)]+' -AllMatches
$urls = foreach ($line in $urlMatches) {
    foreach ($match in $line.Matches) { $match.Value.TrimEnd(';', ',', '.') }
}
$disallowed = @($urls | Where-Object {
    $_ -ne 'http://schemas.android.com/apk/res/android' -and
    $_ -notlike 'https://appassets.androidplatform.net*' -and
    $_ -notlike 'https://www.w3.org*'
} | Sort-Object -Unique)
if ($disallowed.Count -gt 0) { throw "Disallowed implementation URL(s): $($disallowed -join ', ')" }

$apkanalyzer = @(
    (Join-Path $env:ANDROID_SDK_ROOT 'cmdline-tools\latest\bin\apkanalyzer.bat'),
    (Join-Path $env:ANDROID_SDK_ROOT 'tools\bin\apkanalyzer.bat')
) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $apkanalyzer) { throw 'apkanalyzer was not found in the Android SDK.' }
$manifestLines = & $apkanalyzer manifest print $ReleaseApk
$apkanalyzerExitCode = $LASTEXITCODE
if ($apkanalyzerExitCode -ne 0) {
    throw "apkanalyzer manifest inspection failed with exit code $apkanalyzerExitCode."
}
$manifest = $manifestLines | Out-String
foreach ($permission in 'android.permission.INTERNET','android.permission.SYSTEM_ALERT_WINDOW','android.permission.BIND_ACCESSIBILITY_SERVICE') {
    if ($manifest -match [regex]::Escape($permission)) { throw "Forbidden APK permission: $permission" }
}
if ($manifest -match 'android:debuggable="true"') { throw 'Release APK is debuggable.' }

$apksigner = Join-Path $env:ANDROID_SDK_ROOT 'build-tools\36.0.0\apksigner.bat'
if (-not (Test-Path -LiteralPath $apksigner)) {
    throw "apksigner was not found at $apksigner"
}
& $apksigner verify --verbose $ReleaseApk | Out-Null
$apksignerExitCode = $LASTEXITCODE
if ($apksignerExitCode -ne 0) {
    throw "Release APK signature is invalid with exit code $apksignerExitCode."
}
Write-Host 'Boundary verification passed: permissions, origin surface, debuggable=false, signature.'
