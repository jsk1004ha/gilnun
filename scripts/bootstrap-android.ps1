[CmdletBinding()]
param(
    [switch]$SkipAndroidStudio
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$bootstrapRoot = Join-Path $repoRoot '.android-bootstrap'
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$commandToolsUrl = 'https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip'
$commandToolsOfficialSha1 = '16b3f45ddb3d85ea6bbe6a1c0b47146daf0db450'
$gradleUrl = 'https://services.gradle.org/distributions/gradle-9.5.0-bin.zip'
$gradleSha256 = '553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746'

function Invoke-CheckedNative {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [string[]]$ArgumentList = @()
    )
    & $FilePath @ArgumentList
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "$FilePath failed with exit code $exitCode."
    }
}

function Test-Java17Present {
    $candidateHomes = Get-ChildItem -Directory -ErrorAction SilentlyContinue `
        -Path 'C:\Program Files\Eclipse Adoptium\jdk-17*'
    foreach ($candidateHome in $candidateHomes) {
        $java = Join-Path $candidateHome.FullName 'bin\java.exe'
        if (Test-Path -LiteralPath $java) {
            $versionLines = & $java -version 2>&1
            $exitCode = $LASTEXITCODE
            $version = $versionLines | Out-String
            if ($exitCode -eq 0 -and $version -match 'version "17(?:\.|")') {
                return $true
            }
        }
    }
    return $false
}

function Install-WingetPackageIfMissing {
    param(
        [Parameter(Mandatory)][string]$Id,
        [Parameter(Mandatory)][scriptblock]$Present
    )
    if (& $Present) {
        return
    }
    $winget = Get-Command winget.exe -ErrorAction Stop
    Invoke-CheckedNative -FilePath $winget.Source -ArgumentList @(
        'install', '--id', $Id, '--exact', '--silent',
        '--accept-package-agreements', '--accept-source-agreements'
    )
    if (-not (& $Present)) {
        throw "$Id installation completed but the package was not detected."
    }
}

New-Item -ItemType Directory -Force -Path $bootstrapRoot, $sdkRoot | Out-Null

Install-WingetPackageIfMissing -Id 'EclipseAdoptium.Temurin.17.JDK' -Present {
    Test-Java17Present
}

if (-not $SkipAndroidStudio) {
    Install-WingetPackageIfMissing -Id 'Google.AndroidStudio' -Present {
        Test-Path -LiteralPath 'C:\Program Files\Android\Android Studio\bin\studio64.exe'
    }
}

$jdkHome = Get-ChildItem -Directory -Path 'C:\Program Files\Eclipse Adoptium\jdk-17*' |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $jdkHome) {
    throw 'Temurin JDK 17 is required after bootstrap installation.'
}
$env:JAVA_HOME = $jdkHome
$env:Path = "$(Join-Path $jdkHome 'bin');$env:Path"

$sdkManager = Join-Path $sdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path -LiteralPath $sdkManager)) {
    $archive = Join-Path $bootstrapRoot 'commandlinetools-win-14742923_latest.zip'
    $expanded = Join-Path $bootstrapRoot 'commandlinetools-expanded'
    Invoke-WebRequest -UseBasicParsing -Uri $commandToolsUrl -OutFile $archive
    $actualSha1 = (Get-FileHash -Algorithm SHA1 -LiteralPath $archive).Hash.ToLowerInvariant()
    if ($actualSha1 -ne $commandToolsOfficialSha1) {
        throw "Android command-line tools checksum mismatch: $actualSha1"
    }

    if (Test-Path -LiteralPath $expanded) {
        $resolvedExpanded = [System.IO.Path]::GetFullPath($expanded)
        if (-not $resolvedExpanded.StartsWith([System.IO.Path]::GetFullPath($bootstrapRoot), [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to clear unexpected path: $resolvedExpanded"
        }
        Remove-Item -Recurse -Force -LiteralPath $resolvedExpanded
    }
    Expand-Archive -LiteralPath $archive -DestinationPath $expanded -Force

    $latest = Join-Path $sdkRoot 'cmdline-tools\latest'
    New-Item -ItemType Directory -Force -Path $latest | Out-Null
    Copy-Item -Recurse -Force -Path (Join-Path $expanded 'cmdline-tools\*') -Destination $latest
}

$licenseAnswers = 1..200 | ForEach-Object { 'y' }
$licenseAnswers | & $sdkManager --sdk_root=$sdkRoot --licenses | Out-Null
$licenseExitCode = $LASTEXITCODE
if ($licenseExitCode -ne 0) {
    throw "Android SDK license acceptance failed with exit code $licenseExitCode."
}

Invoke-CheckedNative -FilePath $sdkManager -ArgumentList @(
    "--sdk_root=$sdkRoot",
    '--channel=3',
    'platform-tools',
    'platforms;android-37.0',
    'build-tools;37.0.0',
    'build-tools;36.0.0'
)

$sdkProperty = $sdkRoot.Replace('\', '\\').Replace(':', '\:')
Set-Content -LiteralPath (Join-Path $repoRoot 'local.properties') `
    -Value "sdk.dir=$sdkProperty" -Encoding ascii -NoNewline

. (Join-Path $PSScriptRoot 'android-env.ps1')

$gradleWrapper = Join-Path $repoRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    $gradleArchive = Join-Path $bootstrapRoot 'gradle-9.5.0-bin.zip'
    $gradleExpanded = Join-Path $bootstrapRoot 'gradle'
    Invoke-WebRequest -UseBasicParsing -Uri $gradleUrl -OutFile $gradleArchive
    $actualGradleSha = (Get-FileHash -Algorithm SHA256 -LiteralPath $gradleArchive).Hash.ToLowerInvariant()
    if ($actualGradleSha -ne $gradleSha256) {
        throw "Gradle checksum mismatch: $actualGradleSha"
    }
    Expand-Archive -LiteralPath $gradleArchive -DestinationPath $gradleExpanded -Force
    $gradleBat = Join-Path $gradleExpanded 'gradle-9.5.0\bin\gradle.bat'
    Push-Location $repoRoot
    try {
        Invoke-CheckedNative -FilePath $gradleBat -ArgumentList @(
            'wrapper',
            '--gradle-version', '9.5.0',
            '--distribution-type', 'bin'
        )
    } finally {
        Pop-Location
    }
}

Write-Host "Android bootstrap ready: JDK $($GilnunAndroidEnvironment.JavaMajor), SDK $($GilnunAndroidEnvironment.AndroidSdkRoot)"
