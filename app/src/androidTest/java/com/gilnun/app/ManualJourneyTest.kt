package com.gilnun.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.web.PracticeLayout
import com.gilnun.app.web.PracticeUrlPolicy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsOnlyThreeReleaseServiceChoicesWithoutDeveloperControls() {
        composeRule.onNodeWithText("길눈").assertIsDisplayed()
        composeRule.onNodeWithText("화면이 바뀌어도, 해야 할 일을 다시 찾아드려요").assertIsDisplayed()
        composeRule.onNodeWithText("좌표는 빗나가도, 의미는 다시 찾습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("기초연금 신청 연습").assertIsDisplayed()
        composeRule.onNodeWithText("주민등록표 등본 발급 연습").assertIsDisplayed()
        composeRule.onNodeWithText("건강검진 대상 조회 연습").assertIsDisplayed()
        composeRule.onNodeWithText("화면 배치 바꿔보기").assertDoesNotExist()
        composeRule.onNodeWithText("Demo Reset").assertDoesNotExist()
        composeRule.onNodeWithText("레이아웃 A").assertDoesNotExist()
        composeRule.onNodeWithText("PatchV1 준비").assertDoesNotExist()
    }

    @Test
    fun directHelpOffersExactAutomaticHelperAndDeclineChoices() {
        composeRule.onNodeWithText("기초연금 신청 연습").performClick()
        composeRule.onNodeWithText("길눈에게 찾아달라고 하기").performClick()

        composeRule.onNodeWithText("자동 안내 받기").assertIsDisplayed()
        composeRule.onNodeWithText("가족·도우미에게 넘기기").assertIsDisplayed()
        composeRule.onNodeWithText("지금은 괜찮아요").assertIsDisplayed().performClick()
    }

    @Test
    fun helperHandoffIsNativeAndNamesOnlyCurrentAllowedTarget() {
        composeRule.onNodeWithText("주민등록표 등본 발급 연습").performClick()
        composeRule.waitForIdle()
        assertSingleWebView()

        composeRule.onNodeWithText("길눈에게 찾아달라고 하기").performClick()
        composeRule.onNodeWithText("가족·도우미에게 넘기기").performClick()

        composeRule.onNodeWithText("가족·도우미 확인").assertIsDisplayed()
        composeRule.onNodeWithText("주민등록표 등본").assertIsDisplayed()
        composeRule.onNodeWithText("이곳을 안내해 주세요").assertIsDisplayed()
        assertSingleWebView()

        composeRule.onNodeWithText("이곳을 안내해 주세요").performClick()
        composeRule.onNodeWithText("어르신께 돌려드려요").assertIsDisplayed()
        assertSingleWebView()

        composeRule.onNodeWithText("연습 화면으로 돌아가기").performClick()
        composeRule.onNodeWithText("도우미가 확인한 곳을 표시했어요. 어르신이 직접 선택해 주세요.")
            .assertIsDisplayed()
        assertSingleWebView()
    }

    @Test
    fun residentDomAutomaticallyRelocatesAndKeepsOneExactNextTarget() {
        composeRule.onNodeWithText("주민등록표 등본 발급 연습").performClick()
        assertSingleWebView()
        waitForJavascript(
            expected = "true",
            script = """document.querySelector('[data-stable-key="resident-copy-select"]') !== null""",
        )

        evaluateJavascript(
            """document.querySelector('[data-stable-key="resident-copy-select"]').click(); "clicked"""",
        )

        waitForJavascript("B", "document.body.dataset.activeLayout")
        composeRule.onNodeWithText("2 / 5 단계").assertIsDisplayed()
        assertEquals("false", evaluateJavascript("""document.getElementById("layout-update-notice").hidden"""))
        assertEquals("practice-root", evaluateJavascript("""document.getElementById("layout-update-notice").parentElement.id"""))
        assertEquals("5", evaluateJavascript("""document.querySelectorAll(".resident-form-section").length"""))
        assertEquals(
            "resident-address",
            evaluateJavascript(
                """document.querySelector(".resident-form-section").dataset.sectionCheckpoint""",
            ),
        )
        assertEquals("1", evaluateJavascript("""document.querySelectorAll("[data-stable-key]").length"""))
        assertEquals(
            "resident-address-confirm",
            evaluateJavascript(
                """document.querySelector("[data-stable-key]").dataset.stableKey""",
            ),
        )
    }

    @Test
    fun automaticGuidanceHighlightsWithoutAdvancingOrClicking() {
        composeRule.onNodeWithText("건강검진 대상 조회 연습").performClick()
        composeRule.onNodeWithText("길눈에게 찾아달라고 하기").performClick()
        composeRule.onNodeWithText("자동 안내 받기").performClick()

        composeRule.onNodeWithText("노란 테두리를 확인한 뒤 직접 선택해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 5 단계").assertIsDisplayed()
    }

    @Test
    fun threeNoHelpJourneysAndSemanticABLayoutsHaveOwnedOfflineContracts() {
        ServiceId.entries.forEach { serviceId ->
            val service = ServiceCatalog.require(serviceId)
            assertEquals(5, service.steps.size)
            assertNotNull(PracticeUrlPolicy.parseNavigation(PracticeUrlPolicy.pageUrl(serviceId, PracticeLayout.A)))
            assertNotNull(PracticeUrlPolicy.parseNavigation(PracticeUrlPolicy.pageUrl(serviceId, PracticeLayout.B)))
        }
    }

    private fun countWebViews(view: View): Int =
        when (view) {
            is WebView -> 1
            is ViewGroup -> (0 until view.childCount).sumOf { countWebViews(view.getChildAt(it)) }
            else -> 0
        }

    private fun findWebView(view: View): WebView? =
        when (view) {
            is WebView -> view
            is ViewGroup ->
                (0 until view.childCount)
                    .firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
            else -> null
        }

    private fun assertSingleWebView() {
        var count = 0
        composeRule.runOnIdle {
            count = countWebViews(composeRule.activity.window.decorView)
        }
        assertEquals(1, count)
    }

    private fun evaluateJavascript(script: String): String {
        var webView: WebView? = null
        composeRule.runOnIdle {
            webView = findWebView(composeRule.activity.window.decorView)
        }
        val result = arrayOfNulls<String>(1)
        val completed = CountDownLatch(1)
        composeRule.activity.runOnUiThread {
            requireNotNull(webView).evaluateJavascript(script) {
                result[0] = it
                completed.countDown()
            }
        }
        assertTrue("WebView JavaScript evaluation timed out", completed.await(5, TimeUnit.SECONDS))
        return result[0].orEmpty().removeSurrounding("\"")
    }

    private fun waitForJavascript(
        expected: String,
        script: String,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var actual = ""
        while (System.nanoTime() < deadline) {
            actual = evaluateJavascript(script)
            if (actual == expected) return
            Thread.sleep(50)
        }
        assertEquals(expected, actual)
    }
}
