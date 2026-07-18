# 길눈 AI 최종 QA 체크리스트

## 자동 게이트

- [ ] 웹 production build PASS
- [ ] `testDebugUnitTest` PASS
- [ ] `lintRelease` PASS
- [ ] debug/release APK build PASS
- [ ] release signature와 SHA-256 확인
- [ ] INTERNET·접근성·오버레이·미디어 캡처 권한 0건
- [ ] `addJavascriptInterface` 0건, exact appassets origin만 허용
- [ ] release `debuggable=false`

## 실기기

- [ ] 새 설치 뒤 비행기 모드에서 A/B 각각 수동 완주
- [ ] `save-draft` 0/2500/5000ms 세 번에 도움 제안 정확히 1회
- [ ] 0/3000/7000ms, 입력·로딩·checkpoint 변경에는 제안 0회
- [ ] 거절 뒤 30초 cooldown 동안 재제안 0회
- [ ] A에서 만든 PatchV1이 B의 `review-next`만 강조
- [ ] missing/duplicate/page/revision/role/name/expectedState 불일치 7종 모두 fail closed
- [ ] 클릭만 관찰되고 postcondition이 없으면 완료 문구 0회
- [ ] VERIFIED 뒤 도움 3→2, 직접 요청 뒤 2→3
- [ ] 앱 재시작 뒤 패치·도움 단계·마지막 영수증만 복원
- [ ] Demo Reset 5초 이내, 원시 이벤트·폼·쿠키·WebStorage·캐시 복원 0건

## 접근성·무대

- [ ] 글자 100/150/200% × normal/narrow 6조합에서 잘림 없음
- [ ] 핵심 터치 대상 48dp/48px 이상
- [ ] TalkBack 이름과 포커스 순서 확인
- [ ] cold run 10/10
- [ ] offline reboot 3/3
- [ ] 50초 무편집 백업 영상 재생 확인
- [ ] 임의 앱·AI 화면 이해·장기 학습·세계 최초 주장 0건
