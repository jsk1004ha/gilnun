[CmdletBinding()]
param([switch]$AsJson)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'android-env.ps1')

$adb = Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb was not found at $adb"
}
$rows = & $adb devices -l
$adbExitCode = $LASTEXITCODE
if ($adbExitCode -ne 0) { throw "adb devices failed with exit code $adbExitCode." }

$devices = @($rows | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' })
$status = if ($devices.Count -eq 1) { 'READY' } else { 'DEVICE_BLOCKED' }
$result = [pscustomobject]@{ status = $status; authorizedDeviceCount = $devices.Count }
if ($AsJson) { $result | ConvertTo-Json -Compress } else { Write-Host "$status - authorized Android devices: $($devices.Count)" }
