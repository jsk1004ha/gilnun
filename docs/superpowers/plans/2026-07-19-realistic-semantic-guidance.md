# Realistic Semantic Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android APK visibly prove that Gilnun can guide older users through realistic, changing public-service screens by semantic meaning rather than coordinates.

**Architecture:** Preserve the three-service catalog and strict PatchV1/Bridge V2 boundary. Enrich the APK-owned WebView fixture with three distinct institution-shaped shells and large synthetic controls, then add native splash/loading motion and a home value proposition. Layout A/B changes only presentation; the exact semantic target contract remains unchanged.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android WebViewAssetLoader, offline HTML/CSS/JavaScript, JUnit, Gradle Android plugin.

---

### Task 1: Lock the richer-screen contract

**Files:**
- Modify: `app/src/test/java/com/gilnun/app/web/PracticeAssetContractTest.kt`
- Modify: `app/src/test/java/com/gilnun/app/catalog/ServiceCatalogTest.kt`

- [ ] **Step 1: Add failing asset assertions**

Assert that the bundle contains the exact value proposition, three distinct portal names, realistic structures (`portal-nav`, `notice-board`, `form-section`, `choice-grid`), at least three non-progress visual decoys per service, and no real form/input/submission/network elements.

- [ ] **Step 2: Run the focused tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.web.PracticeAssetContractTest" --no-daemon`

Expected: FAIL because the value proposition and institution-shaped structures are absent.

- [ ] **Step 3: Preserve semantic invariants**

Keep assertions that each checkpoint exposes exactly one primary target with the catalog-owned `stableKey`, `role`, `accessibleName`, and `expectedState`; Layout B must move groups without reversing focus order.

### Task 2: Build realistic offline public-service fixtures

**Files:**
- Modify: `app/src/main/assets/welfare/index.html`
- Modify: `app/src/main/assets/welfare/style.css`
- Modify: `app/src/main/assets/welfare/app.js`

- [ ] **Step 1: Add the portal shell**

Add a synthetic official-site notice, portal identity, utility navigation, breadcrumb, service tabs, announcement panel, and a persistent semantic-explanation panel. Keep the fixed practice banner and CSP unchanged.

- [ ] **Step 2: Render institution-specific complexity**

For each service, render realistic read-only tab rows, grouped choices, informational sections, status badges, related-menu buttons, and one current progress action. Use `(가상)`, `(모의)`, and `법적 효력 없음`; do not use `<form>`, `<input>`, `<select>`, real URLs, actual logos, or free-form data.

- [ ] **Step 3: Make Layout B visibly different**

Move the portal navigation, step navigator, information rail, and action group using CSS grid areas and column changes while preserving DOM order. Highlight only the exact semantic match and show “위치가 달라져도 의미를 다시 찾았어요.”

- [ ] **Step 4: Run focused tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.gilnun.app.web.PracticeAssetContractTest" --no-daemon`

Expected: PASS.

### Task 3: Add native value proposition and motion

**Files:**
- Modify: `app/src/main/java/com/gilnun/app/ui/GilnunApp.kt`
- Modify: `app/src/main/java/com/gilnun/app/GilnunViewModel.kt`
- Test: `app/src/test/java/com/gilnun/app/GilnunViewModelTest.kt`

- [ ] **Step 1: Add a testable startup state**

Introduce an in-memory startup presentation state that begins at splash, moves to home after a short coroutine delay, and never persists. Service selection briefly shows a loading layer before the existing practice state.

- [ ] **Step 2: Add splash and loading composables**

Animate the supplied vector logo with opacity and scale, add a short teal progress stroke, and use `LocalMotionDurationScale`/system animator scale semantics so disabled animation resolves immediately. Keep transitions under 500 ms and never animate layout dimensions.

- [ ] **Step 3: Rewrite the home hierarchy**

Show “좌표는 빗나가도, 의미는 다시 찾습니다.” above three proof points: complex screens, changed layouts, direct user choice. Label the three cards as 복지로형, 정부24형, 건강보험형 synthetic practices.

- [ ] **Step 4: Run JVM and compile checks**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon`

Expected: BUILD SUCCESSFUL.

### Task 4: Verify and publish the updated APK

**Files:**
- Modify: `outputs/gilnun-mvp.apk.sha256`
- Modify: `outputs/verification-summary.md`
- Produce: `outputs/gilnun-mvp.apk`

- [ ] **Step 1: Run static source checks**

Run: `node --check app/src/main/assets/welfare/app.js`

Expected: exit 0.

Run: `git diff --check`

Expected: exit 0.

- [ ] **Step 2: Run the APK-only release gate**

Run: `.\scripts\final-mvp-gate.ps1`

Expected: `FINAL_MVP_GATE_AUTOMATED_PASS`, exact signer `9afeec4a9d95be7c5c24c31dad220cd8531548354e3c852634a2e3e2c49ea991`, and device-only checks recorded as `DEVICE_BLOCKED`.

- [ ] **Step 3: Verify deliverables**

Confirm the output APK hash matches `outputs/gilnun-mvp.apk.sha256`, package is `com.gilnun.app`, version is `0.2.0 (2)`, and `/web` has zero changes.

- [ ] **Step 4: Commit with Lore trailers**

Commit the implementation and refreshed evidence, recording automated tests and the unrun physical-device motion, TalkBack, font-scaling, update-install, and actual DOM instrumentation gaps.
