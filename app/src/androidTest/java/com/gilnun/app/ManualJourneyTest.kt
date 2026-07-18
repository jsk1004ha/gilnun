package com.gilnun.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shellExposesOfflineDemoControls() {
        composeRule.onNodeWithText("길눈 AI").assertIsDisplayed()
        composeRule.onNodeWithText("학습자").assertIsDisplayed()
        composeRule.onNodeWithText("레이아웃 A").assertIsDisplayed()
        composeRule.onNodeWithText("직접 도움 요청").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Demo Reset").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun directHelpRequiresConsentAndResetReturnsToLearner() {
        composeRule.onNodeWithText("Demo Reset").performScrollTo().performClick()
        composeRule.onNodeWithText("직접 도움 요청").performScrollTo().performClick()

        composeRule.onNodeWithText("도움이 필요하신가요?").assertIsDisplayed()
        composeRule.onNodeWithText("도움 받기").assertIsDisplayed()
        composeRule.onNodeWithText("지금은 괜찮아요").performClick()

        composeRule.onNodeWithText("Demo Reset").performScrollTo().performClick()
        composeRule
            .onNodeWithText("Demo Reset 완료: 저장 상태와 화면 상태를 모두 지웠습니다.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("학습자").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("레이아웃 A").performScrollTo().assertIsDisplayed()
    }
}
