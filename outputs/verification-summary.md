# 길눈 Android APK v0.2.3 검증 요약

생성 시각: 2026-07-19T09:53:43.1504631+09:00

## AUTOMATED PASS

- JVM 회귀 및 신규 계약 테스트: PASS
- Android release lint: PASS
- debug APK / Android 테스트 APK / signed release APK 빌드: PASS
- 오프라인·권한·WebView·CSP 정적 경계 검사: PASS
- zipalign 및 apksigner 검증: PASS
- 패키지/버전: com.gilnun.app · 0.2.3 (5)
- 서명 인증서 SHA-256: 9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991

## DEVICE_SKIPPED — connected tests were explicitly skipped; no device PASS is claimed

- TalkBack 수동 탐색
- 한국어 TTS 실제 음성 및 음성 미설치 경로
- 시스템 글자 150% / 200% 및 큰 화면 표시
- API 26 / 37 오프라인 완주
- 실제 DOM을 구동하는 A→B 자동 재배치 후 의미 매칭 계측: 자동 실행 증거 없음(정적 의미 계약은 PASS)
- 오프라인 WebView 실제 로딩: 기기 미실행(packaged asset·네트워크 차단 검사는 PASS)
- 기존 v0.2.0 / v0.2.1 APK 위 업데이트 설치

기기가 연결되기 전에는 위 항목을 PASS로 표시하지 않습니다.

APK SHA-256: 552e104b48f0201af81527dd62504b790d5fff0ee91348f5a70adb74c33024b9
APK: outputs/gilnun-mvp.apk
