# 길눈 AI Android·Web MVP 설계

## 승인된 기준

이 문서는 사용자가 “이게 최종본”으로 지정한 `gilnun-android-mvp` 작업 계획을 구현 가능한 제품 설계로 고정한다. 기능·안전·주장 범위가 충돌할 때는 최종 계획의 C1~C6과 Todo 1~18을 우선한다. Todo 19~22는 MVP 최종 게이트 이후의 스트레치이며 이번 기본 구현 범위에 포함하지 않는다.

## 제품 목표

길눈 AI는 통제된 합성 복지 신청 화면에서 다음 폐루프를 오프라인으로 시연한다.

`반복 비진행 탭 감지 → 도움 여부 확인 → 도우미의 한 지점 해결 → 안정 키 패치 저장 → 변경된 레이아웃에서 재사용 → 도움 강도 조절 → 사후조건 기반 영수증`

사용자는 항상 직접 행동한다. 앱은 결제·동의·최종 제출을 자동 실행하지 않고, 다른 앱이나 실제 사이트를 읽거나 조작하지 않는다.

## 산출물

- Android: `com.gilnun.app`, 단일 `:app` 모듈, minSdk 26의 서명 APK.
- Web: Android와 같은 합성 과업과 상태 계약을 구현한 반응형 웹 데모.
- 공통 문서: 상태 계약, 4분 시연 대본, QA 체크리스트, 범위·주장 제한.
- 검증: 단위 테스트, 웹 빌드, Android lint/build, APK 정적 경계, 가능한 실기기 검사.

## 구조

### Android

- Kotlin + Jetpack Compose 한 Activity.
- `WebViewAssetLoader`가 APK에 번들된 `assets/welfare/`만 로드한다.
- `WebViewCompat.addWebMessageListener`는 정확한 appassets origin의 최소 의미 이벤트만 받는다.
- `GilnunViewModel`이 감지기, 패치 엔진, 도움 정책, 영수증 판정기, 최소 저장소를 구체 클래스로 직접 조합한다.
- `SharedPreferences + org.json`에는 패치, 도움 단계, 마지막 최소 영수증만 저장한다.

### Web

- 별도 `web/` 프로젝트로 배포 가능한 데모를 제공한다.
- Android와 동일한 `pageId`, `compatibleRevision`, `stableKey`, `role`, `accessibleName`, `checkpoint`, `expectedState` 계약을 사용한다.
- 브라우저 저장소 없이 메모리 상태로 동작하며 새로고침은 Demo Reset과 같은 안전한 초기화로 취급한다.
- 웹은 제품을 설명하는 랜딩 페이지가 아니라 심사자가 즉시 조작 가능한 제품 데모다.

### 상태 계약

- `pageId`: `welfare-basic-class`
- `compatibleRevision`: `2026-07`
- 비진행 대상: `save-draft`
- 패치 대상: `review-next`, `role=button`, `accessibleName=신청 내용 확인`
- 성공 사후조건: `checkpoint=review-ready`
- 레이아웃 A/B는 위치·순서·글자 크기만 다르고 의미 계약은 같다.

## 상호작용

1. 학습자 모드에서 `임시 저장`을 6초 안에 세 번 누르면 도움 확인 시트가 한 번 열린다.
2. 거절하면 30초 동안 다시 묻지 않는다.
3. 패치가 없으면 도우미 모드에서 `신청 내용 확인` 한 지점을 선택해 PatchV1을 만든다.
4. 레이아웃 B에서 여섯 의미 필드가 모두 일치하는 유일한 대상을 강조한다.
5. 0개·중복·불일치이면 강조와 실행을 모두 중단하고 “안내를 안전하게 불러오지 못했습니다”를 표시한다.
6. 사용자가 직접 대상 버튼을 누르고 `review-ready`가 관찰될 때만 VERIFIED 영수증을 만든다.
7. VERIFIED는 도움 강도를 한 단계 내리고, 직접 요청·새 마찰·UNVERIFIED·FAILED는 즉시 한 단계 올린다.
8. Demo Reset은 패치·도움·영수증·WebView의 비영구 상태를 지우고 레이아웃 A 학습자로 돌아간다.

## 시각 방향

방향은 “차분한 공공 길찾기 도구”다. 병원·관공서의 차갑고 복잡한 대시보드가 아니라, 종이 안내 지도처럼 어디에 있고 다음 한 걸음이 무엇인지 명확해야 한다.

- 바탕: 따뜻한 미색 `#F7F3EA`
- 본문: 짙은 남청 `#172A3A`
- 주요 행동: 청록 `#0B6B5B`
- 도움·주의: 호박색 `#D97706`
- 실패 안전: 적갈색 `#9F3A38`
- 기억 요소: 현재 단계와 도움 패치를 잇는 점선 경로, 안정 키를 나타내는 작은 “길표지” 배지
- 모션: 180~240ms의 짧은 강조와 시트 전환만 사용하고 `prefers-reduced-motion`에서는 제거
- 글자: 오프라인 시스템 한글 서체를 사용하되 1.0~2.0배에서도 계층이 무너지지 않게 크기·행간·여백으로 개성을 만든다.

## 접근성

- 핵심 터치 대상 최소 48dp/48px.
- 색 외에 아이콘·문구·테두리 패턴으로 상태를 중복 전달.
- 200% 글자와 320px 폭에서 가로 스크롤 없이 주요 제어를 노출.
- 모든 역할·레이아웃·도움·거절·Reset 제어에 읽을 수 있는 이름 제공.
- 강조 레이어는 `pointer-events:none`이며 실제 버튼의 포커스와 클릭을 방해하지 않는다.

## 개인정보·보안

- INTERNET, AccessibilityService, 오버레이, 미디어 캡처 권한 없음.
- 실제 개인정보, 입력값, 전체 DOM 텍스트, URL, 좌표, 키 입력을 저장·로그하지 않는다.
- 첫 로드 전 first/third-party cookie를 비활성화하고 기존 쿠키 삭제 callback과 flush를 완료한다.
- 파일·콘텐츠 접근, 혼합 콘텐츠, WebStorage, 데이터베이스, 폼 저장, autofill, 캐시를 차단한다.
- release는 `debuggable=false`; debug APK의 sandbox canary 검사와 release APK 정적 경계 검사를 분리한다.

## 오류 처리

- WebMessage 미지원: 도움 기능만 닫고 합성 과업의 수동 진행은 유지한다.
- 이벤트 스키마 오류: 상태를 바꾸지 않고 최소 오류 코드만 표시한다.
- 패치 불일치: 유사 이름·좌표·selector로 대체하지 않는다.
- 클릭 후 사후조건 없음: UNVERIFIED로 기록하고 완료 표현을 쓰지 않는다.
- 저장 JSON 손상: 해당 최소 상태를 버리고 깨끗한 초기 상태로 복구한다.

## 완료 판단

코드 작성이 끝난 뒤 한 번의 통합 검증 단계에서 웹 빌드, Kotlin 단위 테스트, Android lint/debug/release 빌드, 금지 경계 검사, 서명 검증을 실행한다. Android 기기가 없으면 자동·정적 검증은 완료하되 F3 실기기 게이트를 `DEVICE_BLOCKED`로 정직하게 남긴다.
