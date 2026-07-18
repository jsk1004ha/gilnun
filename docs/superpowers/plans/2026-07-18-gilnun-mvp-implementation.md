# 길눈 AI Android·Web MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development for bounded implementation lanes. The user explicitly requested implementation first and one consolidated verification phase, so do not run intermediate test gates.

**Goal:** 동일한 안전 계약으로 동작하는 오프라인 Android APK와 배포 가능한 웹 데모를 완성한다.

**Architecture:** Android 단일 Compose 모듈이 통제된 로컬 WebView와 결정론적 Kotlin 규칙을 결합한다. 웹 데모는 같은 상태 계약과 사용자 흐름을 독립 구현하며, 서버·계정·외부 데이터 없이 브라우저 메모리에서 동작한다.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX WebKit, SharedPreferences/org.json, TypeScript/React/Vite 기반 Sites 웹 런타임, PowerShell 출고 스크립트.

---

## 구현 순서

### Task 1: 저장소·도구·문서 기준선

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `scripts/android-env.ps1`
- Create: `scripts/bootstrap-android.ps1`
- Create: `scripts/init-signing.ps1`
- Create: `scripts/preflight-device.ps1`

- [ ] Android `:app` 단일 모듈과 JDK 17/SDK 37 환경 복원 계약을 작성한다.
- [ ] 비밀을 홈의 `.gilnun` 밖으로 내보내지 않는 idempotent 서명 초기화 스크립트를 작성한다.
- [ ] final plan의 금지 권한·의존성·출력 규칙을 `.gitignore`와 스크립트에 반영한다.

### Task 2: 웹 제품 데모

**Files:**
- Create/Modify: `web/app/page.tsx`
- Create/Modify: `web/app/globals.css`
- Create/Modify: `web/app/layout.tsx`
- Create: `web/lib/gilnun.ts`

- [ ] 레이아웃 A/B, 학습자/도우미, 3회/6초 감지, 도움 확인, PatchV1 생성·재사용, 실패 안전, 도움 단계, 영수증, Reset을 구현한다.
- [ ] 320px~1440px와 200% 글자에서 동작하는 “차분한 공공 길찾기” 시각 시스템을 구현한다.
- [ ] 실제 개인정보·외부 네트워크·최종 제출이 없는 합성 데이터만 사용한다.

### Task 3: Android 프로젝트와 번들 화면

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/**`
- Create: `app/src/main/assets/welfare/index.html`
- Create: `app/src/main/assets/welfare/style.css`
- Create: `app/src/main/assets/welfare/app.js`

- [ ] Compose 앱과 WebViewAssetLoader 의존성을 구성한다.
- [ ] 웹 데모와 같은 의미 계약을 가진 A/B 합성 신청 화면을 번들한다.
- [ ] CSP, 48px 대상, 반응형/큰 글자, 실패 fixture를 포함한다.

### Task 4: 결정론적 코어와 단위 테스트

**Files:**
- Create: `app/src/main/java/com/gilnun/app/data/Models.kt`
- Create: `app/src/main/java/com/gilnun/app/guidance/StruggleDetector.kt`
- Create: `app/src/main/java/com/gilnun/app/guidance/PatchEngine.kt`
- Create: `app/src/main/java/com/gilnun/app/guidance/HelpPolicy.kt`
- Create: `app/src/main/java/com/gilnun/app/guidance/ReceiptEvaluator.kt`
- Create: `app/src/main/java/com/gilnun/app/data/DemoStateStore.kt`
- Create: `app/src/test/java/com/gilnun/app/**`

- [ ] 순수 Kotlin으로 감지·정확 일치 패치·도움 증감·영수증 판정을 구현한다.
- [ ] final plan의 happy/failure fixture를 실행 가능한 JUnit 테스트로 작성하되 아직 실행하지 않는다.
- [ ] 0개·중복·필드 불일치·사후조건 미도달 경로를 모두 fail closed로 만든다.

### Task 5: Android 셸·브리지·통합

**Files:**
- Create: `app/src/main/java/com/gilnun/app/MainActivity.kt`
- Create: `app/src/main/java/com/gilnun/app/GilnunViewModel.kt`
- Create: `app/src/main/java/com/gilnun/app/ui/GilnunApp.kt`
- Create: `app/src/main/java/com/gilnun/app/ui/GilnunTheme.kt`
- Create: `app/src/main/java/com/gilnun/app/web/GilnunBridge.kt`
- Create: `app/src/main/java/com/gilnun/app/web/DemoWebView.kt`

- [ ] exact-origin WebMessage 이벤트를 검증해 ViewModel reducer로 보낸다.
- [ ] 로드 전 cookie 제거 완료를 기다리고 네트워크·저장·외부 navigation을 차단한다.
- [ ] 역할/레이아웃 전환, 도움 시트, 행동 계약, 영수증, 패치 불일치, Reset UI를 통합한다.

### Task 6: 자동·수동 QA 자산과 출고 스크립트

**Files:**
- Create: `app/src/androidTest/java/com/gilnun/app/**`
- Create: `scripts/build-milestone.ps1`
- Create: `scripts/verify-boundaries.ps1`
- Create: `scripts/final-mvp-gate.ps1`
- Create: `scripts/build-release.ps1`
- Create: `docs/demo-state-contract.md`
- Create: `docs/demo-runbook.md`
- Create: `docs/pitch-script.md`
- Create: `docs/qa-checklist.md`

- [ ] A/B 수동 완주, 전체 데모 흐름, invalid patch, cookie/storage 경계 검사를 코드화한다.
- [ ] 모든 외부 명령의 exit code를 확인하고 비밀을 출력하지 않는 PowerShell 게이트를 작성한다.
- [ ] 4분 발표와 50초 무편집 백업 영상 촬영용 정확한 대본을 작성한다.

### Task 7: 마지막 통합 검증

- [ ] `npm run build`로 웹을 빌드한다.
- [ ] `gradlew clean testDebugUnitTest lintRelease assembleDebug assembleRelease`를 실행한다.
- [ ] `scripts/verify-boundaries.ps1`와 `scripts/final-mvp-gate.ps1`를 실행한다.
- [ ] release APK 서명과 SHA-256을 확인한다.
- [ ] 사용 가능한 Android 기기가 있으면 connected tests와 실기기 QA를 실행하고, 없으면 `DEVICE_BLOCKED` 증거를 남긴다.
- [ ] 실패가 있으면 구현으로 돌아가 수정한 뒤 전체 통합 게이트를 다시 실행한다.

### Task 8: 배포와 산출물 정리

**Files:**
- Create: `outputs/gilnun-mvp.apk`
- Create: `outputs/verification-summary.md`

- [ ] 검증된 release APK와 SHA-256을 사용자 산출물 폴더에 복사한다.
- [ ] 웹 빌드를 Sites에 저장·배포하고 프로덕션 URL을 기록한다.
- [ ] 자동 통과 항목, 기기 의존 항목, 남은 위험을 과장 없이 정리한다.

## 자체 점검

- Placeholder 없음.
- Android·웹·보안·출고·문서 범위를 모두 포함한다.
- 모든 의미 필드 이름은 최종 계획과 동일하다.
- 스트레치 Todo 19~22는 MVP F1~F4 이전에 구현하지 않는다.
- 구현과 테스트 코드는 먼저 작성하되 실행은 Task 7에 모은다.
