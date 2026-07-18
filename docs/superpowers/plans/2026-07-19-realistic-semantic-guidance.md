# Layout-Resilient Realistic Practice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the obvious three-button practices with three realistic five-checkpoint public-service journeys that automatically rearrange after the first action and still recover the exact next target by semantic meaning.

**Architecture:** Keep `ServiceCatalog`, PatchV1, and strict Bridge V2 as the Android-owned source of truth. Expand the catalog and matching offline JavaScript data to fifteen progress checkpoints, render fixed-choice interactive controls without real submission or personal data, and perform the A-to-B transition inside the already loaded WebView so the current checkpoint survives without broadening the URL or bridge schemas. Compose presents the value proposition, a dynamic five-step count, and a clearer semantic-help action.

**Tech Stack:** Kotlin 2.1, Jetpack Compose Material 3, Android WebViewAssetLoader, offline HTML/CSS/JavaScript, JUnit 4, AndroidX instrumentation, Gradle Android plugin, PowerShell release gates.

---

## File map

- `app/src/main/java/com/gilnun/app/catalog/ServiceCatalog.kt`: fifteen Android-owned checkpoint, event, narration, and PatchV1 contracts.
- `app/src/main/assets/welfare/index.html`: fixed offline shell, layout-update announcement, and safety landmarks.
- `app/src/main/assets/welfare/app.js`: matching service data, fixed-choice controls, checkpoint reducer, bridge messages, and in-page A-to-B transition.
- `app/src/main/assets/welfare/style.css`: three distinct institution-shaped presentations, real control states, responsive Layout A/B placement, and reduced motion.
- `app/src/main/java/com/gilnun/app/ui/GilnunApp.kt`: home value proposition, dynamic progress count, help copy, and release UI.
- `app/src/test/java/com/gilnun/app/catalog/ServiceCatalogTest.kt`: route count, exact semantic target, role, and uniqueness contracts.
- `app/src/test/java/com/gilnun/app/web/PracticeAssetContractTest.kt`: offline asset, realistic-control, automatic-layout, safety, and accessibility contracts.
- `app/src/androidTest/java/com/gilnun/app/GuidanceJourneyInstrumentationTest.kt`: five-step reducer journeys and exact guidance receipts.
- `app/src/androidTest/java/com/gilnun/app/ManualJourneyTest.kt`: Compose labels and release-only controls.
- `app/build.gradle.kts`: update-install version `0.2.1 (3)`.
- `scripts/verify-boundaries.ps1`: packaged-asset and version boundary checks.
- `scripts/final-mvp-gate.ps1`: v0.2.1 evidence summary.
- `outputs/gilnun-mvp.apk`, `outputs/gilnun-mvp.apk.sha256`, `outputs/verification-summary.md`: regenerated signed deliverables.

### Task 1: Lock fifteen Android semantic contracts

**Files:**
- Modify: `app/src/test/java/com/gilnun/app/catalog/ServiceCatalogTest.kt`
- Modify: `app/src/main/java/com/gilnun/app/catalog/ServiceCatalog.kt`

- [ ] **Step 1: Write the failing route contract**

Replace the nine expected steps with this exact table and include `role` in `ExpectedStep`:

| Service | Checkpoint | Stable key | Role | Accessible name | Next checkpoint |
|---|---|---|---|---|---|
| basic-pension | pension-service | pension-service-select | button | 기초연금 신청 연습 | pension-applicant |
| basic-pension | pension-applicant | pension-applicant-confirm | button | 연습 사용자 정보 확인 | pension-method |
| basic-pension | pension-method | pension-self-apply | radio | 본인이 신청해요 | pension-contact |
| basic-pension | pension-contact | pension-contact-confirm | button | 연락 방법 확인 | pension-review |
| basic-pension | pension-review | pension-review-confirm | button | 신청 내용 확인 | pension-complete |
| resident-record | resident-type | resident-copy-select | tab | 주민등록표 등본 | resident-address |
| resident-record | resident-address | resident-address-confirm | button | 주소 확인 | resident-issue-type |
| resident-record | resident-issue-type | resident-standard-issue | radio | 발급(모의) | resident-delivery |
| resident-record | resident-delivery | resident-online-delivery | combobox | 온라인발급(본인출력·연습용) | resident-review |
| resident-record | resident-review | resident-finish-practice | button | 민원 신청 연습 마치기 | resident-complete |
| health-screening | health-service | health-service-select | button | 건강검진 대상 조회 연습 | health-person |
| health-screening | health-person | health-person-confirm | button | 연습 사용자 정보 확인 | health-year |
| health-screening | health-year | health-year-2026 | radio | 2026년(가상) | health-kind |
| health-screening | health-kind | health-general-screening | radio | 일반건강검진(모의) | health-query |
| health-screening | health-query | health-screening-query | button | 건강검진 대상 조회 | health-complete |

Use these assertions:

```kotlin
assertEquals(5, service.steps.size)
assertEquals(6, service.route.size)
assertEquals(expected.role, checkpoint.primaryAction?.role)
assertEquals(expected.role, checkpoint.patch?.role)
assertEquals(
    15,
    ServiceCatalog.services
        .flatMap(ServiceContract::steps)
        .mapNotNull(CheckpointContract::patch)
        .size,
)
```

Keep the one fixed friction event per service:

```kotlin
ExpectedFriction(
    ServiceId.BASIC_PENSION,
    "pension-review",
    ServiceEventType.ACTION,
    "pension-save-draft",
    "임시 저장",
)
ExpectedFriction(
    ServiceId.RESIDENT_RECORD,
    "resident-delivery",
    ServiceEventType.HELP,
    "resident-delivery-help",
    "수령 방법 안내",
)
ExpectedFriction(
    ServiceId.HEALTH_SCREENING,
    "health-query",
    ServiceEventType.HELP,
    "health-schedule-help",
    "검진 일정 안내",
)
```

- [ ] **Step 2: Run the focused catalog test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.catalog.ServiceCatalogTest" --no-daemon
```

Expected: FAIL because each service still has three steps and all progress roles are `button`.

- [ ] **Step 3: Expand the catalog with role-aware steps**

Change the helper signature and use the same role for the event and patch:

```kotlin
private fun step(
    pageId: String,
    checkpoint: String,
    narration: String,
    stableKey: String,
    role: String = BUTTON_ROLE,
    accessibleName: String,
    nextCheckpoint: String,
    friction: EventContract? = null,
): CheckpointContract =
    CheckpointContract(
        id = checkpoint,
        narration = narration,
        primaryAction =
            EventContract(
                type = ServiceEventType.ACTION,
                stableKey = stableKey,
                role = role,
                accessibleName = accessibleName,
                effect = EventEffect.PROGRESS,
                expectedCheckpoint = nextCheckpoint,
            ),
        frictionEvent = friction,
        patch =
            PatchV1(
                pageId = pageId,
                compatibleRevision = REVISION,
                stableKey = stableKey,
                role = role,
                accessibleName = accessibleName,
                expectedState = nextCheckpoint,
            ),
    )
```

Populate the catalog from the fifteen-row table. Use these fixed Korean narrations in route order:

```text
기초연금 신청 연습을 찾아 선택해 주세요.
가상 신청자 정보를 확인해 주세요.
본인이 신청하는 연습 경로를 선택해 주세요.
가상 연락 방법을 확인해 주세요.
가상 신청 내용을 마지막으로 확인해 주세요.
주민등록표 등본 탭을 선택해 주세요.
가상 주소를 확인해 주세요.
모의 발급 형태를 선택해 주세요.
연습용 온라인 발급 방법을 선택해 주세요.
민원 신청 연습을 마쳐 주세요.
건강검진 대상 조회 연습을 찾아 선택해 주세요.
가상 사용자 정보를 확인해 주세요.
2026년 가상 조회 기준을 선택해 주세요.
일반건강검진 모의 항목을 선택해 주세요.
가상 건강검진 대상 여부를 조회해 주세요.
```

- [ ] **Step 4: Run the catalog and bridge tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.catalog.ServiceCatalogTest" --tests "com.gilnun.app.web.BridgeEventV2Test" --tests "com.gilnun.app.guidance.ReceiptEvaluatorTest" --no-daemon
```

Expected: BUILD SUCCESSFUL. Bridge V2 accepts all fifteen exact targets without a schema change.

- [ ] **Step 5: Commit the Android contract**

```powershell
git add app/src/main/java/com/gilnun/app/catalog/ServiceCatalog.kt app/src/test/java/com/gilnun/app/catalog/ServiceCatalogTest.kt
git commit -m "Make realistic practice routes meaningful to the guidance engine"
```

### Task 2: Lock the realistic offline asset contract

**Files:**
- Modify: `app/src/test/java/com/gilnun/app/web/PracticeAssetContractTest.kt`

- [ ] **Step 1: Replace three-step asset expectations**

Mirror the fifteen-row Task 1 table in `ExpectedService.checkpoints`, assert five progress steps per service, and update the descriptive assertion:

```kotlin
assertEquals(
    "${service.serviceId} must have exactly five progress steps",
    5,
    Regex(
        """stableKey:\s*"[^"]+".+?effect:\s*"PROGRESS"""",
        RegexOption.DOT_MATCHES_ALL,
    ).findAll(block).count(),
)
```

- [ ] **Step 2: Add realistic-control and layout-update tests**

Add these exact tests:

```kotlin
@Test
fun `each checkpoint exposes four plausible choices without duplicating semantic targets`() {
    assertEquals(15, Regex("""choices:\s*\[""").findAll(javascript).count())
    assertTrue(
        Regex("""choices:\s*\[(?:[^\]]|\](?!,))*\]""", RegexOption.DOT_MATCHES_ALL)
            .findAll(javascript)
            .all { choiceBlock ->
                Regex("""label:\s*"[^"]+"""").findAll(choiceBlock.value).count() >= 4
            },
    )
    assertEquals(
        1,
        javascript.windowed("target.dataset.stableKey".length)
            .count { it == "target.dataset.stableKey" },
    )
}

@Test
fun `first progress action automatically changes presentation without changing bridge schema`() {
    assertContains(html, """id="layout-update-notice"""")
    assertContains(javascript, "function applyAutomaticLayoutUpdate")
    assertContains(javascript, "currentStepIndex === 1")
    assertContains(javascript, """activeLayout === "A"""")
    assertContains(javascript, """document.body.classList.add("layout-b")""")
    assertContains(javascript, "사이트 화면이 업데이트되어 배치가 바뀌었어요")
    assertFalse("Layout changes must not reload the local URL", "window.location" in javascript)
    assertFalse("Bridge V2 keeps only two event shapes", "LAYOUT_CHANGED" in javascript)
}
```

Add assertions for the fixed controls and safety labels:

```kotlin
listOf(
    "주민등록표 초본",
    "영문 주민등록표 등본",
    "서울특별시(가상)",
    "길눈구(가상)",
    "선택발급(모의)",
    "전자문서지갑(연습 제외)",
    "등기우편(연습 제외)",
    "임시 저장",
    "법적 효력 없음",
).forEach { assertContains(javascript, it) }
```

- [ ] **Step 3: Permit only fixed-choice select controls**

Replace the assertion that rejects every `select` with:

```kotlin
assertFalse(Regex("""(?i)<\s*(form|textarea|input)\b""").containsMatchIn(html))
assertFalse(Regex("""(?i)document\.createElement\("(form|textarea|input)"\)""").containsMatchIn(javascript))
assertContains(javascript, """document.createElement("select")""")
assertFalse(Regex("""(?i)\b(name|action|method|enctype)\s*=""").containsMatchIn(html))
```

The test must continue rejecting HTTP URLs, automatic `.click()`, `dispatchEvent`, English `submit`/`payment`/`login`, and Korean `제출`/`결제`/`로그인`.

- [ ] **Step 4: Run the asset contract in the red state**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.web.PracticeAssetContractTest" --no-daemon
```

Expected: FAIL because the JavaScript still contains nine steps, inert decoys, and no automatic layout update.

### Task 3: Build fifteen interactive synthetic checkpoints

**Files:**
- Modify: `app/src/main/assets/welfare/index.html`
- Modify: `app/src/main/assets/welfare/app.js`

- [ ] **Step 1: Add the layout update announcement**

Place this after `.semantic-promise`:

```html
<div id="layout-update-notice" class="layout-update-notice" role="status" hidden>
  <strong>사이트 화면이 업데이트되어 배치가 바뀌었어요</strong>
  <span>위치를 외우지 않아도 괜찮아요. 길눈은 같은 의미를 다시 찾습니다.</span>
</div>
```

Keep the CSP, local stylesheet/script references, and fixed practice banner unchanged.

- [ ] **Step 2: Replace the JavaScript service data**

Each service must contain five step objects in Task 1 order. Every step has these fields:

```javascript
{
  checkpoint: "resident-type",
  stableKey: "resident-copy-select",
  accessibleName: "주민등록표 등본",
  role: "tab",
  nextCheckpoint: "resident-address",
  effect: "PROGRESS",
  type: "ACTION",
  title: "발급할 문서 종류를 선택해 주세요",
  instruction: "비슷한 이름의 문서가 함께 있어요.",
  controlKind: "tabs",
  choices: [
    { label: "주민등록표 등본", value: "copy", target: true },
    { label: "주민등록표 초본", value: "extract" },
    { label: "영문 주민등록표 등본", value: "copy-en" },
    { label: "영문 주민등록표 초본", value: "extract-en" },
  ],
}
```

Use these `controlKind` values in route order:

```text
basic-pension: service-list, confirm-card, radio-group, confirm-card, review-actions
resident-record: tabs, address-selects, radio-group, delivery-select, review-actions
health-screening: service-list, confirm-card, radio-group, radio-group, review-actions
```

Every `choices` array contains at least four labeled fixed options. Only the `target: true` choice may receive `data-stable-key`, `data-expected-state`, the catalog role, and catalog accessible name. The resident address step uses four city options, four district options, and the semantic `주소 확인` button. The resident delivery step creates one fixed-choice `select`; its target value is `온라인발급(본인출력·연습용)`.

- [ ] **Step 3: Render actual local control state**

Create these focused helpers:

```javascript
function markLocalChoice(container, selectedValue) {
  container.querySelectorAll("[data-choice-value]").forEach((choice) => {
    const selected = choice.dataset.choiceValue === selectedValue;
    choice.classList.toggle("is-selected", selected);
    if (choice.getAttribute("role") === "radio") {
      choice.setAttribute("aria-checked", selected ? "true" : "false");
    }
    if (choice.getAttribute("role") === "tab") {
      choice.setAttribute("aria-selected", selected ? "true" : "false");
    }
  });
}

function handleLocalChoice(step, container, choice) {
  markLocalChoice(container, choice.value);
  if (choice.target === true) {
    handleInteraction(step);
    return;
  }
  setStatus("선택은 바뀌었지만 이 연습의 다음 순서는 아니에요.");
}
```

Create semantic attributes in one function only:

```javascript
function applySemanticTarget(target, event) {
  target.dataset.stableKey = event.stableKey;
  target.dataset.expectedState = event.nextCheckpoint;
  target.setAttribute("role", event.role);
  target.setAttribute("aria-label", event.accessibleName);
}
```

The fixed-choice `select` uses `change`, compares `event.target.value` with the target option value, and calls `handleInteraction(step)` only on the exact target value. No code calls `.click()` or `dispatchEvent()`.

- [ ] **Step 4: Use institution-specific compositions**

Add and dispatch these three renderers:

```javascript
function renderServiceStep(step) {
  if (service.serviceId === "resident-record") return renderResidentStep(step);
  if (service.serviceId === "basic-pension") return renderPensionStep(step);
  return renderHealthStep(step);
}
```

`renderResidentStep` orders tabs, announcement accordion, control section, and action bar. `renderPensionStep` orders the service path, applicant summary, control section, and save rail. `renderHealthStep` orders the personal-matters side menu, lookup criteria, control section, and query actions. The low-level choice builders are shared, but each renderer must append groups in a different order and use a different root class.

- [ ] **Step 5: Keep the strict interaction order**

For progress:

```javascript
function handleInteraction(event) {
  emitInteraction(event);
  if (event.effect === "NON_PROGRESS") {
    setStatus(event.status);
    return;
  }

  currentCheckpoint = event.nextCheckpoint;
  currentStepIndex += 1;
  isComplete = currentStepIndex === service.steps.length;
  applyAutomaticLayoutUpdate();
  renderCurrent();
  emitCheckpointChanged();
}
```

This preserves the security rule that the new checkpoint DOM renders before `CHECKPOINT_CHANGED`.

- [ ] **Step 6: Run JavaScript syntax and focused tests**

Run:

```powershell
node --check app/src/main/assets/welfare/app.js
.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.web.PracticeAssetContractTest" --no-daemon
```

Expected: syntax check exits 0. Asset tests may still fail only on the CSS layout assertions introduced in Task 4.

### Task 4: Make the automatic A-to-B change visible and accessible

**Files:**
- Modify: `app/src/main/assets/welfare/app.js`
- Modify: `app/src/main/assets/welfare/style.css`

- [ ] **Step 1: Add the one-time in-page transition**

Initialize:

```javascript
let activeLayout = layout;
let layoutUpdated = activeLayout === "B";
const layoutUpdateNotice = document.getElementById("layout-update-notice");
```

Implement:

```javascript
function applyAutomaticLayoutUpdate() {
  if (layoutUpdated || activeLayout !== "A" || currentStepIndex !== 1) return;
  layoutUpdated = true;
  activeLayout = "B";
  document.body.classList.add("layout-b", "layout-just-updated");
  document.documentElement.dataset.activeLayout = activeLayout;
  layoutUpdateNotice.hidden = false;
  window.setTimeout(() => {
    document.body.classList.remove("layout-just-updated");
  }, 450);
}
```

The timeout removes only a visual class. It does not delay rendering, block input, change a checkpoint, send a bridge event, or navigate the WebView.

- [ ] **Step 2: Give Layout B a materially different mobile composition**

At all widths, Layout B must change group placement instead of collapsing to the same single-column order. Use grid areas:

```css
.service-workspace {
  grid-template-areas: "rail content";
}

.side-menu {
  grid-area: rail;
}

.step-groups {
  grid-area: content;
}

.layout-b .service-workspace {
  grid-template-areas:
    "content"
    "rail";
  grid-template-columns: minmax(0, 1fr);
}

.layout-b .side-menu {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
```

For `max-width: 40rem`, Layout A remains `rail` then `content`; Layout B remains `content` then `rail`. Do not use CSS `order`, `row-reverse`, `column-reverse`, or DOM reparenting.

- [ ] **Step 3: Style three distinct service surfaces**

Use:

```css
.service-resident-record .portal-header { background: #25477D; }
.service-resident-record .control-section { border-radius: 0; }
.service-resident-record .section-heading { background: #36598F; color: #FFFFFF; }

.service-basic-pension .portal-header { background: #147D84; }
.service-basic-pension .control-section { border-radius: 18px; }
.service-basic-pension .section-heading { background: #DDEFF0; color: #0B2838; }

.service-health-screening .portal-header { background: #0B5D50; }
.service-health-screening .control-section { border-left: 8px solid #F2C94C; }
.service-health-screening .section-heading { background: #FFF6CF; color: #0B2838; }
```

Style `button`, `select`, tab, radio, accordion, and action states at 20px or larger and 56px minimum height. Keep horizontal overflow hidden, visible focus rings, and no nested vertical scroll.

- [ ] **Step 4: Add nonblocking motion and reduced motion**

```css
.layout-just-updated .practice-card,
.layout-just-updated .side-menu {
  animation: layout-arrival 420ms ease-out both;
}

@keyframes layout-arrival {
  from { opacity: 0.35; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .layout-just-updated .practice-card,
  .layout-just-updated .side-menu {
    animation: none;
  }
}
```

- [ ] **Step 5: Run the complete offline asset test**

Run:

```powershell
node --check app/src/main/assets/welfare/app.js
.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.web.PracticeAssetContractTest" --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit realistic controls and layout recovery**

```powershell
git add app/src/main/assets/welfare/index.html app/src/main/assets/welfare/app.js app/src/main/assets/welfare/style.css app/src/test/java/com/gilnun/app/web/PracticeAssetContractTest.kt
git commit -m "Prove semantic recovery inside realistic changing service screens"
```

### Task 5: Expose the product advantage in Compose

**Files:**
- Modify: `app/src/main/java/com/gilnun/app/ui/GilnunApp.kt`
- Modify: `app/src/androidTest/java/com/gilnun/app/ManualJourneyTest.kt`

- [ ] **Step 1: Write the failing release-copy tests**

Change Compose assertions to require:

```kotlin
composeRule.onNodeWithText("화면이 바뀌어도, 해야 할 일을 다시 찾아드려요").assertIsDisplayed()
composeRule.onNodeWithText("좌표는 빗나가도, 의미는 다시 찾습니다.").assertIsDisplayed()
composeRule.onNodeWithText("길눈에게 찾아달라고 하기").assertIsDisplayed()
composeRule.onNodeWithText("화면 배치 바꿔보기").assertDoesNotExist()
```

Change the automatic guidance test to assert `1 / 5 단계`.

- [ ] **Step 2: Run Android test compilation**

Run:

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Expected: compile succeeds; the new UI assertions would fail on a connected device until the next step.

- [ ] **Step 3: Rewrite the home benefit hierarchy**

Above the service cards, show:

```kotlin
Text(
    text = "화면이 바뀌어도, 해야 할 일을 다시 찾아드려요",
    style = MaterialTheme.typography.headlineLarge,
    color = GilnunNavy,
)
Text(
    text = "좌표는 빗나가도, 의미는 다시 찾습니다.",
    style = MaterialTheme.typography.headlineMedium,
    color = GilnunTeal,
)
Text(
    text = "복잡한 민원 화면에서 버튼의 이름·역할·다음 단계를 확인해 필요한 곳을 다시 찾습니다.",
    style = MaterialTheme.typography.bodyLarge,
)
```

Keep the supplied logo and three service cards. Change card descriptions to describe the difficult choice:

```text
기초연금: 신청 관계와 확인 항목 사이에서 다음 순서 찾기
주민등록표 등본: 문서·주소·발급 형태·수령 방법을 차례로 선택하기
건강검진: 민원 메뉴와 조회 기준 사이에서 대상 조회 찾기
```

- [ ] **Step 4: Remove the release layout toggle and use dynamic progress**

Change `PracticeTopBar` to:

```kotlin
@Composable
private fun PracticeTopBar(
    stepIndex: Int,
    totalSteps: Int,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = onHome,
            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
        ) {
            Text("← 홈으로")
        }
        Text(
            text = "${stepIndex.coerceIn(1, totalSteps)} / $totalSteps 단계",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

Pass `service.steps.size`, remove `onChangeLayout` from `PracticeScreen`, and keep `selectServiceForTest` for contract tests. Rename the dock action from `도움이 필요해요` to `길눈에게 찾아달라고 하기`; the dialog choices remain unchanged.

- [ ] **Step 5: Compile the release UI**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon
```

Expected: BUILD SUCCESSFUL.

### Task 6: Update five-step instrumentation contracts

**Files:**
- Modify: `app/src/androidTest/java/com/gilnun/app/GuidanceJourneyInstrumentationTest.kt`
- Modify: `app/src/androidTest/java/com/gilnun/app/ManualJourneyTest.kt`

- [ ] **Step 1: Assert complete five-step routes**

Use:

```kotlin
ServiceId.entries.forEach { serviceId ->
    val service = ServiceCatalog.require(serviceId)
    assertEquals(5, service.steps.size)
    assertEquals(6, service.route.size)
}
```

The existing `completeWithoutHelp` loop already advances every catalog step and therefore needs no hard-coded route duplication.

- [ ] **Step 2: Add exact A/B semantic preservation**

For every service and checkpoint:

```kotlin
ServiceId.entries.forEach { serviceId ->
    val service = ServiceCatalog.require(serviceId)
    service.steps.forEach { step ->
        val patch = requireNotNull(step.patch)
        assertEquals(step.primaryAction?.stableKey, patch.stableKey)
        assertEquals(step.primaryAction?.role, patch.role)
        assertEquals(step.primaryAction?.accessibleName, patch.accessibleName)
        assertEquals(step.primaryAction?.expectedCheckpoint, patch.expectedState)
    }
}
```

Keep the cross-service rejection, helper handoff, automatic-guidance-no-click, and completion-help tests.

- [ ] **Step 3: Compile all instrumentation tests**

Run:

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Compose and instrumentation changes**

```powershell
git add app/src/main/java/com/gilnun/app/ui/GilnunApp.kt app/src/androidTest/java/com/gilnun/app/GuidanceJourneyInstrumentationTest.kt app/src/androidTest/java/com/gilnun/app/ManualJourneyTest.kt
git commit -m "Explain layout-resilient guidance before users need it"
```

### Task 7: Bump the update APK and preserve the security boundary

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `scripts/verify-boundaries.ps1`
- Modify: `scripts/final-mvp-gate.ps1`

- [ ] **Step 1: Bump the package version**

```kotlin
versionCode = 3
versionName = "0.2.1"
```

- [ ] **Step 2: Allow fixed selects but keep data-entry and submission blocked**

In `verify-boundaries.ps1`, use:

```powershell
if ($allAssets -match '(?is)<\s*(form|input|textarea)\b') {
    throw 'Practice assets contain a form or free-entry input element.'
}
if ($allAssets -match '(?i)\b(submit|payment|login)\b|제출|결제|로그인') {
    throw 'Practice assets contain a real submission, payment, or login affordance.'
}
```

Keep CSP `form-action 'none'`, network, permission, cookie, file, WebView bridge, and packaged-origin assertions unchanged.

- [ ] **Step 3: Update packaged version assertions**

```powershell
Assert-Contains -Text $manifest -Expected 'android:versionCode="3"' -Description 'version code'
Assert-Contains -Text $manifest -Expected 'android:versionName="0.2.1"' -Description 'version name'
```

Update the generated summary title and package/version line to `v0.2.1` and `0.2.1 (3)`. Change the device note from `A↔B` to `A→B 자동 재배치 후 의미 매칭`.

- [ ] **Step 4: Run static and focused version checks**

Run:

```powershell
git diff --check
node --check app/src/main/assets/welfare/app.js
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
```

Expected: BUILD SUCCESSFUL and no diff whitespace errors.

### Task 8: Run the signed APK gate and publish evidence

**Files:**
- Modify: `outputs/gilnun-mvp.apk`
- Modify: `outputs/gilnun-mvp.apk.sha256`
- Modify: `outputs/verification-summary.md`

- [ ] **Step 1: Confirm no website source changes**

Run:

```powershell
git status --short -- web
```

Expected: no output.

- [ ] **Step 2: Run the complete release gate**

Run:

```powershell
.\scripts\final-mvp-gate.ps1
```

Expected:

```text
FINAL_MVP_GATE_AUTOMATED_PASS
DEVICE_BLOCKED
```

The signer must be:

```text
9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991
```

- [ ] **Step 3: Verify the exact deliverable**

Run:

```powershell
Get-FileHash -Algorithm SHA256 outputs\gilnun-mvp.apk
Get-Content outputs\gilnun-mvp.apk.sha256
```

Expected: both lowercase-normalized digests match. `verification-summary.md` must state package `com.gilnun.app`, version `0.2.1 (3)`, automated PASS items, and honest `DEVICE_BLOCKED` items.

- [ ] **Step 4: Commit implementation and release evidence**

Use a Lore commit that records:

```text
Constraint: No real institution connection, personal data, submission, payment, or network permission.
Rejected: URL reload for the layout change | it would reset the current WebView checkpoint.
Confidence: high
Scope-risk: moderate
Directive: Keep Bridge V2 at two event shapes and exact semantic matching fail-closed.
Tested: JVM tests, release lint, debug/release/androidTest APK builds, boundary scan, zipalign, apksigner.
Not-tested: TalkBack, physical-device TTS, 150/200% font, motion, offline journey, and update install remain DEVICE_BLOCKED without a connected device.
```

- [ ] **Step 5: Push the completed branch**

Run:

```powershell
git push origin codex/gilnun-android-mvp
```

Expected: the remote branch advances to the final local commit and `git status -sb` shows no ahead/behind count.
