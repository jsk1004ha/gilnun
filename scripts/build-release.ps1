[CmdletBinding()]
param([switch]$SkipConnected)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
& (Join-Path $PSScriptRoot 'final-mvp-gate.ps1') -SkipConnected:$SkipConnected
