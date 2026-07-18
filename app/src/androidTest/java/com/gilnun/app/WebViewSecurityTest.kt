package com.gilnun.app

import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebViewSecurityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Suppress("DEPRECATION")
    @Test
    fun embeddedFixtureKeepsStorageNetworkAndFileAccessClosed() {
        composeRule.onNodeWithText("기초연금 신청 연습").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            val webView =
                composeRule.activity
                    .findViewById<View>(android.R.id.content)
                    .findWebView()
            assertNotNull("The owned fixture WebView must be hosted", webView)

            requireNotNull(webView).apply {
                assertTrue(settings.javaScriptEnabled)
                assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
                assertFalse(settings.allowFileAccess)
                assertFalse(settings.allowContentAccess)
                assertTrue(settings.blockNetworkLoads)
                assertFalse(settings.domStorageEnabled)
                assertFalse(settings.databaseEnabled)
                assertFalse(settings.saveFormData)
                assertFalse(settings.allowFileAccessFromFileURLs)
                assertFalse(settings.allowUniversalAccessFromFileURLs)
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, settings.mixedContentMode)
                assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS, importantForAutofill)
                assertFalse(CookieManager.getInstance().acceptCookie())
                assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(this))
            }
        }
    }
}

private fun View.findWebView(): WebView? {
    if (this is WebView) return this
    if (this !is ViewGroup) return null

    for (index in 0 until childCount) {
        getChildAt(index).findWebView()?.let { return it }
    }
    return null
}
