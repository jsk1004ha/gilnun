[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Get-JavaMajorVersion {
    param([Parameter(Mandatory)][string]$JavaExe)

    $versionLines = & $JavaExe -version 2>&1
    $exitCode = $LASTEXITCODE
    $output = $versionLines | Out-String
    if ($exitCode -ne 0 -or $output -notmatch 'version "(?:1\.)?(\d+)') {
        throw "Unable to read Java version from $JavaExe"
    }
    return [int]$Matches[1]
}

function Find-Temurin17Home {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    $candidates += Get-ChildItem -Directory -ErrorAction SilentlyContinue `
        -Path 'C:\Program Files\Eclipse Adoptium\jdk-17*' |
        Sort-Object Name -Descending |
        Select-Object -ExpandProperty FullName

    foreach ($candidate in $candidates | Select-Object -Unique) {
        $java = Join-Path $candidate 'bin\java.exe'
        if ((Test-Path -LiteralPath $java) -and (Get-JavaMajorVersion -JavaExe $java) -eq 17) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw 'Temurin JDK 17 was not found. Run scripts/bootstrap-android.ps1 first.'
}

function Add-PathEntry {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }
    $entries = $env:Path -split ';'
    if ($entries -notcontains $Path) {
        $env:Path = "$Path;$env:Path"
    }
}

function Set-GilnunAndroidEnvironment {
    $javaHome = Find-Temurin17Home
    $sdkRoot = if ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } elseif ($env:ANDROID_HOME) {
        $env:ANDROID_HOME
    } else {
        Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    }

    $sdkRoot = [System.IO.Path]::GetFullPath($sdkRoot)
    $sdkManager = Join-Path $sdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (-not (Test-Path -LiteralPath $sdkManager)) {
        throw "Android command-line tools were not found at $sdkManager. Run scripts/bootstrap-android.ps1 first."
    }

    $env:JAVA_HOME = $javaHome
    $env:ANDROID_SDK_ROOT = $sdkRoot
    $env:ANDROID_HOME = $sdkRoot

    Add-PathEntry (Join-Path $javaHome 'bin')
    Add-PathEntry (Join-Path $sdkRoot 'platform-tools')
    Add-PathEntry (Join-Path $sdkRoot 'cmdline-tools\latest\bin')
    Add-PathEntry (Join-Path $sdkRoot 'build-tools\36.0.0')

    $major = Get-JavaMajorVersion -JavaExe (Join-Path $javaHome 'bin\java.exe')
    if ($major -ne 17) {
        throw "JDK major must be exactly 17, found $major."
    }

    [pscustomobject]@{
        JavaHome = $javaHome
        JavaMajor = $major
        AndroidSdkRoot = $sdkRoot
    }
}

$script:GilnunAndroidEnvironment = Set-GilnunAndroidEnvironment
