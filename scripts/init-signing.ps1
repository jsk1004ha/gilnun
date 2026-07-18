[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot 'android-env.ps1')

$signingDir = Join-Path $HOME '.gilnun'
$envFile = Join-Path $signingDir 'signing.env.ps1'
$keystore = Join-Path $signingDir 'release.jks'
$alias = 'gilnun-release'

function New-Secret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Protect-SingleQuotedPowerShell {
    param([Parameter(Mandatory)][string]$Value)
    return $Value.Replace("'", "''")
}

New-Item -ItemType Directory -Force -Path $signingDir | Out-Null

if (-not (Test-Path -LiteralPath $envFile)) {
    $storePassword = New-Secret
    $keyPassword = New-Secret
    $content = @(
        "`$env:GILNUN_KEYSTORE = '$(Protect-SingleQuotedPowerShell $keystore)'"
        "`$env:GILNUN_STORE_PASSWORD = '$(Protect-SingleQuotedPowerShell $storePassword)'"
        "`$env:GILNUN_KEY_ALIAS = '$alias'"
        "`$env:GILNUN_KEY_PASSWORD = '$(Protect-SingleQuotedPowerShell $keyPassword)'"
    )
    Set-Content -LiteralPath $envFile -Value $content -Encoding utf8

    $acl = New-Object System.Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $identity,
        [System.Security.AccessControl.FileSystemRights]::FullControl,
        [System.Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit',
        [System.Security.AccessControl.PropagationFlags]::None,
        [System.Security.AccessControl.AccessControlType]::Allow
    )
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $signingDir -AclObject $acl
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
        throw "Missing signing setting: $name"
    }
}

$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $env:GILNUN_KEYSTORE)) {
    & $keytool -genkeypair `
        -storetype JKS `
        -keystore $env:GILNUN_KEYSTORE `
        -storepass $env:GILNUN_STORE_PASSWORD `
        -keypass $env:GILNUN_KEY_PASSWORD `
        -alias $env:GILNUN_KEY_ALIAS `
        -keyalg RSA -keysize 3072 -validity 3650 `
        -dname 'CN=Gilnun Competition MVP, O=Gilnun, C=KR'
    $generationExitCode = $LASTEXITCODE
    if ($generationExitCode -ne 0) {
        throw "Release keystore generation failed with exit code $generationExitCode."
    }
}

& $keytool -list `
    -keystore $env:GILNUN_KEYSTORE `
    -storepass $env:GILNUN_STORE_PASSWORD `
    -alias $env:GILNUN_KEY_ALIAS | Out-Null
$validationExitCode = $LASTEXITCODE
if ($validationExitCode -ne 0) {
    throw "Release keystore alias or password validation failed with exit code $validationExitCode."
}

Write-Host "Signing material ready at $signingDir (secrets not displayed)."
