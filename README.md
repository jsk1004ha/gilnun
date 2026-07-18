# 길눈 AI Android·Web MVP

길눈 AI는 통제된 합성 복지 신청 화면에서 `막힘 후보 → 도움 확인 → 한 지점
해결 → 의미 패치 재사용 → 도움 강도 조절 → 검증 영수증` 폐루프를 오프라인으로
보여 주는 대회용 MVP입니다.

인터랙티브 웹 데모: https://gilnun-ai-jsk1004.js10041530.chatgpt.site

## 안전 범위

- Android APK는 앱에 번들된 `appassets` 화면만 엽니다.
- INTERNET, 접근성 서비스, 오버레이, 미디어 캡처 권한을 사용하지 않습니다.
- 좌표, selector, DOM 본문, 입력값, URL, 키 입력을 저장하지 않습니다.
- 결제·동의·최종 제출을 자동 실행하지 않으며 사용자가 화면 동작을 직접 합니다.
- 웹 데모도 실제 개인정보나 외부 서비스 없이 브라우저 메모리에서만 동작합니다.

## 구조

- `app/`: Kotlin, Jetpack Compose, AndroidX WebKit 기반 설치형 앱
- `web/`: 같은 상태 계약을 구현한 반응형 조작형 웹 데모
- `scripts/`: Android 환경 복원, 서명, 빌드, 개인정보·권한 경계 검증
- `docs/`: 상태 계약, 4분 데모 런북, 발표 대본, QA 체크리스트

## 빌드

Windows PowerShell 7에서:

```powershell
pwsh -ExecutionPolicy Bypass -File scripts/bootstrap-android.ps1 -SkipAndroidStudio
pwsh -ExecutionPolicy Bypass -File scripts/final-mvp-gate.ps1
```

웹만 빌드할 때:

```powershell
Set-Location web
npm install
npm run build
```

검증된 Android 산출물은 `outputs/gilnun-mvp.apk`, 검증 근거는
`outputs/verification-summary.md`에 정리됩니다.
