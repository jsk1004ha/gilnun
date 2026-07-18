package com.gilnun.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.web.PracticeLayout
import com.gilnun.app.web.PracticeUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        composeRule.onNodeWithText("기초연금 신청 연습").assertIsDisplayed()
        composeRule.onNodeWithText("주민등록표 등본 발급 연습").assertIsDisplayed()
        composeRule.onNodeWithText("건강검진 대상 조회 연습").assertIsDisplayed()
        composeRule.onNodeWithText("Demo Reset").assertDoesNotExist()
        composeRule.onNodeWithText("레이아웃 A").assertDoesNotExist()
        composeRule.onNodeWithText("PatchV1 준비").assertDoesNotExist()
    }

    @Test
    fun directHelpOffersExactAutomaticHelperAndDeclineChoices() {
        composeRule.onNodeWithText("기초연금 신청 연습").performClick()
        composeRule.onNodeWithText("도움이 필요해요").performClick()

        composeRule.onNodeWithText("자동 안내 받기").assertIsDisplayed()
        composeRule.onNodeWithText("가족·도우미에게 넘기기").assertIsDisplayed()
        composeRule.onNodeWithText("지금은 괜찮아요").assertIsDisplayed().performClick()
    }

    @Test
    fun helperHandoffIsNativeAndNamesOnlyCurrentAllowedTarget() {
        composeRule.onNodeWithText("주민등록표 등본 발급 연습").performClick()
        composeRule.onNodeWithText("도움이 필요해요").performClick()
        composeRule.onNodeWithText("가족·도우미에게 넘기기").performClick()

        composeRule.onNodeWithText("가족·도우미 확인").assertIsDisplayed()
        composeRule.onNodeWithText("주민등록표 등본").assertIsDisplayed()
        composeRule.onNodeWithText("이곳을 안내해 주세요").assertIsDisplayed()
    }

    @Test
    fun automaticGuidanceHighlightsWithoutAdvancingOrClicking() {
        composeRule.onNodeWithText("건강검진 대상 조회 연습").performClick()
        composeRule.onNodeWithText("도움이 필요해요").performClick()
        composeRule.onNodeWithText("자동 안내 받기").performClick()

        composeRule.onNodeWithText("노란 테두리를 확인한 뒤 직접 선택해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 3 단계").assertIsDisplayed()
    }

    @Test
    fun threeNoHelpJourneysAndSemanticABLayoutsHaveOwnedOfflineContracts() {
        ServiceId.entries.forEach { serviceId ->
            val service = ServiceCatalog.require(serviceId)
            assertEquals(3, service.steps.size)
            assertNotNull(PracticeUrlPolicy.parseNavigation(PracticeUrlPolicy.pageUrl(serviceId, PracticeLayout.A)))
            assertNotNull(PracticeUrlPolicy.parseNavigation(PracticeUrlPolicy.pageUrl(serviceId, PracticeLayout.B)))
        }
    }
}
