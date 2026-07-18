# 길눈 AI 데모 상태 계약

| 필드 | 고정값 |
| --- | --- |
| pageId | `welfare-basic-class` |
| compatibleRevision | `2026-07` |
| 시작 checkpoint | `consent-ready` |
| 비진행 stableKey | `save-draft` |
| 패치 stableKey | `review-next` |
| role | `button` |
| accessibleName | `신청 내용 확인` |
| expectedState | `review-ready` |

PatchV1은 위 의미 필드 여섯 개만 저장한다. 좌표, selector, XPath, HTML, URL, 입력값은 저장하지 않는다. 대상이 없거나 둘 이상이거나 필드 하나라도 다르면 `PATCH_UNAVAILABLE`이며 하이라이트·실행·VERIFIED 영수증은 모두 0건이어야 한다.

사용자가 직접 `review-next`를 누르고 `review-ready`가 관찰된 경우에만 완료로 기록한다. 클릭만 관찰되거나 revision이 바뀌면 UNVERIFIED/FAILED다. 최종 제출·동의·결제 동작은 존재하지 않는다.
