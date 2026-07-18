[CmdletBinding()]
param([string]$ReleaseApk = '')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'android-env.ps1')

if (-not $ReleaseApk) { $ReleaseApk = Join-Path $repoRoot 'artifacts\gilnun-mvp.apk' }
$ReleaseApk = [System.IO.Path]::GetFullPath($ReleaseApk)
if (-not (Test-Path -LiteralPath $ReleaseApk -PathType Leaf)) {
    throw "Release APK not found: $ReleaseApk"
}

function Assert-Contains {
    param(
        [Parameter(Mandatory)][string]$Text,
        [Parameter(Mandatory)][string]$Expected,
        [Parameter(Mandatory)][string]$Description
    )
    if ($Text.IndexOf($Expected, [System.StringComparison]::Ordinal) -lt 0) {
        throw "Missing $Description ('$Expected')."
    }
}

function Read-ExactApkEntry {
    param(
        [Parameter(Mandatory)][System.IO.Compression.ZipArchive]$Archive,
        [Parameter(Mandatory)][string]$EntryName
    )
    $matches = @($Archive.Entries | Where-Object { $_.FullName -ceq $EntryName })
    if ($matches.Count -ne 1) {
        throw "APK must contain exactly one '$EntryName' entry; found $($matches.Count)."
    }
    $stream = $matches[0].Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($memory)
        return ,$memory.ToArray()
    } finally {
        $memory.Dispose()
        $stream.Dispose()
    }
}

function Assert-NoSourcePattern {
    param(
        [Parameter(Mandatory)][System.IO.FileInfo[]]$Files,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Description
    )
    $hits = @($Files | Select-String -Pattern $Pattern -CaseSensitive:$false)
    if ($hits.Count -gt 0) {
        $locations = $hits | ForEach-Object { "$($_.Path):$($_.LineNumber)" }
        throw "Forbidden ${Description}: $($locations -join ', ')"
    }
}

$sourceRoot = Join-Path $repoRoot 'app\src\main'
$textFiles = @(
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -File |
        Where-Object { $_.Extension -in '.kt','.java','.xml','.html','.js','.css','.svg' }
)

$forbiddenSourcePatterns = [ordered]@{
    'addJavascriptInterface' = 'JavaScript interface'
    'android\.permission\.INTERNET' = 'internet permission'
    'android\.permission\.SYSTEM_ALERT_WINDOW' = 'overlay permission'
    'android\.permission\.BIND_ACCESSIBILITY_SERVICE' = 'accessibility-service permission'
    'android\.permission\.CAMERA' = 'camera permission'
    'android\.permission\.RECORD_AUDIO' = 'microphone permission'
    'android\.permission\.(READ|WRITE|MANAGE)_EXTERNAL_STORAGE' = 'storage permission'
    'android\.permission\.FOREGROUND_SERVICE_MEDIA_PROJECTION' = 'media-projection permission'
    'MediaProjectionManager' = 'media-projection API'
    '\b(fetch|XMLHttpRequest|WebSocket)\s*\(' = 'network API'
    '\b(localStorage|sessionStorage|indexedDB)\b' = 'browser persistence'
    'document\.cookie' = 'DOM cookie access'
    '\.click\s*\(' = 'automatic DOM click'
    'dispatchEvent\s*\(' = 'synthetic DOM event'
    '\b(console\.(log|debug|info)|Log\.(v|d|i))\s*\(' = 'payload-capable diagnostic logging'
}
foreach ($entry in $forbiddenSourcePatterns.GetEnumerator()) {
    Assert-NoSourcePattern -Files $textFiles -Pattern $entry.Key -Description $entry.Value
}

$urlMatches = @($textFiles | Select-String -Pattern 'https?://[^"''\s<)]+' -AllMatches)
$urls = foreach ($line in $urlMatches) {
    foreach ($match in $line.Matches) { $match.Value.TrimEnd(';', ',', '.', '\') }
}
$allowedExactUrls = @(
    'http://schemas.android.com/apk/res/android',
    'http://schemas.android.com/tools',
    'http://www.w3.org/2000/svg',
    'https://appassets.androidplatform.net'
)
$disallowed = @(
    $urls |
        Where-Object {
            $_ -notin $allowedExactUrls -and
            $_ -notlike 'https://appassets.androidplatform.net/assets/welfare/index.html*'
        } |
        Sort-Object -Unique
)
if ($disallowed.Count -gt 0) {
    throw "Disallowed implementation URL(s): $($disallowed -join ', ')"
}

$assetRoot = Join-Path $sourceRoot 'assets\welfare'
$indexPath = Join-Path $assetRoot 'index.html'
$scriptPath = Join-Path $assetRoot 'app.js'
$stylePath = Join-Path $assetRoot 'style.css'
foreach ($path in $indexPath, $scriptPath, $stylePath) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required APK-owned practice asset is missing: $path"
    }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($ReleaseApk)
try {
    $indexBytes = Read-ExactApkEntry -Archive $archive -EntryName 'assets/welfare/index.html'
    $scriptBytes = Read-ExactApkEntry -Archive $archive -EntryName 'assets/welfare/app.js'
    $styleBytes = Read-ExactApkEntry -Archive $archive -EntryName 'assets/welfare/style.css'
} finally {
    $archive.Dispose()
}

$assetPairs = @(
    @{ Source = $indexPath; Name = 'assets/welfare/index.html'; Bytes = $indexBytes },
    @{ Source = $scriptPath; Name = 'assets/welfare/app.js'; Bytes = $scriptBytes },
    @{ Source = $stylePath; Name = 'assets/welfare/style.css'; Bytes = $styleBytes }
)
foreach ($asset in $assetPairs) {
    $sourceBytes = [System.IO.File]::ReadAllBytes($asset.Source)
    if ([Convert]::ToBase64String($sourceBytes) -cne [Convert]::ToBase64String($asset.Bytes)) {
        throw "Packaged asset diverges from checkout source: $($asset.Name)"
    }
}

$index = [System.Text.Encoding]::UTF8.GetString($indexBytes)
$script = [System.Text.Encoding]::UTF8.GetString($scriptBytes)
$style = [System.Text.Encoding]::UTF8.GetString($styleBytes)
$allAssets = "$index`n$script`n$style"

foreach ($directive in "default-src 'none'", "connect-src 'none'", "form-action 'none'") {
    Assert-Contains -Text $index -Expected $directive -Description 'CSP directive'
}
foreach ($token in @(
    'basic-pension',
    'resident-record',
    'health-screening',
    'bokjiro-basic-pension',
    'gov24-resident-record',
    'nhis-health-screening',
    '2026-07',
    '연습용 화면 · 실제 기관과 연결되지 않아요',
    '신청 연습을 마쳤어요',
    '발급 연습을 마쳤어요',
    '대상 조회 연습을 마쳤어요',
    '법적 효력 없음',
    '일반건강검진 대상(모의)',
    'CHECKPOINT_CHANGED',
    'NON_PROGRESS'
)) {
    Assert-Contains -Text $allAssets -Expected $token -Description 'practice contract token'
}
if ($allAssets -match '(?is)<\s*(form|input|textarea|select)\b') {
    throw 'Practice assets contain a form or input element.'
}
if ($allAssets -match '(?i)\b(submit|payment|login)\b|제출|결제|로그인') {
    throw 'Practice assets contain a real submission, payment, or login affordance.'
}
Assert-Contains -Text $style -Expected 'overflow-x: hidden' -Description 'horizontal overflow protection'
Assert-Contains -Text $style -Expected 'prefers-reduced-motion' -Description 'reduced-motion support'

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
Assert-Contains -Text $manifest -Expected 'package="com.gilnun.app"' -Description 'application package'
Assert-Contains -Text $manifest -Expected 'android:versionCode="2"' -Description 'version code'
Assert-Contains -Text $manifest -Expected 'android:versionName="0.2.0"' -Description 'version name'
if ($manifest -match 'android:debuggable="true"') { throw 'Release APK is debuggable.' }
foreach ($permission in @(
    'android.permission.INTERNET',
    'android.permission.SYSTEM_ALERT_WINDOW',
    'android.permission.BIND_ACCESSIBILITY_SERVICE',
    'android.permission.CAMERA',
    'android.permission.RECORD_AUDIO',
    'android.permission.READ_EXTERNAL_STORAGE',
    'android.permission.WRITE_EXTERNAL_STORAGE',
    'android.permission.MANAGE_EXTERNAL_STORAGE',
    'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION'
)) {
    if ($manifest -match [regex]::Escape($permission)) {
        throw "Forbidden APK permission: $permission"
    }
}

$buildTools = Join-Path $env:ANDROID_SDK_ROOT 'build-tools\36.0.0'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$zipalign = Join-Path $buildTools 'zipalign.exe'
foreach ($tool in $apksigner, $zipalign) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Required Android build tool not found: $tool"
    }
}
& $zipalign -c -P 16 -v 4 $ReleaseApk | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Release APK is not zip-aligned.' }
$signerOutput = & $apksigner verify --verbose --print-certs $ReleaseApk 2>&1
if ($LASTEXITCODE -ne 0) { throw 'Release APK signature is invalid.' }
$signerMatches = [regex]::Matches(
    ($signerOutput | Out-String),
    'certificate SHA-256 digest:\s*([0-9A-Fa-f:]+)',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)
if ($signerMatches.Count -eq 0) { throw 'APK signer SHA-256 digest was not reported.' }
$actualSigners = @(
    $signerMatches |
        ForEach-Object { $_.Groups[1].Value.Replace(':', '').ToLowerInvariant() } |
        Sort-Object -Unique
)
$expectedSigner = '9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991'
if ($actualSigners.Count -ne 1 -or $actualSigners[0] -ne $expectedSigner) {
    throw "APK signer certificate set changed. Expected only $expectedSigner, found $($actualSigners -join ',')."
}

Write-Host 'Boundary verification passed: package/version, packaged offline/privacy assets, alignment, exact signer set.'
