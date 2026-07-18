package com.gilnun.app.web

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.readText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeAssetContractTest {
    @Test
    fun `bundle contains only the three local resources allowed by the WebView`() {
        val assetNames =
            Files.list(assetDirectory).use { paths ->
                paths.map(Path::name).sorted().toList()
            }

        assertEquals(listOf("app.js", "index.html", "style.css"), assetNames)
        assertContains(html, """<link rel="stylesheet" href="style.css">""")
        assertContains(html, """<script src="app.js" defer></script>""")
    }

    @Test
    fun `HTML is a Korean offline-only shell with the fixed safety banner`() {
        assertTrue(Regex("""<html\s+lang="ko"""").containsMatchIn(html))
        assertContains(html, "연습용 화면 · 실제 기관과 연결되지 않아요")
        assertContains(html, """default-src 'none'""")
        assertContains(html, """script-src 'self'""")
        assertContains(html, """style-src 'self'""")
        assertContains(html, """connect-src 'none'""")
        assertContains(html, """form-action 'none'""")
        assertContains(html, """base-uri 'none'""")
        assertContains(html, """data-revision="2026-07"""")
        assertFalse("Inline scripts are forbidden", Regex("""<script(?!\s+src=)""").containsMatchIn(html))
        assertFalse("Inline styles are forbidden", Regex("""\sstyle\s*=""").containsMatchIn(html))
        assertFalse("Inline event handlers are forbidden", Regex("""\son[a-z]+\s*=""").containsMatchIn(html))
    }

    @Test
    fun `practice shell explains semantic recovery and exposes institution shaped landmarks`() {
        assertContains(html, "좌표는 빗나가도, 의미는 다시 찾습니다.")
        assertContains(html, "버튼의 위치가 달라져도 이름·역할·다음 상태를 확인해 다시 찾습니다.")
        assertContains(html, """id="layout-update-notice"""")
        assertContains(html, """role="status"""")
        assertContains(html, "사이트 화면이 업데이트되어 배치가 바뀌었어요")
        assertContains(html, "위치를 외우지 않아도 괜찮아요. 길눈은 같은 의미를 다시 찾습니다.")
        assertTrue(
            "The update notice must immediately follow the semantic promise",
            Regex(
                """semantic-promise[\s\S]*?</section>\s*<[^>]+id="layout-update-notice"[^>]*role="status"[^>]*hidden""",
            ).containsMatchIn(html),
        )
        listOf(
            "portal-header",
            "portal-nav",
            "portal-breadcrumb",
            "portal-tabs",
            "notice-board",
            "side-menu",
        ).forEach { className ->
            assertContains(html, """class="$className""")
        }
        assertContains(javascript, "institution: \"복지로형\"")
        assertContains(javascript, "institution: \"정부24형\"")
        assertContains(javascript, "institution: \"건강보험형\"")
        assertContains(javascript, "read-only-choice-row")
        assertContains(javascript, "grouped-summary")
        assertContains(javascript, "function renderPensionStep")
        assertContains(javascript, "function renderResidentStep")
        assertContains(javascript, "function renderHealthStep")
        assertFalse(
            "Realistic services must not fall back to one generic renderer",
            "function renderInstitutionStep" in javascript,
        )
        assertFalse("Decorative shell must not create bridge targets", "data-stable-key" in html)
    }

    @Test
    fun `release copy does not expose layout page or revision metadata`() {
        assertFalse("Layout badge must not be visible", "layout-label" in html)
        assertFalse("Page identifier must not be visible", "page-label" in html)
        assertFalse("Revision metadata must not be rendered", Regex("""<footer\b""").containsMatchIn(html))
        assertFalse("Layout name must remain internal", Regex("""`배치\s+\$\{layout}""").containsMatchIn(javascript))
        assertFalse("Page identifier must remain internal", Regex("""`page\s+\$\{service\.pageId}""").containsMatchIn(javascript))
    }

    @Test
    fun `catalog has exactly three services with exact five-step routes`() {
        val expected =
            listOf(
                ExpectedService(
                    serviceId = "basic-pension",
                    pageId = "bokjiro-basic-pension",
                    checkpoints =
                        listOf(
                            ExpectedStep(
                                "pension-service",
                                "pension-service-select",
                                "button",
                                "기초연금 신청 연습",
                                "pension-applicant",
                            ),
                            ExpectedStep(
                                "pension-applicant",
                                "pension-applicant-confirm",
                                "button",
                                "연습 사용자 정보 확인",
                                "pension-method",
                            ),
                            ExpectedStep(
                                "pension-method",
                                "pension-self-apply",
                                "radio",
                                "본인이 신청해요",
                                "pension-contact",
                            ),
                            ExpectedStep(
                                "pension-contact",
                                "pension-contact-confirm",
                                "button",
                                "연락 방법 확인",
                                "pension-review",
                            ),
                            ExpectedStep(
                                "pension-review",
                                "pension-review-confirm",
                                "button",
                                "신청 내용 확인",
                                "pension-complete",
                            ),
                        ),
                    friction =
                        ExpectedFriction(
                            checkpoint = "pension-review",
                            type = "ACTION",
                            stableKey = "pension-save-draft",
                            accessibleName = "임시 저장",
                        ),
                    completionCheckpoint = "pension-complete",
                    completionTitle = "신청 연습을 마쳤어요",
                ),
                ExpectedService(
                    serviceId = "resident-record",
                    pageId = "gov24-resident-record",
                    checkpoints =
                        listOf(
                            ExpectedStep(
                                "resident-type",
                                "resident-copy-select",
                                "tab",
                                "주민등록표 등본",
                                "resident-address",
                            ),
                            ExpectedStep(
                                "resident-address",
                                "resident-address-confirm",
                                "button",
                                "주소 확인",
                                "resident-issue-type",
                            ),
                            ExpectedStep(
                                "resident-issue-type",
                                "resident-standard-issue",
                                "radio",
                                "발급(모의)",
                                "resident-delivery",
                            ),
                            ExpectedStep(
                                "resident-delivery",
                                "resident-online-delivery",
                                "combobox",
                                "온라인발급(본인출력·연습용)",
                                "resident-review",
                            ),
                            ExpectedStep(
                                "resident-review",
                                "resident-finish-practice",
                                "button",
                                "민원 신청 연습 마치기",
                                "resident-complete",
                            ),
                        ),
                    friction =
                        ExpectedFriction(
                            checkpoint = "resident-delivery",
                            type = "HELP",
                            stableKey = "resident-delivery-help",
                            accessibleName = "수령 방법 안내",
                        ),
                    completionCheckpoint = "resident-complete",
                    completionTitle = "발급 연습을 마쳤어요",
                ),
                ExpectedService(
                    serviceId = "health-screening",
                    pageId = "nhis-health-screening",
                    checkpoints =
                        listOf(
                            ExpectedStep(
                                "health-service",
                                "health-service-select",
                                "button",
                                "건강검진 대상 조회 연습",
                                "health-person",
                            ),
                            ExpectedStep(
                                "health-person",
                                "health-person-confirm",
                                "button",
                                "연습 사용자 정보 확인",
                                "health-year",
                            ),
                            ExpectedStep(
                                "health-year",
                                "health-year-2026",
                                "radio",
                                "2026년(가상)",
                                "health-kind",
                            ),
                            ExpectedStep(
                                "health-kind",
                                "health-general-screening",
                                "radio",
                                "일반건강검진(모의)",
                                "health-query",
                            ),
                            ExpectedStep(
                                "health-query",
                                "health-screening-query",
                                "button",
                                "건강검진 대상 조회",
                                "health-complete",
                            ),
                        ),
                    friction =
                        ExpectedFriction(
                            checkpoint = "health-query",
                            type = "HELP",
                            stableKey = "health-schedule-help",
                            accessibleName = "검진 일정 안내",
                        ),
                    completionCheckpoint = "health-complete",
                    completionTitle = "대상 조회 연습을 마쳤어요",
                ),
            )

        assertEquals(3, Regex("""serviceId:\s*"[^"]+"""").findAll(javascript).count())
        expected.forEach { service ->
            val block = serviceBlock(service.serviceId)
            assertContains(block, """serviceId: "${service.serviceId}"""")
            assertContains(block, """pageId: "${service.pageId}"""")
            assertContains(block, "revision: REVISION")
            assertEquals(
                "${service.serviceId} must have exactly five progress steps",
                5,
                Regex("""stableKey:\s*"[^"]+".+?effect:\s*"PROGRESS"""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(block)
                    .count(),
            )
            service.checkpoints.forEach { step ->
                assertContains(
                    compact(block),
                    compact(
                        """
                        checkpoint: "${step.checkpoint}",
                        stableKey: "${step.stableKey}",
                        role: "${step.role}",
                        accessibleName: "${step.accessibleName}",
                        nextCheckpoint: "${step.nextCheckpoint}",
                        """.trimIndent(),
                    ),
                )
            }
            assertContains(
                compact(block),
                compact(
                    """
                    checkpoint: "${service.friction.checkpoint}",
                    type: "${service.friction.type}",
                    stableKey: "${service.friction.stableKey}",
                    accessibleName: "${service.friction.accessibleName}",
                    effect: "NON_PROGRESS",
                    """.trimIndent(),
                ),
            )
            assertContains(
                compact(block),
                compact(
                    """
                    completion: {
                      checkpoint: "${service.completionCheckpoint}",
                      title: "${service.completionTitle}",
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun `all fifteen checkpoints expose at least four deterministic fixed choices`() {
        val choiceArrays =
            Regex(
                """choices:\s*Object\.freeze\(\[((?:\s*"[^"]+"\s*,?){4,})\s*\]\)""",
                RegexOption.DOT_MATCHES_ALL,
            ).findAll(javascript).toList()

        assertEquals("One fixed choice array is required per checkpoint", 15, choiceArrays.size)
        choiceArrays.forEachIndexed { index, match ->
            assertTrue(
                "Checkpoint ${index + 1} must expose at least four fixed choices",
                Regex(""""[^"]+"""").findAll(match.groupValues[1]).count() >= 4,
            )
        }
        assertContains(javascript, "function selectLocalChoice")
        assertContains(javascript, "aria-pressed")
        assertContains(javascript, "aria-checked")
        assertContains(javascript, "aria-selected")
        assertContains(javascript, """createElement("select"""")
    }

    @Test
    fun `practice copy is synthetic and exposes only the allowed mock results`() {
        assertContains(javascript, "연습 사용자 (가상)")
        assertContains(javascript, "연습 세대 (가상)")
        assertContains(javascript, "법적 효력 없음")
        assertContains(javascript, "일반건강검진 대상(모의)")
        assertEquals(1, javascript.windowed("일반건강검진 대상(모의)".length).count {
            it == "일반건강검진 대상(모의)"
        })

        val allAssets = listOf(html, javascript, css).joinToString("\n")
        listOf(
            "주민등록번호",
            "생년월일",
            "전화번호",
            "휴대전화",
            "상세 주소",
            "계좌번호",
            "혈압",
            "혈당",
            "진단명",
            "병원명",
            "예약 시간",
        ).forEach { forbidden ->
            assertFalse("Forbidden personal or medical value label: $forbidden", forbidden in allAssets)
        }
        assertFalse(Regex("""(?i)https?://""").containsMatchIn(allAssets))
        assertFalse(Regex("""(?i)<\s*form\b""").containsMatchIn(html))
        assertFalse(Regex("""(?i)<\s*(input|textarea)\b""").containsMatchIn(html))
        assertFalse(Regex("""(?i)\.(click|dispatchEvent)\s*\(""").containsMatchIn(javascript))
        assertFalse(Regex("""(?i)\b(submit|payment|login)\b""").containsMatchIn(javascript))
    }

    @Test
    fun `query contract supports only the three services and two visual layouts`() {
        assertContains(
            compact(javascript),
            compact(
                """
                const SUPPORTED_SERVICE_IDS = Object.freeze([
                  "basic-pension",
                  "resident-record",
                  "health-screening",
                ]);
                """.trimIndent(),
            ),
        )
        assertContains(
            compact(javascript),
            """const SUPPORTED_LAYOUTS = Object.freeze(["A", "B"]);""",
        )
        assertContains(javascript, """query.get("service")""")
        assertContains(javascript, """query.get("layout")""")
        assertContains(javascript, "let activeLayout")
        assertContains(javascript, """document.body.dataset.activeLayout = activeLayout""")
        assertContains(javascript, "function applyAutomaticLayoutUpdate")
        assertContains(javascript, "currentStepIndex !== 1")
        assertContains(javascript, """activeLayout !== "A"""")
        assertContains(javascript, """activeLayout = "B"""")
        assertContains(javascript, """.classList.add("layout-b", "layout-just-updated")""")
        assertContains(javascript, "layoutUpdateNotice.hidden = false")
        assertContains(javascript, "450")
        assertFalse("The automatic update must not reload the document", ".reload(" in javascript)
        assertTrue(
            "Layout B must place content before the progress rail",
            Regex(
                """\.layout-b\s+\.service-workspace\s*\{[^}]*grid-template-areas:\s*"content rail"""",
                RegexOption.DOT_MATCHES_ALL,
            )
                .containsMatchIn(css),
        )
        assertTrue(
            "Mobile layout B must still place content before the progress rail",
            Regex(
                """@media\s*\(max-width:\s*40rem\)[\s\S]*?\.layout-b\s+\.service-workspace\s*\{[^}]*grid-template-areas:\s*"content"\s*"rail"""",
            ).containsMatchIn(css),
        )
        assertFalse("CSS order is forbidden", Regex("""\border\s*:""").containsMatchIn(css))
        assertFalse("Reverse direction is forbidden", "reverse" in css)
    }

    @Test
    fun `layout B relocates groups while semantic recovery keeps one exact target`() {
        assertContains(javascript, "function createSemanticTarget")
        assertContains(javascript, "applyAutomaticLayoutUpdate();")
        assertContains(javascript, "위치가 달라져도 의미를 다시 찾았어요.")
        assertContains(javascript, "이름·역할·다음 상태")
        assertContains(css, ".layout-b .service-workspace")
        assertContains(css, ".layout-b .step-groups")
        assertEquals(
            "Only the semantic target factory may assign stable keys",
            1,
            javascript.windowed("control.dataset.stableKey =".length).count {
                it == "control.dataset.stableKey ="
            },
        )
        assertTrue(
            "The new screen and layout must render before checkpoint notification",
            Regex(
                """applyAutomaticLayoutUpdate\(\);\s*renderCurrent\(\);\s*emitCheckpointChanged\(\);""",
            ).containsMatchIn(javascript),
        )
    }

    @Test
    fun `Bridge V2 emits only strict catalog-owned interaction and checkpoint fields`() {
        assertContains(
            compact(javascript),
            compact(
                """
                const INTERACTION_FIELDS = Object.freeze([
                  "schemaVersion",
                  "type",
                  "serviceId",
                  "revision",
                  "checkpoint",
                  "stableKey",
                  "role",
                  "accessibleName",
                  "effect",
                ]);
                """.trimIndent(),
            ),
        )
        assertContains(
            compact(javascript),
            compact(
                """
                const CHECKPOINT_FIELDS = Object.freeze([
                  "schemaVersion",
                  "type",
                  "serviceId",
                  "revision",
                  "checkpoint",
                ]);
                """.trimIndent(),
            ),
        )
        assertContains(javascript, "const BRIDGE_SCHEMA_VERSION = 2;")
        assertContains(javascript, "schemaVersion: BRIDGE_SCHEMA_VERSION")
        assertContains(javascript, """type: "CHECKPOINT_CHANGED"""")
        assertContains(javascript, "exactKeys(payload, INTERACTION_FIELDS)")
        assertContains(javascript, "exactKeys(payload, CHECKPOINT_FIELDS)")
        assertContains(javascript, "bridge.postMessage(JSON.stringify(payload))")
        assertTrue(
            "The next screen must render before CHECKPOINT_CHANGED is emitted",
            Regex("""renderCurrent\(\);\s*emitCheckpointChanged\(\);""").containsMatchIn(javascript),
        )

        val interactionBody = functionBody("emitInteraction")
        listOf("rawText", "freeText", "value", "url", "selector", "coordinate", "clientX", "clientY")
            .forEach { forbidden ->
                assertFalse(
                    "Interaction payload must not include $forbidden",
                    Regex("""(?i)\b${Regex.escape(forbidden)}\b""").containsMatchIn(interactionBody),
                )
            }
    }

    @Test
    fun `native highlights require one exact current PatchV1 target and never activate it`() {
        assertContains(
            compact(javascript),
            compact(
                """
                const HIGHLIGHT_FIELDS = Object.freeze([
                  "schemaVersion",
                  "command",
                  "pageId",
                  "compatibleRevision",
                  "stableKey",
                  "role",
                  "accessibleName",
                  "expectedState",
                ]);
                """.trimIndent(),
            ),
        )
        assertContains(
            compact(javascript),
            """const CLEAR_HIGHLIGHT_FIELDS = Object.freeze(["schemaVersion", "command"]);""",
        )
        assertContains(javascript, "const PATCH_SCHEMA_VERSION = 1;")
        val highlightBody = functionBody("applyHighlight")
        assertContains(highlightBody, "exactKeys(command, HIGHLIGHT_FIELDS)")
        assertContains(highlightBody, "command.schemaVersion !== PATCH_SCHEMA_VERSION")
        assertContains(highlightBody, "command.pageId !== service.pageId")
        assertContains(highlightBody, "command.compatibleRevision !== service.revision")
        assertContains(highlightBody, "target.dataset.stableKey === command.stableKey")
        assertContains(highlightBody, "target.dataset.expectedState === command.expectedState")
        assertContains(highlightBody, "target.getAttribute(\"role\") === command.role")
        assertContains(highlightBody, "target.getAttribute(\"aria-label\") === command.accessibleName")
        assertContains(highlightBody, "matches.length !== 1")
        assertFalse(Regex("""(?i)\.(click|dispatchEvent)\s*\(""").containsMatchIn(highlightBody))
        assertContains(javascript, """command.command === "CLEAR_HIGHLIGHT"""")
        assertContains(javascript, "command.schemaVersion === PATCH_SCHEMA_VERSION")
    }

    @Test
    fun `styles meet large-text touch focus overflow and reduced-motion contracts`() {
        listOf("#0B2838", "#147D84", "#F2C94C", "#F7FAFC", "#D75A5A").forEach { color ->
            assertTrue("Required palette color missing: $color", css.contains(color, ignoreCase = true))
        }
        assertTrue(
            Regex("""body\s*\{[^}]*font-size:\s*20px[^}]*line-height:\s*1\.55""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(css),
        )
        assertTrue(
            Regex(
                """\.action-button\s*\{[^}]*min-height:\s*56px[^}]*font-size:\s*22px[^}]*font-weight:\s*(700|bold)""",
                RegexOption.DOT_MATCHES_ALL,
            ).containsMatchIn(css),
        )
        assertTrue(
            Regex("""h1,\s*h2,\s*h3\s*\{[^}]*font-size:\s*(28|29|30)px""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(css),
        )
        assertTrue(
            "Progress labels must remain at least 20px",
            Regex("""\.progress-list\s+li\s*\{[^}]*font-size:\s*(2[0-9]|[3-9][0-9])px""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(css),
        )
        assertTrue(
            "Visible eyebrow, panel, and step labels must remain at least 20px",
            Regex(
                """\.eyebrow,\s*\.panel-label,\s*\.step-number\s*\{[^}]*font-size:\s*20px""",
                RegexOption.DOT_MATCHES_ALL,
            ).containsMatchIn(css),
        )
        assertContains(css, ":focus-visible")
        assertTrue(
            "Focus indicator must include a high-contrast navy ring",
            Regex(""":focus-visible\s*\{[^}]*outline:\s*4px\s+solid\s+var\(--navy\)""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(css),
        )
        assertContains(css, "@media (prefers-reduced-motion: reduce)")
        assertTrue(Regex("""overflow-x:\s*(hidden|clip)""").containsMatchIn(css))
        assertFalse(
            "Nested vertical scrolling is not allowed",
            Regex("""overflow-y:\s*(auto|scroll)""").containsMatchIn(css),
        )
        assertContains(css, "min-width: 0")
    }

    private fun serviceBlock(serviceId: String): String {
        val marker = """serviceId: "$serviceId""""
        val start = javascript.indexOf(marker)
        assertTrue("Missing service block: $serviceId", start >= 0)
        val next =
            listOf("basic-pension", "resident-record", "health-screening")
                .map { """serviceId: "$it"""" }
                .map { javascript.indexOf(it, start + marker.length) }
                .filter { it >= 0 }
                .minOrNull()
                ?: javascript.indexOf("const query", start).takeIf { it >= 0 }
                ?: javascript.length
        return javascript.substring(start, next)
    }

    private fun functionBody(name: String): String {
        val start = javascript.indexOf("function $name(")
        assertTrue("Missing function: $name", start >= 0)
        var depth = 0
        var opened = false
        for (index in start until javascript.length) {
            when (javascript[index]) {
                '{' -> {
                    depth += 1
                    opened = true
                }
                '}' -> {
                    depth -= 1
                    if (opened && depth == 0) return javascript.substring(start, index + 1)
                }
            }
        }
        error("Unclosed function: $name")
    }

    private fun compact(value: String): String = value.replace(Regex("""\s+"""), " ").trim()

    private fun assertContains(
        actual: String,
        expected: String,
    ) {
        assertTrue("Expected asset to contain: $expected", actual.contains(expected))
    }

    private data class ExpectedStep(
        val checkpoint: String,
        val stableKey: String,
        val role: String,
        val accessibleName: String,
        val nextCheckpoint: String,
    )

    private data class ExpectedFriction(
        val checkpoint: String,
        val type: String,
        val stableKey: String,
        val accessibleName: String,
    )

    private data class ExpectedService(
        val serviceId: String,
        val pageId: String,
        val checkpoints: List<ExpectedStep>,
        val friction: ExpectedFriction,
        val completionCheckpoint: String,
        val completionTitle: String,
    )

    private companion object {
        val assetDirectory: Path by lazy {
            listOf(
                Paths.get("app", "src", "main", "assets", "welfare"),
                Paths.get("src", "main", "assets", "welfare"),
            ).map(Path::toAbsolutePath)
                .firstOrNull(Files::isDirectory)
                ?: error("Could not locate app/src/main/assets/welfare")
        }
        val html: String by lazy { assetDirectory.resolve("index.html").readText() }
        val javascript: String by lazy { assetDirectory.resolve("app.js").readText() }
        val css: String by lazy { assetDirectory.resolve("style.css").readText() }
    }
}
