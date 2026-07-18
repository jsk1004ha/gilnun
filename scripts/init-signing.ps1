[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot 'android-env.ps1')

$signingDir = Join-Path $HOME '.gilnun'
$envFile = Join-Path $signingDir 'signing.env.ps1'
if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw "Existing release signing environment is required at $envFile. No key was created."
}

. $envFile

$required = @(
    'GILNUN_KEYSTORE',
    'GILNUN_STORE_PASSWORD',
    'GILNUN_KEY_ALIAS',
    'GILNUN_KEY_PASSWORD'
)
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing existing signing setting: $name. No key was created."
    }
}

$keystore = [System.IO.Path]::GetFullPath($env:GILNUN_KEYSTORE)
if (-not (Test-Path -LiteralPath $keystore -PathType Leaf)) {
    throw "Existing release keystore was not found at $keystore. No key was created."
}

$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
& $keytool -list `
    -keystore $keystore `
    -storepass $env:GILNUN_STORE_PASSWORD `
    -alias $env:GILNUN_KEY_ALIAS | Out-Null
$validationExitCode = $LASTEXITCODE
if ($validationExitCode -ne 0) {
    throw "Existing release keystore alias or password validation failed with exit code $validationExitCode."
}

$env:GILNUN_KEYSTORE = $keystore
Write-Host 'Existing release signing material verified (secrets not displayed; no key created).'
