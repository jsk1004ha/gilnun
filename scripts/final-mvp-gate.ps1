[CmdletBinding()]
param([switch]$SkipConnected)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$outputsDir = Join-Path $repoRoot 'outputs'
New-Item -ItemType Directory -Force -Path $outputsDir | Out-Null

& (Join-Path $PSScriptRoot 'build-milestone.ps1') `
    -Gate release24 `
    -SkipConnected:$SkipConnected

$artifactApk = Join-Path $repoRoot 'artifacts\gilnun-mvp.apk'
& (Join-Path $PSScriptRoot 'verify-boundaries.ps1') -ReleaseApk $artifactApk

$outputApk = Join-Path $outputsDir 'gilnun-mvp.apk'
Copy-Item -LiteralPath $artifactApk -Destination $outputApk -Force
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $outputApk).Hash.ToLowerInvariant()
Set-Content `
    -LiteralPath (Join-Path $outputsDir 'gilnun-mvp.apk.sha256') `
    -Value "$hash  gilnun-mvp.apk" `
    -Encoding ascii

$deviceEvidencePath = Join-Path $repoRoot 'artifacts\android-test-status.json'
if (-not (Test-Path -LiteralPath $deviceEvidencePath -PathType Leaf)) {
    throw "Connected-test evidence is missing: $deviceEvidencePath"
}
$deviceEvidence = Get-Content -LiteralPath $deviceEvidencePath -Raw | ConvertFrom-Json
$deviceStatus = switch ([string]$deviceEvidence.status) {
    'PASSED' {
        'DEVICE_PARTIAL — instrumentation actually passed; manual accessibility and update-install checks remain'
    }
    'BLOCKED' {
        "DEVICE_BLOCKED — preflight status $($deviceEvidence.preflightStatus); connected tests were not run"
    }
    'SKIPPED' {
        'DEVICE_SKIPPED — connected tests were explicitly skipped; no device PASS is claimed'
    }
    default {
        throw "Unknown connected-test evidence status: $($deviceEvidence.status)"
    }
}

$summary = @(
    '# 길눈 Android APK v0.2.1 검증 요약'
    ''
    "생성 시각: $([DateTimeOffset]::Now.ToString('o'))"
    ''
    '## AUTOMATED PASS'
    ''
    '- JVM 회귀 및 신규 계약 테스트: PASS'
    '- Android release lint: PASS'
    '- debug APK / Android 테스트 APK / signed release APK 빌드: PASS'
    '- 오프라인·권한·WebView·CSP 정적 경계 검사: PASS'
    '- zipalign 및 apksigner 검증: PASS'
    '- 패키지/버전: com.gilnun.app · 0.2.1 (3)'
    '- 서명 인증서 SHA-256: 9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991'
    ''
    "## $deviceStatus"
    ''
    '- TalkBack 수동 탐색'
    '- 한국어 TTS 실제 음성 및 음성 미설치 경로'
    '- 시스템 글자 150% / 200% 및 큰 화면 표시'
    '- API 26 / 37 오프라인 완주'
    '- 실제 DOM을 구동하는 A→B 자동 재배치 후 의미 매칭 계측: 자동 실행 증거 없음(정적 의미 계약은 PASS)'
    '- 오프라인 WebView 실제 로딩: 기기 미실행(packaged asset·네트워크 차단 검사는 PASS)'
    '- 기존 v0.1.0 APK 위 업데이트 설치'
    ''
    '기기가 연결되기 전에는 위 항목을 PASS로 표시하지 않습니다.'
    ''
    "APK SHA-256: $hash"
    'APK: outputs/gilnun-mvp.apk'
)
Set-Content `
    -LiteralPath (Join-Path $outputsDir 'verification-summary.md') `
    -Value $summary `
    -Encoding utf8

Write-Host 'FINAL_MVP_GATE_AUTOMATED_PASS'
Write-Host $deviceStatus
Write-Host "APK SHA-256 $hash"
Write-Host "Deliverables: $outputsDir"
