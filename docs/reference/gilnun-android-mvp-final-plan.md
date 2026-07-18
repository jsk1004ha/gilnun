# gilnun-android-mvp - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** 비행기 모드에서도 동작하는 설치형 Android APK입니다. 한 개의 합성 복지 신청 화면에서 막힘 후보를 감지하고, 청년이 한 번 해결한 지점을 변경된 화면에 재사용하며, 도움 강도와 검증 영수증까지 4분 안에 시연합니다.

**Why this approach:** 임의 앱을 흉내 내는 데 시간을 쓰지 않고, 대회의 핵심 차별점인 막힘→국소 해결→재사용→자립 지원 폐루프 하나를 실제로 끝냅니다. 화면 좌표 대신 앱이 제공한 안정 키를 써서 레이아웃 변경에도 버티고, 모든 위험 동작은 안내 전용으로 닫습니다.

**What it will NOT do:** 다른 앱을 읽거나 조작하지 않습니다. 클라우드 AI·백엔드·실제 개인정보·결제·동의·최종 제출 자동화가 없습니다. 장기 학습 효과나 세계 최초를 주장하지 않습니다.

**Effort:** Large — 24시간 임계경로, 8시간 fallback APK
**Risk:** High - 현재 PC에 Android SDK/adb가 없어 첫 빌드 환경 설치와 실기기 WebView 검증이 선행됩니다.
**Decisions I made for you:** Kotlin+Compose 단일 모듈, 앱 소유 로컬 WebView, 규칙 기반 감지, 한 기기 역할 전환, SharedPreferences, 안내 전용 MVP, minSdk 26을 기본값으로 정했습니다.

Your next move: 이 계획대로 구현을 시작하라고 지시하면 Todo 1의 Android 환경 준비부터 8시간 fallback을 먼저 확보합니다. Full execution detail follows below.

---

> TL;DR (machine): HEAVY / high-risk 24h plan; deliver one offline signed APK proving controlled friction detection, helper-authored stable-key patch reuse, reversible guidance, and verified receipts, with an 8h fallback.

## 앱 소개 — 길눈 AI가 무엇인가

### 한 줄 정의

길눈 AI는 고령 사용자가 디지털 화면에서 막히는 순간을 행동으로 감지하고, 청년이 그 지점만 한 번 해결하면 같은 곳에서 막히는 다른 사용자에게 해결 안내를 재사용하는 집단지성형 디지털 내비게이션이다.

핵심 문장: **“길을 대신 걸어주는 AI가 아니라, 모두가 다시 막히지 않도록 디지털 길을 고치는 AI.”**

### 해결하려는 문제

고령층의 디지털 격차는 스마트폰 보유 여부만의 문제가 아니다. 작은 버튼, 비슷한 선택지, 긴 스크롤, 낯선 아이콘 때문에 신청·예약·문서 전송 과정의 특정 한 단계에서 막히는 경우가 많다. 기존 사용법 영상은 전체 과정을 다시 보게 하고, 원격 지원은 사람이 매번 접속해야 하며, 일반 AI 자동화는 사용자가 무엇을 했는지 알기 어렵고 의존을 키울 수 있다.

길눈은 전체 작업을 대신하지 않는다. 실제 사용자가 막힌 **한 지점**을 찾아서 작은 해결 패치로 고치고, 그 해결을 다시 쓴다.

### 누가 사용하는가

- 고령 학습자: 앱 안의 디지털 과업을 직접 수행하고 필요한 순간에만 도움을 받는다.
- 청년 도우미: 전체 매뉴얼을 만드는 대신 막힌 한 단계만 20~30초 안에 해결해 패치로 저장한다.
- 향후 공공기관·서비스 운영자: 어떤 화면에서 반복적으로 막히는지 익명 집계로 확인하고 원래 화면을 개선한다. 이 기관 기능은 MVP 이후다.

### 앱이 작동하는 방식

1. 사용자가 같은 비진행 버튼을 6초 안에 세 번 누르거나 직접 도움을 요청한다.
2. 길눈은 사용자가 무능하다고 판단하지 않고 “도움이 필요하신가요?”라고 확인한다.
3. 이미 검증된 패치가 있으면 올바른 버튼과 다음 행동을 설명·강조한다.
4. 패치가 없으면 같은 APK의 청년 모드에서 도우미가 그 한 단계만 해결한다.
5. 앱은 좌표가 아니라 화면 ID, 버전, 안정 키, 버튼 역할, 접근성 이름, 예상 결과를 패치로 저장한다.
6. 버튼 위치·글자 크기·카드 순서가 달라져도 의미 정보가 같으면 패치를 재사용한다.
7. 정보가 하나라도 맞지 않으면 다른 버튼을 추측하지 않고 안내를 중단한다.
8. 사용자가 직접 누르고 실제 다음 상태에 도달했을 때만 완료 영수증을 보여준다.
9. 성공하면 다음 시도의 도움을 한 단계 줄이고, 다시 어려워하면 즉시 높인다.

### 핵심 기능

| 기능 | 사용자에게 보이는 결과 |
| --- | --- |
| 막힘 후보 감지 | 반복 오터치·직접 요청 뒤 도움 여부를 먼저 물음 |
| 국소 복구 패치 | 청년이 전체 과업이 아니라 막힌 버튼 한 개만 해결 |
| 안정 키 재사용 | 레이아웃이 달라져도 같은 의미의 버튼을 다시 찾음 |
| 실패 안전 | 화면 정보가 다르면 자동으로 다른 버튼을 고르지 않음 |
| 단계적 도움 축소 | 상세 설명→강조→짧은 힌트→관찰로 도움을 줄임 |
| 행동 계약·영수증 | 하기 전 범위와 한 일을 보여주고 실제 결과를 구분 |
| 막힘 지도 | 향후 어떤 화면이 많이 막히는지 익명 집계해 개선에 활용 |

### 기존 방식과 다른 점

| 기존 방식 | 한계 | 길눈 |
| --- | --- | --- |
| 사용법 영상 | 전체 과정을 정적으로 다시 봐야 함 | 실제로 막힌 한 지점에만 개입 |
| 좌표 매크로 | 화면 배치가 바뀌면 실패 | 안정 키와 의미 속성으로 다시 확인 |
| 일반 AI 에이전트 | 매번 즉흥 판단하고 행동 범위가 불명확 | 청년이 검증한 좁은 패치만 적용 |
| 원격 지원 | 같은 문제도 사람이 매번 접속 | 한 번 해결한 지점을 반복 재사용 |
| 고령자 간편 모드 | 모두에게 같은 단순 UI | 개인의 성공·실패에 따라 도움 강도 조절 |

### 이번 대회 MVP가 증명하는 것

이번 APK는 임의의 다른 앱을 조작하는 범용 AI가 아니다. 앱 안에 포함된 합성 복지 신청 화면에서 다음 폐루프가 실제로 동작함을 증명한다.

**막힘 후보 감지 → 사용자 확인 → 청년의 한 지점 해결 → 안정 키 패치 생성 → 변경된 화면에 재사용 → 도움 축소·복구 → 결과 검증 영수증**

MVP에는 서버·LLM·실제 개인정보·AccessibilityService·결제·동의·최종 제출 자동화가 없다. 브랜드는 길눈 AI를 사용하되 발표에서는 **온디바이스 규칙 기반 연구 시제품**이라고 정확히 밝힌다.

## 친구에게 바로 넘길 실행 지시

### 우리가 24시간 안에 만들 것

Android 앱 하나를 만든다. 앱 안에는 가짜 복지관 신청 페이지가 들어 있고 인터넷 없이 동작한다. 사용자가 임시 저장을 세 번 잘못 누르면 길눈이 도움을 물어본다. 청년 모드에서 내용 확인 버튼을 한 번 지정하면, 버튼 위치와 글자 크기가 바뀐 다른 화면에서도 같은 버튼을 찾아 강조한다. 사용자가 직접 누른 뒤 실제 다음 화면으로 넘어갔을 때만 성공 영수증을 보여준다.

### 준비물

- Windows 개발 PC 1대. 현재 PC에는 Android SDK와 adb가 없으므로 첫 작업은 설치다.
- Android 실기기 1대와 USB 케이블. 개발자 옵션·USB 디버깅을 켜고 PC 연결 승인까지 받아야 한다.
- 8시간만 남아도 제출할 수 있도록 기능을 아래 순서대로 만든다.

### 시간순 작업

| 시간 | 지금 해야 할 일 | 이 시점에 남겨야 할 결과 |
| --- | --- | --- |
| 0~2시간 | Android Studio/SDK 설치, 빈 Compose 앱 빌드, 휴대폰에 설치 | 휴대폰에서 켜지는 첫 APK |
| 2~4시간 | 앱 안에 복지 신청 화면 A/B와 학습자·청년 전환 구현 | 비행기 모드에서 두 화면 모두 수동 완주 |
| 4~6시간 | WebView가 탭 대상과 진행 상태를 Android로 보내게 하고 3회 반복 탭 감지 | 임시 저장 3회에 도움 질문 1회 |
| 6~8시간 | 미리 넣어 둔 review-next 패치로 버튼 강조, 사용자 탭, 결과 영수증 | 서명된 gilnun-fallback8.apk — 여기까지가 비상 제출선 |
| 8~11시간 | 청년 모드에서 올바른 버튼 한 개를 직접 지정해 패치 생성 | 하드코딩 패치가 아니라 실제 생성 패치 |
| 11~14시간 | A에서 만든 패치를 B의 이동된 버튼에 재사용, 불일치 시 중단 | 서명된 gilnun-mvp14.apk — 핵심 차별점 완성 |
| 14~18시간 | 도움 단계 3→2→1→0, 실패 시 복구, 행동 계약·영수증·Demo Reset | 전체 4분 시연 흐름 |
| 18~24시간 | 큰 글자·오프라인·개인정보·실기기 회귀, 서명 APK, 백업 영상 | 최종 gilnun-mvp.apk와 50초 백업 영상 |

### 3명이면 이렇게 나눈다

| 담당 | 맡을 일 | 서로 맞춰야 하는 계약 |
| --- | --- | --- |
| A — Android | 프로젝트/빌드, Compose 셸, WebView 브리지, 감지기, 패치 엔진, APK | pageId, stableKey, checkpoint 메시지 |
| B — 화면/UX | 가짜 복지 신청 HTML/CSS/JS, A/B 레이아웃, 큰 글자, 강조 효과 | save-draft, review-next, review-ready 이름을 바꾸지 않음 |
| C — QA/발표 | 실기기 테스트, 실패 fixture, 개인정보·오프라인 검사, 대본·영상 | 매 시간 게이트 APK를 받아 즉시 설치 시험 |

한 명이면 표의 시간순으로 그대로 진행한다. 파일을 동시에 수정할 때는 A가 Kotlin, B가 assets/welfare, C가 scripts와 docs를 맡아 충돌을 피한다.

### 4분 발표에서 보여줄 것

1. 레이아웃 A의 버튼 좌표를 그대로 쓰면 레이아웃 B에서 빗나가는 비교 장면.
2. 학습자가 임시 저장을 세 번 눌러도 진행되지 않아 길눈이 도움 여부를 묻는 장면.
3. 등록 패치가 없어서 청년 모드로 바꾸고 청년이 내용 확인 버튼 한 지점만 해결하는 장면.
4. 그 패치를 저장하고 버튼 위치·글자 크기가 달라진 B로 바꾸는 장면.
5. 길눈이 좌표가 아니라 review-next 안정 키로 올바른 버튼만 강조하는 장면.
6. 사용자가 직접 눌러 review-ready가 된 뒤에만 검증 완료 영수증을 보여주는 장면.
7. 다음 시도의 도움 강도가 한 단계 낮아지고, 다시 어려우면 즉시 올라가는 장면.

발표 첫 문장: “길눈은 범용 AI가 아닙니다. 고령 사용자가 막힌 지점을 발견하고, 한 번 검증한 해결을 참여 서비스 안에서 안전하게 재사용하는 온디바이스 안내 계층입니다.”

발표 마지막 문장: “길을 대신 걸어주는 AI가 아니라, 모두가 다시 막히지 않도록 디지털 길을 고치는 AI입니다.”

### 시간이 부족할 때 자르는 순서

1. 스트레치 기능 전부.
2. TTS와 애니메이션.
3. 도움 단계 UI의 장식.
4. 청년의 실시간 패치 생성 — 8시간 비상판에서는 미리 넣은 패치로 대체.

절대 자르지 않는 것: A/B 화면, 반복 탭 감지, 사용자 확인, 안정 키 일치, 불일치 실패 안전, 사용자 직접 탭, 검증 영수증, 오프라인 APK.

### 절대 만들지 말 것

- 다른 앱을 읽는 AccessibilityService.
- 서버, 로그인, LLM API, 실제 개인정보.
- 결제·동의·제출 자동 클릭.
- 좌표 실패 시 대충 비슷한 버튼을 누르는 fallback.
- “임의 앱 지원”, “AI가 화면을 이해”, “학습 효과 입증”, “세계 최초”라는 발표 문구.

## Scope
### Must have
- 결과물은 설치 가능한 단일 Android APK다. 패키지는 com.gilnun.app, 모듈은 :app 하나, minSdk 26이다.
- APK 안에 스마트폰 기초교육 신청 샌드박스를 번들한다. 레이아웃 A/B는 카드 순서·버튼 위치·글자 크기만 다르고 의미 계약은 같다.
- 시연 폐루프는 학습자 마찰 후보 감지 → 도움 확인 → 같은 APK의 도우미가 한 지점 해결 → 패치 저장 → B에서 재사용 → 도움 한 단계 축소/즉시 복구 → 결과 영수증이다.
- MVP 마찰 신호는 직접 도움 요청과 같은 비진행 의미 대상 3회/6초뿐이다. 체크포인트가 바뀌거나 입력·로딩 중이면 감지하지 않는다.
- 패치는 pageId, compatibleRevision, stableKey, role, accessibleName, expectedState를 모두 일치시킨다. 0개·중복·불일치면 추측 없이 중단한다.
- 기본 지원은 설명과 DOM 내부 강조이며 사용자가 직접 누른다. 내용 확인 화면까지만 가고 최종 신청·동의·결제는 실행하지 않는다.
- 영수증은 안내 표시, 사용자 행동 관찰, 사후조건 검증을 구분하고 검증된 전이만 완료라고 쓴다.
- 원시 행동 이벤트는 메모리에만 두고, SharedPreferences에는 패치·도움 단계·마지막 최소 영수증만 작은 org.json으로 저장한다.
- 비행기 모드, 큰 글자 200%, 좁은 화면, 레이아웃 변형, 앱 재시작과 Demo Reset을 포함한 실기기 시연이 가능해야 한다.
- C1~C6을 모두 충족한 뒤 서명 APK, 4분 시연 대본, 50초 무편집 백업 영상, 검증 증거를 산출한다.

#### 데모 과업과 상태 계약

| 항목 | 고정값 |
| --- | --- |
| 과업 | 스마트폰 기초교육 신청 내용 확인하기 |
| 막힘 지점 | 큰 임시 저장 버튼을 반복 누르고 작은 내용 확인 버튼을 찾지 못함 |
| 비진행 대상 | save-draft; 눌러도 checkpoint가 consent-ready에서 변하지 않음 |
| 패치 대상 | review-next; role=button; accessibleName=신청 내용 확인 |
| 성공 사후조건 | checkpoint=review-ready |
| 안전 경계 | 실제 제출 없음, 합성 정보만 사용, 최종 신청 버튼 없음 |
| A/B 차이 | 순서·위치·글자 배율·뷰포트만 변경; stableKey와 의미 계약 유지 |

### Must NOT have (guardrails, anti-slop, scope boundaries)
- AccessibilityService, SYSTEM_ALERT_WINDOW, MediaProjection, OCR, 스크린샷, 임의 앱/사이트 조작.
- INTERNET 권한, 백엔드, 계정, 클라우드 LLM, 분석·광고·원격 크래시 SDK, 원격 도우미.
- 실제 개인정보에서 파생된 데이터, 실제 복지·건강 기록, 키 입력·화면 원문·URL·음성 저장.
- 결제·송금·약관 동의·개인정보 제출·삭제·설정 변경·최종 신청의 자동 실행.
- 좌표·XPath·CSS selector 저장, fuzzy match, 이름 유사도, 일치 실패 시 좌표 폴백.
- addJavascriptInterface 또는 와일드카드 origin 브리지; 외부 탐색과 네트워크 로드.
- Room, Hilt, Repository/UseCase 인터페이스, 멀티모듈, 플러그인 아키텍처, 범용 규칙 엔진.
- AI가 화면을 이해한다, 장기 자립 효과를 입증했다, 임의 앱을 지원한다, 세계 최초라는 주장.
- MVP 통과 전에 체류시간·스크롤 왕복·A-B-A-B 감지, TTS, QR 공유, 자동 클릭을 추가하는 것.

#### 기본 결정과 사용자가 바꿀 수 있는 가정

| 결정 | 기본값 | 이유 |
| --- | --- | --- |
| 배포 | sideload용 서명 APK | 대회에서 Play 심사를 기다리지 않음 |
| 일정 | 24시간 임계경로, 8시간 fallback | 시간이 짧아도 수직 슬라이스를 남김 |
| 스택 | Kotlin, Compose, AGP 9.3.0, Gradle 9.5.0, JDK 17, compileSdk 37 | 2026-07 공식 안정 조합 |
| 상태 | ViewModel 한 곳 + 구체 클래스 직접 조합 | 앱 하나에 DI 계층이 불필요 |
| 저장 | SharedPreferences + org.json | 패치 하나와 상태 몇 개뿐 |
| AI | 온디바이스 결정론 규칙 | 실제 동작을 설명·검증할 수 있음 |
| 도우미 | 한 기기 역할 전환 | 계정·서버·두 번째 기기 제거 |
| 자동화 | MVP는 안내 전용 | 잘못된 클릭 위험과 정책 부담 제거 |

#### 시간 게이트

| 게이트 | 반드시 남는 결과 | 실패하면 즉시 자를 것 |
| --- | --- | --- |
| 0~4h | 도구 설치, Compose 셸, 오프라인 A/B WebView, 수동 완주 | 장식·애니메이션 |
| 4~8h | 이벤트/체크포인트, 반복 탭 감지, 미리 탑재한 패치 강조, 기본 영수증, fallback8 서명 APK | 도우미 패치 생성 |
| 8~14h | 도우미가 한 지점 패치를 실제 생성하고 B에서 재사용, mvp14 서명 APK | 다단계 패치 |
| 14~18h | 가역 도움 단계, 행동 계약, 정확한 영수증, 5초 Reset | TTS |
| 18~24h | 접근성·오프라인·개인정보·실기기 회귀, 서명 APK, 발표 백업 | 모든 스트레치 |

현재 PC에는 Android SDK, adb, ANDROID_HOME이 없고 winget은 사용 가능하다. 따라서 Todo 1은 선택이 아니라 빌드 선행조건이다.

## Verification strategy
> 자동 검사는 실행자가 끝까지 수행한다. 실기기 연결 시 사용자가 휴대폰 잠금 해제와 USB 디버깅 신뢰 승인을 한 번 하는 것은 명시적 전제다.
- Test decision: 비즈니스 규칙은 TDD(JUnit4), WebView/Compose 흐름은 AndroidX Test·Espresso, 정적 경계는 Gradle lint·apkanalyzer·adb, 실기기 화면은 체크리스트와 캡처로 검증한다.
- RED/GREEN 규칙: StruggleDetector, PatchEngine, HelpPolicy, Receipt 판정은 먼저 실패 테스트를 실행해 RED 증거를 남기고 최소 구현 후 같은 테스트의 GREEN 증거를 남긴다.
- 전체 자동 게이트: ./gradlew clean testDebugUnitTest lintRelease assembleDebug
- 기기 게이트: ./gradlew connectedDebugAndroidTest
- 릴리스 게이트: ./gradlew clean testDebugUnitTest lintRelease assembleRelease 후 apksigner verify --verbose --print-certs.
- 증거 기본 경로: .omo/evidence/gilnun-android-mvp/task-N/; 각 작업은 red.txt, green.txt, screenshot.png, logcat.txt, 또는 result.json 중 필요한 최소 파일을 남긴다.
- 실기기 수동 항목도 실행자가 직접 조작하고 화면 녹화/스크린샷을 증거로 남긴다. 외부 참가자 응답은 APK 완료의 필수 조건이 아니다.
- 실패 한계: WebMessage 기능 미지원, origin 불일치, 패치 0개/중복/불일치, 사후조건 미도달, 네트워크 시도 중 하나라도 발생하면 도움을 중단하고 실패 상태를 보인다.

#### 핵심 테스트 오라클

| 대상 | Happy PASS | Failure PASS |
| --- | --- | --- |
| Detector | save-draft 3회/6초, 동일 checkpoint에서 제안 정확히 1회 | 정상 버튼, 스크롤, 입력, 로딩, 거절 cooldown은 제안 0회 |
| PatchEngine | A에서 만든 review-next가 B에서 유일하게 해석·강조 | 키 없음/중복, role/name/revision 불일치 모두 PATCH_UNAVAILABLE |
| HelpPolicy | 검증 성공 뒤 최대 한 단계 감소 | 요청·마찰·실패 즉시 한 단계 상승, 0..3 범위 유지 |
| Receipt | review-ready 확인 뒤 VERIFIED | 클릭만 관찰하고 상태 불변이면 UNVERIFIED |
| Privacy | canary 입력 후 logcat/SharedPreferences에 0건 | canary가 한 번이라도 발견되면 출고 중단 |
| Offline | 재부팅+비행기 모드 3회 전체 완주 | INTERNET 권한 또는 외부 요청 1건이면 실패 |
| Accessibility | 100/150/200%에서 핵심 제어 미잘림, 48dp, TalkBack 이름 | 대상 강조 이탈·텍스트 겹침·이름 없음 중 하나면 실패 |
| Stage | 콜드런 10회, Reset 5초 이내 | 10회 중 1회라도 잘못된 대상/정지면 원인 수정 후 다시 0부터 |

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.

| Wave | 시간 | 작업 | 병렬 규칙 | 종료 게이트 |
| --- | --- | --- | --- | --- |
| 0 | 0~4h | 1~4 | 1 뒤 2와 3 병렬, 4가 통합 | 수동 오프라인 A/B 완주 |
| 1 | 4~8h | 5~8 | 5 뒤 6과 7 병렬, 8이 fallback 통합 | 감지→확인→기본 패치→영수증 |
| 2 | 8~14h | 9~12 | 9 뒤 10과 11 병렬, 12가 통합 | A에서 생성한 패치가 B에서 재사용 |
| 3 | 14~18h | 13~15 | 13과 14 병렬, 15가 통합 | 도움 축소/복구·계약·Reset |
| 4 | 18~24h | 16~18 | 16과 17 병렬, 18이 출고 | 서명 APK와 10회 콜드런 |
| S | MVP 후 | 19~22 | 19~21 독립, 22는 모든 안전 게이트 뒤 | 시간 남을 때만 선택 실행 |

한 명이면 번호 순서대로 한다. 2~3명이면 Android 코어(1,3,5~7,9,13~15), Web/UX(2,4,8,10~12,16), QA/발표(17~18)를 맡되 한 작업의 최종 책임자는 한 명만 둔다.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | - | 2~18 | - |
| 2 | 1 | 4,5,7,8 | 3 |
| 3 | 1 | 4~12 | 2 |
| 4 | 2,3 | 8,12,16~18 | - |
| 5 | 2,3 | 6~12 | - |
| 6 | 5 | 8,12,13,17 | 7 |
| 7 | 3,5 | 8,10~12 | 6 |
| 8 | 4,6,7 | 12,18 | - |
| 9 | 7 | 10~12,15 | - |
| 10 | 5,9 | 12 | 11 |
| 11 | 7,9 | 12,15,17 | 10 |
| 12 | 8,10,11 | 13~18 | - |
| 13 | 6,12 | 15,18 | 14 |
| 14 | 8,11,12 | 15,17,18 | 13 |
| 15 | 13,14 | 17,18 | - |
| 16 | 12,15 | 18 | 17 |
| 17 | 11,14,15 | 18 | 16 |
| 18 | 16,17 | 19~22 | - |
| 19 | 18 | - | 20,21 |
| 20 | 18 | - | 19,21 |
| 21 | 18 | - | 19,20 |
| 22 | 18, 모든 안전 테스트 | - | - |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->

- [ ] 1. Android 도구 체인과 단일 모듈 빌드 기준선 만들기 [C1, C6] [0~4h]
  What to do: scripts/bootstrap-android.ps1, android-env.ps1, init-signing.ps1, preflight-device.ps1을 먼저 만든다. clean host의 bootstrap은 예외적으로 android-env를 먼저 호출하지 않는다. 먼저 Windows 기본 winget만으로 Android Studio 2026.1.2.10과 EclipseAdoptium.Temurin.17.JDK를 없을 때 설치하고 설치 성공을 확인한 뒤에만 android-env.ps1을 dot-source한다. android-env.ps1은 Temurin 17 설치 경로와 %LOCALAPPDATA%\Android\Sdk를 매번 다시 찾아 JAVA_HOME, ANDROID_SDK_ROOT, PATH를 현재 호출 프로세스에 설정하고 java -version의 major가 정확히 17인지 확인한다. bootstrap의 설치 이후 단계와 init-signing, build-milestone, verify-boundaries, final-mvp-gate는 모두 android-env.ps1을 dot-source하므로 새 PowerShell 세션에서도 같은 환경을 재구성한다. sdkmanager가 없으면 공식 commandlinetools-win-14742923_latest.zip을 고정 URL로 내려받고 스크립트에 기록한 공식 checksum과 일치할 때만 cmdline-tools\latest에 푼다. 라이선스 수락 뒤 platform-tools, platforms;android-37, build-tools;36.0.0을 설치하고 local.properties를 만든다. 고정 checksum의 Gradle 9.5로 wrapper를 생성한다. settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/*, gradle.properties, app/build.gradle.kts, app/src/main/AndroidManifest.xml, .gitignore를 만든다. init-signing.ps1은 사용자 홈 .gilnun에 암호학적 난수 비밀번호, release.jks, 고정 alias gilnun-release, ACL로 현재 사용자만 읽는 signing.env.ps1을 최초 1회 만들고 keytool로 alias를 검증한다. build-milestone.ps1은 android-env.ps1과 signing.env.ps1을 dot-source해 GILNUN_KEYSTORE, GILNUN_STORE_PASSWORD, GILNUN_KEY_ALIAS, GILNUN_KEY_PASSWORD를 매번 복원하며 어떤 비밀도 stdout/evidence/Git에 쓰지 않는다. preflight-device는 승인된 device 1대가 없으면 실기기 게이트만 DEVICE_BLOCKED로 기록한다.
  Must NOT do: 다중 모듈, Hilt/Room/네트워크/분석 SDK, kotlin-android 중복 플러그인, 키스토어·비밀번호 커밋.
  Parallelization: Wave 0 | Blocked by: 없음 | Blocks: 2~18.
  References: .omo/drafts/gilnun-android-mvp.md; https://developer.android.com/studio; https://developer.android.com/tools/sdkmanager; https://developer.android.com/build/releases/agp-9-3-0-release-notes; https://developer.android.com/build/migrate-to-built-in-kotlin.
  Acceptance criteria: 서로 다른 깨끗한 PowerShell 프로세스에서 bootstrap-android.ps1과 init-signing.ps1을 두 번씩 실행해 모두 exit 0이고 idempotent다. 이어 새 프로세스의 build-milestone.ps1이 JDK 17, SDK 도구, 4개 signing env를 자체 복원해 release APK를 서명한다. ./gradlew :app:assembleDebug가 APK를 만들고 preflight-device 결과가 READY 또는 명시적 DEVICE_BLOCKED다.
  QA scenarios: happy — JDK가 없는 상태를 모사한 테스트에서 bootstrap이 strict env 검사 전에 winget install 경로를 선택하고, 설치 완료 뒤 환경 검사를 통과한다. 이어 새 PowerShell 프로세스 2개에서 bootstrap/init-signing/build smoke를 실행하고 비밀을 제외한 버전·경로·checksum만 green.txt에 저장; failure — 설치 실패, JDK major, command-line tools checksum, 라이선스, signing.env ACL/alias/비밀번호 중 하나를 틀리면 해당 단계에서 nonzero exit이고 artifact와 비밀 로그가 0건. Evidence .omo/evidence/gilnun-android-mvp/task-1/.
  Commit: Y | chore(android): bootstrap single-module offline app

- [ ] 2. 합성 복지 신청 화면과 레이아웃 A/B 만들기 [C1] [0~4h]
  What to do: app/src/main/assets/welfare/index.html, app.js, style.css를 만든다. 과업은 프로그램 선택 → 필수 항목 확인 → 신청 내용 확인 화면까지다. save-draft는 비진행 대상, review-next는 patchable target, review-ready는 성공 checkpoint다. query 또는 네이티브 메시지로 A/B를 전환하고 A/B는 위치·순서·글자 크기만 바꾼다. 모든 조작 요소에 data-gilnun-key, role, accessible name, checkpoint/expected-state 계약을 넣는다.
  Must NOT do: 실제 기관 로고·실제 개인정보·CDN·외부 폰트/이미지·최종 제출 버튼·좌표/XPath 기반 식별.
  Parallelization: Wave 0 | Blocked by: 1 | Blocks: 4,5,7,8 | Can parallelize with: 3.
  References: .omo/ulw-research/20260718-200209/SYNTHESIS.md의 사용자 흐름·안전 경계; WCAG 2.2 target size.
  Acceptance criteria: A와 B 모두 네트워크 없이 review-ready까지 수동 진행되고 review-next의 stableKey/role/name/expectedState는 동일하다. 핵심 버튼 CSS 최소 높이는 48px다.
  QA scenarios: happy — WebView 또는 브라우저 로컬 fixture에서 A/B 각각 완주 화면 캡처; failure — B에서 review-next 키를 제거한 fixture가 명시적 contract error를 표시. Evidence .omo/evidence/gilnun-android-mvp/task-2/layout-a.png, layout-b.png, contract-failure.png.
  Commit: Y | feat(demo): add synthetic welfare flow variants

- [ ] 3. 안전한 로컬 WebView와 최소 Compose 셸 구현하기 [C1, C5] [0~4h]
  What to do: app/src/main/java/com/gilnun/app/MainActivity.kt, GilnunViewModel.kt, ui/GilnunApp.kt, web/DemoWebView.kt를 만든다. WebViewAssetLoader로 https://appassets.androidplatform.net/assets/welfare/index.html만 로드하고 학습자/도우미 역할 칩, A/B 전환, 도움 버튼을 한 화면에 둔다. 첫 loadUrl 전에 CookieManager.setAcceptCookie(false), setAcceptThirdPartyCookies(webView,false)를 적용하고 removeAllCookies callback 완료와 flush 뒤에만 페이지를 연다. 외부 URL은 차단하고 blockNetworkLoads=true, file/content access=false, mixed content=never, cacheMode=LOAD_NO_CACHE, domStorage/database/saveFormData=false, importantForAutofill=NO_EXCLUDE_DESCENDANTS, release debugging=false로 고정한다. Manifest는 usesCleartextTraffic=false이고 로컬 HTML은 외부 연결을 막는 CSP와 autocomplete=off를 가진다.
  Must NOT do: Navigation 프레임워크, 별도 Activity/Fragment, system overlay, 외부 브라우징, addJavascriptInterface.
  Parallelization: Wave 0 | Blocked by: 1 | Blocks: 4~12 | Can parallelize with: 2.
  References: https://developer.android.com/develop/ui/views/layout/webapps/load-local-content; https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges.
  Acceptance criteria: 앱을 실행하면 번들 화면만 열리고 학습자/도우미 및 A/B 전환이 즉시 반영된다. 외부 링크와 외부 요청은 BLOCKED_NAVIGATION으로 끝나며 WebStorage/Cookie/cache/autofill 저장이 비활성이다.
  QA scenarios: happy — ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gilnun.app.LocalWebViewSmokeTest; failure — https://example.com 이동과 first-load/third-party cookie, localStorage, form-autofill 저장을 시도하고 force-stop·재부팅 뒤 페이지 이탈 0건, CookieManager 값 0건, 값 복원 0건. Evidence .omo/evidence/gilnun-android-mvp/task-3/.
  Commit: Y | feat(shell): host owned offline webview

- [ ] 4. 4시간 수직 슬라이스와 A/B 수동 완주를 잠그기 [C1, C6] [0~4h]
  What to do: app/src/androidTest/java/com/gilnun/app/ManualJourneyTest.kt를 추가해 A/B 각각 review-ready까지 진행한다. docs/demo-state-contract.md에 pageId, compatibleRevision, stableKey, checkpoint, 금지 동작을 한 페이지로 고정한다. Compose 상태와 WebView 생명주기를 회전/재개 때 다시 로드할 수 있게 최소 처리한다.
  Must NOT do: 감지·패치 생성·도움 정책을 이 작업에 끼워 넣거나 스크립트 성공을 사용자 연구 결과처럼 표현.
  Parallelization: Wave 0 integration | Blocked by: 2,3 | Blocks: 8,12,16~18.
  References: app/src/main/assets/welfare/*; app/src/main/java/com/gilnun/app/web/DemoWebView.kt.
  Acceptance criteria: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gilnun.app.ManualJourneyTest가 A/B 2개 테스트를 모두 통과한다.
  QA scenarios: happy — A/B 정상 완주; failure — expectedState를 잘못 설정한 테스트 복사본이 review-ready 단언에서 RED가 된 뒤 원복. Evidence .omo/evidence/gilnun-android-mvp/task-4/red.txt와 green.txt.
  Commit: Y | test(demo): lock offline A-B vertical slice

- [ ] 5. DOM↔Android 메시지·체크포인트 계약 구현하기 [C2, C3, C5] [4~8h]
  What to do: data/Models.kt, web/GilnunBridge.kt와 assets/welfare/app.js의 메시지 계층을 만든다. InteractionEvent는 schemaVersion, type, pageId, compatibleRevision, stableKey, role, accessibleName, checkpoint, monotonicMs만 받는다. exact origin rule 하나로 WebViewCompat.addWebMessageListener를 사용하고 허용 필드·길이·enum을 파싱 시 검증한다. WEB_MESSAGE_LISTENER 미지원이면 도움 기능만 닫고 수동 과업은 유지한다.
  Must NOT do: form value, innerText 전체, URL, 좌표, 키 입력, 임의 JSON, 와일드카드 origin 저장 또는 로그 출력.
  Parallelization: Wave 1 | Blocked by: 2,3 | Blocks: 6~12.
  References: https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge; .omo/ulw-research/20260718-200209/SYNTHESIS.md 기술 구조.
  Acceptance criteria: ./gradlew testDebugUnitTest --tests com.gilnun.app.web.GilnunBridgeTest가 정상 이벤트와 모든 거부 fixture를 통과하고 앱 디버그 칩에 event type/stableKey/checkpoint만 보인다.
  QA scenarios: happy — exact origin의 유효 TARGET_TAP 수신; failure — 다른 origin, 알 수 없는 type, 4KB 초과, 누락 필드가 모두 INVALID_EVENT이고 상태를 바꾸지 않음. Evidence .omo/evidence/gilnun-android-mvp/task-5/red.txt, green.txt.
  Commit: Y | feat(bridge): validate minimal semantic events

- [ ] 6. 반복 비진행 탭 감지기를 TDD로 구현하기 [C2] [4~8h]
  What to do: guidance/StruggleDetector.kt와 app/src/test/java/com/gilnun/app/guidance/StruggleDetectorTest.kt를 만든다. 주입된 monotonic clock으로 같은 stableKey 3회/6000ms, 동일 checkpoint일 때 후보 1회를 내고 checkpoint 변경·입력 focus·loading·유효 진행·cooldown에서 상태를 초기화/억제한다. 직접 도움 요청은 즉시 후보를 낸다.
  Must NOT do: 나이/인지능력 추론, 체류·스크롤·왕복 규칙, 실제 delay/sleep, 후보 발생 즉시 클릭.
  Parallelization: Wave 1 | Blocked by: 5 | Blocks: 8,12,13,17 | Can parallelize with: 7.
  References: SYNTHESIS.md의 막힘 후보 규칙; .omo/ulw-research/20260718-200209/wave-2-verification.md.
  Acceptance criteria: 먼저 대상 테스트 RED를 저장하고 구현 후 ./gradlew testDebugUnitTest --tests com.gilnun.app.guidance.StruggleDetectorTest GREEN. save-draft 3회는 정확히 1회, 모든 음성 negative fixture는 0회다.
  QA scenarios: happy — 0/2500/5000ms 탭으로 1회 후보; failure — 0/3000/7000ms, checkpoint 변경, 입력 중, 거절 뒤 30초 cooldown 각각 0회. Evidence .omo/evidence/gilnun-android-mvp/task-6/.
  Commit: Y | feat(guidance): detect confirmed interaction friction

- [ ] 7. 미리 탑재한 안정 키 패치와 실패 안전 강조를 TDD로 구현하기 [C3, C5] [4~8h]
  What to do: guidance/PatchEngine.kt, PatchEngineTest.kt와 app.js의 resolve/highlight 명령을 만든다. PatchV1은 pageId, compatibleRevision, stableKey, role, accessibleName, expectedState만 허용한다. native가 patch를 검증하고 DOM은 유일 대상 재검증 후 gilnun-highlight CSS class를 붙이고 scrollIntoView 한다. 8시간 fallback용 review-next 내장 패치 하나를 합성 상수로 둔다.
  Must NOT do: selector/좌표/HTML/JS/URL/wildcard/action payload, fuzzy matching, native 좌표 overlay, 검증 실패 후 대체 대상.
  Parallelization: Wave 1 | Blocked by: 3,5 | Blocks: 8,10~12 | Can parallelize with: 6.
  References: SYNTHESIS.md의 패치 필드와 실패 안전; app/src/main/assets/welfare/*.
  Acceptance criteria: ./gradlew testDebugUnitTest --tests com.gilnun.app.guidance.PatchEngineTest GREEN. A/B 유일 대상은 RESOLVED, pageId·revision·stableKey 없음/중복·role·accessibleName·expectedState 불일치 7종은 모두 PATCH_UNAVAILABLE이다.
  QA scenarios: happy — B의 이동된 review-next만 강조; failure — 7개 불일치 fixture 각각 강조 0개, 실행 0회, VERIFIED 영수증 0건이며 화면이 달라 안내할 수 없음을 표시. Evidence .omo/evidence/gilnun-android-mvp/task-7/.
  Commit: Y | feat(patch): resolve stable targets fail closed

- [ ] 8. 8시간 fallback 폐루프를 통합하기 [C1, C2, C3, C4, C6] [4~8h]
  What to do: GilnunViewModel.kt와 ui/GilnunApp.kt에 감지 후보 확인 시트를 연결한다. 거절은 cooldown, 수락+내장 패치는 설명/DOM 강조, 수락+패치 없음은 도우미 모드 안내로 간다. Models.kt에 ActionReceipt의 guidanceShown, userActionObserved, postconditionVerified, outcome을 추가하고 review-ready일 때만 VERIFIED로 보인다. DemoFlowTest.kt로 전체 흐름을 고정한다.
  Must NOT do: 자동 클릭, 완료 상태 추측, 막힘 확정 문구, 도우미 패치 생성, 영수증에 입력값 포함.
  Parallelization: Wave 1 integration | Blocked by: 4,6,7 | Blocks: 12,18.
  References: SYNTHESIS.md 사용자 흐름 1~9와 행동 영수증; guidance/*.
  Acceptance criteria: layout B에서 save-draft 3회 → 도움 확인 1회 → 수락 → review-next 강조 → 사용자 탭 → review-ready → VERIFIED 영수증이 재현된다. 이어 powershell -ExecutionPolicy Bypass -File scripts/build-milestone.ps1 -Gate fallback8이 artifacts/gilnun-fallback8.apk를 서명·검증하고 SHA-256을 남기며, 승인된 기기가 있으면 설치/실행 스모크까지 통과한다.
  QA scenarios: happy — DemoFlowTest와 fallback8 milestone script; failure — 탭 이벤트만 보내고 checkpoint를 막으면 receipt=UNVERIFIED, signing env/서명 검증 실패는 artifact 복사를 금지. Evidence .omo/evidence/gilnun-android-mvp/task-8/.
  Commit: Y | feat(mvp): complete eight-hour fallback loop

- [ ] 9. 도우미의 한 지점 해결을 PatchV1으로 캡처하기 [C3] [8~14h]
  What to do: GilnunViewModel.kt와 ui/GilnunApp.kt에 도우미 capture mode를 추가한다. 도우미가 patchable target을 누르고 expected checkpoint 전이가 검증됐을 때만 후보 패치를 만든다. 저장 전 preview에 page/revision/key/role/name/expectedState와 안내 범위를 보여주고 도우미가 확인한다.
  Must NOT do: 전체 경로 녹화, 여러 단계 패치, 실패/오클릭 저장, 임의 자연어 명령, 원시 사용자 이력 노출.
  Parallelization: Wave 2 | Blocked by: 7 | Blocks: 10~12,15.
  References: SYNTHESIS.md 사용자 흐름 4~7; data/Models.kt; guidance/PatchEngine.kt.
  Acceptance criteria: A에서 도우미가 review-next로 review-ready 전이를 만들면 PatchV1 한 개가 preview되고, save-draft나 전이 실패는 패치를 만들지 않는다.
  QA scenarios: happy — helper capture success fixture; failure — non-patchable, duplicate key, checkpoint 불변, 역할 불일치 4종이 모두 저장 0건. Evidence .omo/evidence/gilnun-android-mvp/task-9/.
  Commit: Y | feat(helper): capture one verified repair point

- [ ] 10. 최소 로컬 상태 저장과 스키마 검증 구현하기 [C3, C4, C5] [8~14h]
  What to do: data/DemoStateStore.kt와 DemoStateStoreTest.kt를 만든다. SharedPreferences 한 파일에 PatchV1, help state, last receipt만 org.json으로 저장한다. schemaVersion과 필수 필드를 읽을 때 재검증하고 손상·구버전은 격리 후 빈 상태로 실패 안전 복구한다. 원시 이벤트는 ViewModel 메모리에만 둔다.
  Must NOT do: Room, 암호화 프레임워크, 사용자 ID, 시도 전체 로그, 입력값, 이벤트 trace, 자동 마이그레이션 계층.
  Parallelization: Wave 2 | Blocked by: 5,9 | Blocks: 12,15,17 | Can parallelize with: 11.
  References: draft의 persistence 기본값; Android SharedPreferences 공식 문서; app/src/main/java/com/gilnun/app/data/Models.kt.
  Acceptance criteria: ./gradlew testDebugUnitTest --tests com.gilnun.app.data.DemoStateStoreTest GREEN. 재시작 후 유효 패치만 복원되고 malformed/unknown schema는 crash 없이 EMPTY_STATE다.
  QA scenarios: happy — round-trip equality; failure — 잘린 JSON, schemaVersion=999, 누락 stableKey, 초과 문자열이 모두 무시됨. Evidence .omo/evidence/gilnun-android-mvp/task-10/.
  Commit: Y | feat(state): persist only minimal demo state

- [ ] 11. 패치 활성화 계약과 불일치 사용자 경로 만들기 [C3, C5] [8~14h]
  What to do: ui/GilnunApp.kt에 패치 미리보기/활성화 확인을 추가하고 PatchEngine이 매 적용 직전 page/revision/key/role/name을 다시 검증하도록 한다. 계약 문구는 신청 내용 확인 버튼을 강조하며 제출·결제·입력은 없다고 명시한다. 불일치는 자동 실행 없이 새 도움 요청으로 전환한다.
  Must NOT do: 항상 허용, 조용한 패치 활성화, 불일치 숨김, 위험 동작 allowlist.
  Parallelization: Wave 2 | Blocked by: 7,9 | Blocks: 12,15,17 | Can parallelize with: 10.
  References: SYNTHESIS.md 안전 경계와 행동 계약; guidance/PatchEngine.kt.
  Acceptance criteria: 활성화 전 범위가 화면/접근성 읽기로 확인 가능하고, 적용 직전 DOM이 바뀌면 아무 요소도 강조하지 않은 채 PATCH_UNAVAILABLE가 된다.
  QA scenarios: happy — preview 확인 후 정확한 대상 강조; failure — 확인 시트가 열린 동안 revision 변경 시 취소 및 실패 영수증. Evidence .omo/evidence/gilnun-android-mvp/task-11/.
  Commit: Y | feat(safety): require scoped patch activation

- [ ] 12. A 생성→B 재사용과 좌표 매크로 비교를 E2E로 증명하기 [C3, C6] [8~14h]
  What to do: DemoFlowTest.kt에 초기화 → A에서 막힘 → 도우미 capture/save → B 전환 → 막힘 → 저장 패치 resolve/highlight → 사용자 탭 → review-ready를 넣는다. assets에 좌표 기록 방식 비교 마커를 추가해 A 좌표가 B에서 틀리는 장면을 명시적으로 비교 데모라고 표시한다. 2 layout × 3 text scale × 2 viewport fixture 12개를 순회한다.
  Must NOT do: 비교용 좌표를 실제 패치에 저장하거나, 통제 WebView 결과를 임의 앱 지원 증거로 표현.
  Parallelization: Wave 2 integration | Blocked by: 8,10,11 | Blocks: 13~18.
  References: SYNTHESIS.md 대회 시연; app/src/androidTest/java/com/gilnun/app/DemoFlowTest.kt.
  Acceptance criteria: 12/12 fixture에서 stable patch는 올바른 review-next만 강조하고 좌표 마커는 layout B mismatch를 보인다. pageId/revision/key missing/key duplicate/role/name/expectedState의 invalid fixture 7/7은 실패 안전이다. build-milestone.ps1 -Gate mvp14가 artifacts/gilnun-mvp14.apk를 서명·검증·해시하고 가능하면 설치 스모크한다.
  QA scenarios: happy — DemoFlowTest와 mvp14 milestone script; failure — 7종 parameter set 모두 강조 0개, 실행 0회, VERIFIED 0건. Evidence .omo/evidence/gilnun-android-mvp/task-12/result.xml과 macro-comparison.mp4.
  Commit: Y | test(flow): prove semantic replay across variants

- [ ] 13. 가역 도움 단계 정책을 TDD로 구현하기 [C4] [14~18h]
  What to do: guidance/HelpPolicy.kt와 HelpPolicyTest.kt를 만든다. LEVEL_3 상세설명+강조, LEVEL_2 강조, LEVEL_1 한 줄 힌트, LEVEL_0 관찰로 정의한다. 검증된 사용자 수행 성공은 최대 한 단계만 낮추고, 도움 요청·새 마찰 후보·UNVERIFIED/FAILED는 즉시 한 단계 올린다. 이 규칙은 데모용 상태 기계이며 학습/숙련 판정이 아님을 코드와 UI에 명시한다.
  Must NOT do: 2단계 이상 점프, 나이 기반 초기값, 영구 하향, 성공 이벤트만으로 장기 자립 주장.
  Parallelization: Wave 3 | Blocked by: 6,12 | Blocks: 15,18 | Can parallelize with: 14.
  References: SYNTHESIS.md 도움 축소 결정; draft C4.
  Acceptance criteria: ./gradlew testDebugUnitTest --tests com.gilnun.app.guidance.HelpPolicyTest GREEN. 모든 sequence에서 0..3이고 성공은 -1 이하, 실패/요청은 +1 이상이며 즉시 복구된다.
  QA scenarios: happy — VERIFIED 뒤 3→2; failure — 같은 다음 시도에 HELP_REQUEST를 보내 2→3, assisted/UNVERIFIED는 감소 없음. Evidence .omo/evidence/gilnun-android-mvp/task-13/.
  Commit: Y | feat(guidance): add reversible assistance levels

- [ ] 14. 사전 행동 계약과 진실한 사후 영수증 완성하기 [C4, C5] [14~18h]
  What to do: Models.kt와 ui/GilnunApp.kt에 ActionContract/Receipt 표시를 완성한다. 전에는 대상·할 일·하지 않을 일·사용자 직접 탭을, 후에는 guidanceShown/userActionObserved/postconditionVerified/outcome을 한국어로 보여준다. last receipt만 저장하며 timestamp는 로컬 표시용이고 사용자 식별자는 없다.
  Must NOT do: click observed를 complete로 승격, 숨은 행동, 입력값/DOM 텍스트/전화번호 기록, 성공을 과장하는 축하 문구.
  Parallelization: Wave 3 | Blocked by: 8,11,12 | Blocks: 15,17,18 | Can parallelize with: 13.
  References: SYNTHESIS.md 행동 영수증; data/Models.kt.
  Acceptance criteria: ReceiptEvaluator 단위 테스트에서 checkpoint=review-ready만 VERIFIED이고 click-only, timeout, revision-change는 UNVERIFIED/FAILED다. 화면 독자는 계약과 결과를 읽을 수 있다.
  QA scenarios: happy — 정상 전이 영수증 캡처; failure — JS가 click만 보내고 postcondition을 누락하면 완료 단어가 0회. Evidence .omo/evidence/gilnun-android-mvp/task-14/.
  Commit: Y | feat(receipt): report only verified outcomes

- [ ] 15. 도움 정책·저장·영수증·Demo Reset을 통합하기 [C4, C5, C6] [14~18h]
  What to do: GilnunViewModel.kt에서 detector, patch engine, help policy, store를 구체 객체로 직접 조합한다. 상태 흐름을 단일 reducer 수준으로 유지하고 앱 재시작 후 패치/도움 단계/last receipt만 복원한다. Demo Reset은 확인 한 번 뒤 prefs, WebView state, in-memory events, highlight를 지우고 layout A 학습자로 5초 내 돌아간다.
  Must NOT do: DI container, repository interface, 백그라운드 worker, 숨은 reset, 원시 event 복원.
  Parallelization: Wave 3 integration | Blocked by: 13,14 | Blocks: 17,18.
  References: app/src/main/java/com/gilnun/app/GilnunViewModel.kt; data/DemoStateStore.kt.
  Acceptance criteria: 재시작 복원 테스트와 Reset 테스트가 통과하고, adb shell am force-stop 후에도 최소 상태만 복원되며 Reset 후 shared_prefs에 길눈 상태 0건이다.
  QA scenarios: happy — patch 저장→force-stop→복원; failure — reset 직전 malformed state가 있어도 5초 내 깨끗한 초기 화면. Evidence .omo/evidence/gilnun-android-mvp/task-15/.
  Commit: Y | feat(demo): integrate state and fast reset

- [ ] 16. 고령 사용자 접근성·반응형 표시 기준 통과하기 [C1, C6] [18~24h]
  What to do: ui/GilnunApp.kt, ui/GilnunTheme.kt, assets/welfare/style.css를 점검해 핵심 터치 영역 48dp/px 이상, 텍스트 200%에서 재배치, 충분한 대비, focus order, contentDescription/label, 색만으로 전달하지 않는 상태, 동작 감소를 적용한다. DOM highlight는 scroll/resize 뒤 대상에 붙고 pointer-events:none이다.
  Must NOT do: 고정 높이로 텍스트 자르기, 작은 아이콘 단독 버튼, 자동 사라지는 핵심 문구, 음성만 있는 안내.
  Parallelization: Wave 4 | Blocked by: 12,15 | Blocks: 18 | Can parallelize with: 17.
  References: https://www.w3.org/TR/WCAG22/; https://developer.android.com/guide/topics/ui/accessibility/apps.
  Acceptance criteria: 100/150/200% 및 normal/narrow 총 6조합에서 review-next, 도움, 거절, 역할, Reset이 잘리지 않고 TalkBack 이름이 있으며 잘못된 강조 0건이다.
  QA scenarios: happy — physical device font scale 1.0/1.5/2.0 캡처; failure — 접근성 scanner/TalkBack에서 unlabeled critical control 0건, clipping 발견 시 테스트 실패로 기록 후 수정. Evidence .omo/evidence/gilnun-android-mvp/task-16/.
  Commit: Y | fix(a11y): support large text and touch targets

- [ ] 17. 개인정보·브리지·오프라인 경계를 출고 수준으로 감사하기 [C5, C6] [18~24h]
  What to do: AndroidManifest.xml에 allowBackup=false를 고정하고 DemoWebView.kt에서 외부 navigation/network/file/content access, WebStorage/DOM storage/database/cache/form save/autofill, first/third-party cookie와 release debugging을 차단한다. 첫 load 전 cookie 삭제 callback을 기다리고 Demo Reset 시 clearFormData, clearCache(true), clearHistory, WebStorage.deleteAllData, removeAllCookies callback+flush를 완료한다. SecurityBoundaryTest.kt와 scripts/verify-boundaries.ps1를 만든다. debuggable test APK에 합성 canary를 입력·force-stop·재부팅·Reset한 뒤에만 adb exec-out run-as로 shared_prefs, files, cache, databases, app_webview 전체의 UTF-8/UTF-16 canary 0건과 CookieManager 값 0건을 검사한다. non-debuggable release APK에는 run-as를 시도하지 않고 manifest/debuggable=false, 권한, APK 문자열/리소스, bridge allowlist, 설치·오프라인 스모크를 별도 검사한다.
  Must NOT do: 저장 안 하니 법 적용 없음이라는 주장, 디버그 로그에 payload 출력, 인터넷 권한, crash analytics, 실제 개인정보 테스트.
  Parallelization: Wave 4 | Blocked by: 10,11,14,15 | Blocks: 18 | Can parallelize with: 16.
  References: SYNTHESIS.md 안전 경계; https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges; 개인정보 보호법 제15·16조 링크는 SYNTHESIS 참조.
  Acceptance criteria: verify-boundaries.ps1가 debug APK full-sandbox canary 0건과 release APK debuggable=false/static boundary/install smoke를 각각 올바른 방식으로 통과한다. force-stop·재부팅 뒤 form/cookie/localStorage/cache 복원은 0건이다.
  QA scenarios: happy — debug canary scan 뒤 non-debuggable release 비행기 모드 3회 완주; failure — first/third-party cookie, 외부 URL, invalid origin, WebStorage/form canary, debuggable release fixture가 각각 nonzero exit이고, release에 run-as를 호출하는 테스트 자체도 계획 위반으로 실패. Evidence .omo/evidence/gilnun-android-mvp/task-17/.
  Commit: Y | test(security): enforce offline privacy boundary

- [ ] 18. 서명 APK·10회 리허설·발표 백업을 출고하기 [C6] [18~24h]
  What to do: scripts/build-release.ps1, docs/demo-runbook.md, docs/pitch-script.md, docs/qa-checklist.md를 만든다. 키는 사용자 홈의 .gilnun에 두고 환경변수로만 읽어 release APK를 서명·검증한 뒤 artifacts/gilnun-mvp.apk로 복사한다. 4분 대본은 좌표 비교→마찰 후보→도우미 패치→B 재사용→영수증→도움 변화 순서이며 통제 WebView/규칙 기반 시제품 한계를 명시한다. 동일 APK의 50초 무편집 백업 영상을 만든다.
  Must NOT do: 키/비밀번호 커밋, 디버그 APK 제출, 임의 앱·장기 학습·최초 주장, 실패 장면 편집으로 은폐.
  Parallelization: Wave 4 release gate | Blocked by: 16,17 | Blocks: 19~22.
  References: SYNTHESIS.md 발표·주장 제한; docs/*; app/build/outputs/apk/release/*.
  Acceptance criteria: powershell -ExecutionPolicy Bypass -File scripts/build-milestone.ps1 -Gate release24가 Gradle test/lint/assemble, connected test(기기 READY일 때), verify-boundaries, apksigner verify, SHA-256, artifacts/gilnun-mvp.apk 복사를 각 LASTEXITCODE로 통제한다. 콜드런 10/10, 비행기 모드 3/3, Reset 5초 이내, 50초 백업 영상 재생 성공이다.
  QA scenarios: happy — 새 설치부터 전체 4분 흐름 10회; failure — 1회라도 crash/잘못된 대상/거짓 영수증이면 카운트를 0으로 리셋하고 수정 후 다시 10회. Evidence .omo/evidence/gilnun-android-mvp/task-18/release-gate.md, sha256.txt, demo-backup.mp4.
  Commit: Y | build(release): package competition-ready apk

- [ ] 19. 스트레치: 두 번째 번들 과업으로 일반성 점검하기 [C1, C3] [MVP 통과 후]
  What to do: 같은 계약을 쓰는 합성 보건소 프로그램 일정 확인 과업을 assets/welfare에 추가하고 기존 PatchEngine 수정 없이 PatchV1 데이터만 늘린다.
  Must NOT do: 엔진 분기, 외부 사이트, 실제 기관 데이터, MVP 회귀 실패 상태에서 시작.
  Parallelization: Stretch | Blocked by: 18과 MVP F1~F4 승인 | Can parallelize with: 20,21.
  Acceptance criteria: 두 과업 A/B 총 24 fixture가 기존 엔진으로 통과하고 새 과업 전용 if/when 분기 0개다.
  QA scenarios: happy — 전체 parameterized flow; failure — incompatible revision fail closed. Evidence .omo/evidence/gilnun-android-mvp/task-19/.
  Commit: Y | feat(stretch): add second semantic demo task

- [ ] 20. 스트레치: 개인정보 없는 로컬 막힘 지도 만들기 [C2, C6] [MVP 통과 후]
  What to do: stableKey별 후보 횟수와 verified recovery 수만 정수로 집계해 앱 안의 간단한 목록/막대에 표시한다. 원시 이벤트·사용자 ID·시간선은 저장하지 않고 Reset으로 지운다.
  Must NOT do: 대시보드 라이브러리, 서버 전송, 개인별 점수, 작은 표본의 퍼센트를 실제 고령층 통계로 발표.
  Parallelization: Stretch | Blocked by: 18과 MVP F1~F4 승인 | Can parallelize with: 19,21.
  Acceptance criteria: 합성 5회 입력과 집계가 일치하고 저장소에는 stableKey와 정수 외 행동 정보가 없다.
  QA scenarios: happy — known counts; failure — unknown key와 malformed count 무시. Evidence .omo/evidence/gilnun-android-mvp/task-20/.
  Commit: Y | feat(stretch): show local aggregate friction map

- [ ] 21. 스트레치: 오프라인 TTS와 동일 자막 추가하기 [C4] [MVP 통과 후]
  What to do: Android TextToSpeech가 이미 설치된 한국어 엔진에서만 안내 문구를 읽고 화면에 동일 자막을 항상 유지한다. 엔진 없음/언어 없음이면 조용히 자막만 남긴다.
  Must NOT do: 네트워크 음성, 음성 입력, 음성만 제공, TTS 엔진 다운로드 유도.
  Parallelization: Stretch | Blocked by: 18과 MVP F1~F4 승인 | Can parallelize with: 19,20.
  Acceptance criteria: 비행기 모드에서 한국어 엔진 있음/없음 두 경로 모두 과업이 완료되고 화면 문구가 동일하다.
  QA scenarios: happy — offline speech+caption; failure — LANG_MISSING에서 crash/무한 재시도 0건. Evidence .omo/evidence/gilnun-android-mvp/task-21/.
  Commit: Y | feat(stretch): narrate guidance offline

- [ ] 22. 스트레치: 안전한 비최종 동작 한 번만 확인 실행하기 [C3, C4, C5] [모든 게이트 후]
  What to do: review-next 한 동작만 compile-time allowlist에 넣고 계약 확인 직후 page/revision/key/role/name을 다시 해석해 1회 실행한다. expectedState가 없으면 FAILED receipt를 남기고 다시 실행하지 않는다. 사용자는 안내 전용 모드로 되돌릴 수 있다.
  Must NOT do: 최종 제출·동의·입력·결제·삭제, 일반 action field, 여러 단계, 백그라운드 실행, 좌표 fallback.
  Parallelization: Stretch safety gate | Blocked by: 18, MVP F1~F4 승인, 7/11/14/17의 모든 실패 테스트 | Can parallelize with: 없음.
  Acceptance criteria: 정상 allowlist 20/20, stale revision/key 20/20 취소, 모든 금지 target 100% 거부. 실패 1건이면 라이브 데모와 release APK에서 기능을 제거한다.
  QA scenarios: happy — 확인 후 review-next 1회와 VERIFIED; failure — 확인 시트 중 layout/revision 변경, duplicate key, submit-like role에서 실행 0회. Evidence .omo/evidence/gilnun-android-mvp/task-22/.
  Commit: Y | feat(stretch): confirm one safe nonfinal action

## Final verification wave
> Todo 1~18 직후 MVP에 대해 병렬 실행하고 ALL APPROVE여야 한다. 그 뒤에만 Todo 19~22 중 선택한 stretch를 실행하며, 실행한 stretch가 있으면 F1~F4의 관련 항목을 다시 실행한다. 결과를 공개하고 사용자의 명시적 확인 전에는 완료를 선언하지 않는다.
- [ ] F1. Plan compliance audit
  Verify: C1~C6과 Todo 1~18의 산출물이 존재하고 각 acceptance command의 최신 실행이 PASS인지 대조한다. 미실행·가짜 증거·out-of-date 결과는 승인하지 않는다.
  Command: powershell -ExecutionPolicy Bypass -File scripts/final-mvp-gate.ps1. 이 스크립트가 각 Gradle·경계·서명 명령 뒤 LASTEXITCODE를 검사하고 첫 실패에서 중단한다.
  Evidence: .omo/evidence/gilnun-android-mvp/final/f1-plan-compliance.md.
  Verdict: APPROVE 또는 REJECT와 정확한 todo 번호.

- [ ] F2. Code quality and security review
  Verify: 한 :app, 구체 클래스 직접 조합, SharedPreferences+org.json, exact-origin WebMessage, fail-closed PatchEngine, no INTERNET/AccessibilityService/addJavascriptInterface/coordinate fallback/PII logging을 코드와 APK 양쪽에서 확인한다.
  Command: powershell -ExecutionPolicy Bypass -File scripts/verify-boundaries.ps1. 스크립트는 lookaround 없는 금지어 검색과 별도 URL allowlist 비교를 사용하고, https://appassets.androidplatform.net 외 구현 URL을 거부하며 apkanalyzer manifest permissions도 검사한다.
  Evidence: .omo/evidence/gilnun-android-mvp/final/f2-quality-security.md.
  Verdict: APPROVE 또는 REJECT와 file:line.

- [ ] F3. Real manual QA
  Verify: 실제 Android 기기에서 새 설치, 비행기 모드, A 도우미 패치 생성, B 재사용, 영수증, 도움 변화, 재시작, Reset을 수행한다. font scale 100/150/200%, 좁은 viewport, TalkBack 이름, invalid patch 경로를 포함한다.
  Procedure: adb install -r artifacts/gilnun-mvp.apk; 화면 녹화와 logcat을 시작한 뒤 docs/qa-checklist.md를 수행한다. 콜드런 10/10과 offline reboot 3/3이 아니면 REJECT.
  Evidence: .omo/evidence/gilnun-android-mvp/final/f3-device-qa.md, f3-device.mp4, f3-logcat.txt.
  Verdict: APPROVE 또는 REJECT와 재현 순서.

- [ ] F4. Scope fidelity and pitch truthfulness
  Verify: 앱·대본·슬라이드가 통제된 WebView, 앱 작성 stable key, 규칙 기반 마찰 후보, 안내 전용 MVP라는 실제 범위를 말한다. 임의 앱·AI 화면 이해·장기 학습·최초·전국 효과 주장은 0건이다. 좌표 비교는 비교용 시뮬레이션임을 밝힌다.
  Credibility gate: 시간이 허용되면 60~90초 storyboard를 대회 관점 리뷰어 8명에게 보여준다. 6/8 이상이 참여 공공서비스 안의 재사용 안내 가치를 정확히 설명하고, 신뢰도·관련성 중앙값 4/5 이상, hardcoded라 평가한 사람이 1명 이하여야 현 포지셔닝을 유지한다. 실패 시 앱은 바꾸지 않고 공공서비스 운영자가 심는 온디바이스 안내·검증 계층으로 포지셔닝을 좁힌다. 리뷰어를 구하지 못하면 사용자 검증을 했다고 주장하지 않는다.
  Evidence: .omo/evidence/gilnun-android-mvp/final/f4-claim-audit.md와 선택적 f4-credibility.csv.
  Verdict: APPROVE 또는 REJECT와 문제 문구.

## Commit strategy
- 작업 브랜치 기본값: codex/gilnun-android-mvp.
- Todo 하나당 해당 구현+테스트를 한 원자적 conventional commit으로 만든다. 빌드가 깨지는 중간 커밋은 남기지 않는다.
- Wave 종료마다 ./gradlew testDebugUnitTest lintDebug assembleDebug를 실행하고 증거를 남긴 뒤 다음 Wave로 간다.
- artifacts/*.apk, *.jks, local.properties, .gradle/, build/, .omo/evidence의 대용량 영상·로그는 커밋하지 않는다. 재현 가능한 docs와 scripts만 커밋한다.
- 스트레치는 MVP release commit 이후 별도 커밋으로 두어 한 번에 revert할 수 있게 한다. Todo 22가 한 번이라도 안전 게이트를 실패하면 커밋하지 않고 기능을 제거한다.
- 사용자가 명시적으로 요청하기 전에는 push, PR, Play 업로드를 하지 않는다.

## Success criteria
1. C1 — 설치된 APK가 비행기 모드에서 합성 과업 A/B를 수동 완주하고 외부 리소스를 요청하지 않는다.
2. C2 — save-draft 3회/6초와 직접 도움 요청만 후보를 만들며 negative fixture는 0회, 거절 cooldown은 진행을 막지 않는다.
3. C3 — A에서 도우미가 만든 한 단계 PatchV1이 12/12 변형에서 B의 review-next만 강조하고 pageId/revision/key missing/key duplicate/role/name/expectedState 7종 불일치는 모두 강조·실행·VERIFIED 0건으로 실패 안전이다.
4. C4 — 도움은 3→2→1→0에서 한 단계씩만 변하고 실패/요청 시 즉시 복구된다. 영수증은 shown/observed/verified를 구분한다.
5. C5 — APK에 INTERNET·접근성·오버레이 권한이 없고, 브리지는 exact appassets origin만 받는다. debug APK 전체 sandbox·logcat canary는 0건이고, release는 debuggable=false·정적 경계·설치 스모크를 통과하며 first/third-party cookie와 WebStorage/form/autofill/cache 복원은 0건이다.
6. C6 — signed artifacts/gilnun-mvp.apk의 서명과 SHA-256이 검증되고 실제 기기 cold run 10/10, offline reboot 3/3, Reset 5초 이내다.
7. 접근성 — 100/150/200% 글자와 좁은 화면에서 핵심 제어가 잘리지 않고 48dp, TalkBack 이름, 대비, DOM highlight 정렬을 통과한다.
8. 발표 — 4분 안에 좌표 방식 실패→후보 감지→도우미 해결→B 재사용→검증 영수증→가역 도움을 보여주고 50초 백업 영상이 있다.
9. 주장 — 브랜드는 길눈 AI를 쓰되 온디바이스 규칙 기반 연구 시제품이라 밝히고 임의 앱 지원·장기 학습·세계 최초를 주장하지 않는다.
10. 시간 부족 — 8시간이면 Todo 1~8의 preloaded-patch fallback APK를 출고하고, 14시간이면 Todo 12까지, 24시간이면 Todo 18까지 완료한다. 미완성 고급 기능보다 통과한 이전 게이트를 제출한다.
11. 스트레치 — Todo 18 뒤 MVP F1~F4를 먼저 승인받는다. 그 후 Todo 19~22는 시간이 남을 때만 시작하고, 실행한 stretch 뒤 관련 F1~F4를 재실행하며 MVP 회귀가 생기면 즉시 제거한다.
12. 최종 완료 — F1~F4가 모두 APPROVE이고 사용자가 결과를 확인한 뒤에만 구현 목표를 완료로 선언한다.
