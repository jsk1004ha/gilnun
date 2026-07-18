package com.gilnun.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.gilnun.app.BuildConfig
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.data.PatchV1
import java.io.ByteArrayInputStream
import org.json.JSONObject

sealed interface BridgeStatus {
    data object Available : BridgeStatus

    data object Unavailable : BridgeStatus

    data object PageReady : BridgeStatus

    data class Rejected(
        val error: BridgeError,
    ) : BridgeStatus

    data class PageFailed(
        val code: String = "LOCAL_LOAD_FAILED",
    ) : BridgeStatus
}

sealed interface WebCommand {
    val requestId: Long

    data class Highlight(
        val patch: PatchV1,
        override val requestId: Long = SystemClock.elapsedRealtimeNanos(),
    ) : WebCommand

    data class ClearHighlight(
        override val requestId: Long = SystemClock.elapsedRealtimeNanos(),
    ) : WebCommand

    data class Reset(
        val serviceId: ServiceId,
        val layout: PracticeLayout = PracticeLayout.A,
        override val requestId: Long = SystemClock.elapsedRealtimeNanos(),
    ) : WebCommand
}

/** Hosts only the three APK-owned, offline practice journeys. */
@Composable
fun DemoWebView(
    serviceId: ServiceId,
    layout: PracticeLayout,
    modifier: Modifier = Modifier,
    command: WebCommand? = null,
    onEvent: (BridgeEventV2) -> Unit,
    onBridgeStatus: (BridgeStatus) -> Unit = {},
    onSecurityEvent: (String) -> Unit = {},
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentOnBridgeStatus by rememberUpdatedState(onBridgeStatus)
    val currentOnSecurityEvent by rememberUpdatedState(onSecurityEvent)
    val textZoom =
        (LocalConfiguration.current.fontScale * 100)
            .toInt()
            .coerceIn(MIN_TEXT_ZOOM, MAX_TEXT_ZOOM)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SecureGilnunWebView(
                context = context,
                initialService = serviceId,
                initialLayout = layout.allowedForBuild(),
                onEvent = { currentOnEvent(it) },
                onBridgeStatus = { currentOnBridgeStatus(it) },
                onSecurityEvent = { currentOnSecurityEvent(it) },
            ).also { it.updateTextZoom(textZoom) }
        },
        update = { webView ->
            webView.requestPage(serviceId, layout.allowedForBuild())
            webView.updateTextZoom(textZoom)
            command?.let(webView::dispatch)
        },
        onRelease = SecureGilnunWebView::disposeSecurely,
    )
}

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature", "ViewConstructor")
private class SecureGilnunWebView(
    context: Context,
    initialService: ServiceId,
    initialLayout: PracticeLayout,
    private val onEvent: (BridgeEventV2) -> Unit,
    private val onBridgeStatus: (BridgeStatus) -> Unit,
    private val onSecurityEvent: (String) -> Unit,
) : WebView(context) {
    private val assetLoader =
        WebViewAssetLoader
            .Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    private val cookieManager = CookieManager.getInstance()
    private var desiredRequest = PracticePageRequest(initialService, initialLayout)
    private var cookiesCleared = false
    private var pageReady = false
    private var disposed = false
    private var lastCommandId: Long? = null
    private var activeHighlightPayload: String? = null
    private var pendingHighlightPayload: String? = null

    init {
        configureSecureSettings()
        installLocalClient()
        installMessageBridge()
        clearPrivateStateThenLoad()
    }

    fun requestPage(
        serviceId: ServiceId,
        layout: PracticeLayout,
    ) {
        val request = PracticePageRequest(serviceId, layout)
        if (request == desiredRequest) return
        desiredRequest = request
        activeHighlightPayload = null
        pendingHighlightPayload = null
        if (cookiesCleared && !disposed) loadDesiredPage()
    }

    fun updateTextZoom(value: Int) {
        settings.textZoom = value.coerceIn(MIN_TEXT_ZOOM, MAX_TEXT_ZOOM)
    }

    fun dispatch(command: WebCommand) {
        if (lastCommandId == command.requestId || disposed) return
        lastCommandId = command.requestId
        when (command) {
            is WebCommand.Highlight -> {
                activeHighlightPayload = command.patch.toHighlightPayload()
                sendOrQueue(checkNotNull(activeHighlightPayload))
            }

            is WebCommand.ClearHighlight -> {
                activeHighlightPayload = null
                pendingHighlightPayload = null
                sendIfReady(clearHighlightPayload())
            }

            is WebCommand.Reset -> {
                desiredRequest =
                    PracticePageRequest(
                        command.serviceId,
                        command.layout.allowedForBuild(),
                    )
                activeHighlightPayload = null
                pendingHighlightPayload = null
                clearPrivateStateThenLoad()
            }
        }
    }

    fun disposeSecurely() {
        if (disposed) return
        disposed = true
        pageReady = false
        activeHighlightPayload = null
        pendingHighlightPayload = null
        stopLoading()
        clearTransientState()
        cookieManager.removeAllCookies { cookieManager.flush() }
        removeAllViews()
        destroy()
    }

    private fun configureSecureSettings() {
        setBackgroundColor(Color.TRANSPARENT)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        isSaveEnabled = false
        setDownloadListener { _, _, _, _, _ -> onSecurityEvent("BLOCKED_DOWNLOAD") }
        WebView.setWebContentsDebuggingEnabled(false)

        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = false
            databaseEnabled = false
            saveFormData = false
            setGeolocationEnabled(false)
            mediaPlaybackRequiresUserGesture = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false

            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
        }

        cookieManager.setAcceptCookie(false)
        cookieManager.setAcceptThirdPartyCookies(this, false)
    }

    private fun installLocalClient() {
        webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val allowed =
                        request.isForMainFrame &&
                            PracticeUrlPolicy.isExpectedNavigation(
                                request.url.toString(),
                                desiredRequest,
                            )
                    if (!allowed) onSecurityEvent("BLOCKED_NAVIGATION")
                    return !allowed
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse {
                    val url = request.url.toString()
                    val parsedPage = PracticeUrlPolicy.parseNavigation(url)
                    val allowed =
                        if (parsedPage != null) {
                            parsedPage == desiredRequest
                        } else {
                            PracticeUrlPolicy.isAllowedResource(url)
                        }
                    if (!allowed) {
                        return blockedResponse()
                    }
                    return assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) {
                    if (PracticeUrlPolicy.isExpectedNavigation(url, desiredRequest)) {
                        pageReady = true
                        pendingHighlightPayload?.let {
                            pendingHighlightPayload = null
                            postExactOriginMessage(it)
                        }
                        onBridgeStatus(BridgeStatus.PageReady)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        pageReady = false
                        onBridgeStatus(BridgeStatus.PageFailed())
                    }
                }
            }
    }

    private fun installMessageBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            onBridgeStatus(BridgeStatus.Unavailable)
            return
        }
        WebViewCompat.addWebMessageListener(
            this,
            GilnunBridge.JS_OBJECT_NAME,
            setOf(GilnunBridge.APP_ORIGIN),
        ) { _, message, sourceOrigin, isMainFrame, _ ->
            val payload =
                if (message.type == WebMessageCompat.TYPE_STRING) {
                    message.data
                } else {
                    null
                }
            when (val result = GilnunBridge.accept(payload, sourceOrigin, isMainFrame)) {
                is BridgeResult.Accepted -> onEvent(result.event)
                is BridgeResult.Rejected -> onBridgeStatus(BridgeStatus.Rejected(result.error))
            }
        }
        onBridgeStatus(BridgeStatus.Available)
    }

    private fun clearPrivateStateThenLoad() {
        pageReady = false
        cookiesCleared = false
        clearTransientState()
        cookieManager.setAcceptCookie(false)
        cookieManager.setAcceptThirdPartyCookies(this, false)
        cookieManager.removeAllCookies {
            cookieManager.flush()
            post {
                if (!disposed) {
                    cookiesCleared = true
                    loadDesiredPage()
                }
            }
        }
    }

    private fun clearTransientState() {
        stopLoading()
        clearFormData()
        clearCache(true)
        clearHistory()
        clearSslPreferences()
        WebStorage.getInstance().deleteAllData()
    }

    private fun loadDesiredPage() {
        pageReady = false
        pendingHighlightPayload = activeHighlightPayload
        loadUrl(PracticeUrlPolicy.pageUrl(desiredRequest.serviceId, desiredRequest.layout))
    }

    private fun sendOrQueue(payload: String) {
        if (pageReady) postExactOriginMessage(payload) else pendingHighlightPayload = payload
    }

    private fun sendIfReady(payload: String) {
        if (pageReady) postExactOriginMessage(payload)
    }

    private fun postExactOriginMessage(payload: String) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            onBridgeStatus(BridgeStatus.Unavailable)
            return
        }
        WebViewCompat.postWebMessage(
            this,
            WebMessageCompat(payload),
            APP_ORIGIN_URI,
        )
    }

    private fun PatchV1.toHighlightPayload(): String =
        JSONObject()
            .put("schemaVersion", PATCH_SCHEMA_VERSION)
            .put("command", "HIGHLIGHT")
            .put("pageId", pageId)
            .put("compatibleRevision", compatibleRevision)
            .put("stableKey", stableKey)
            .put("role", role)
            .put("accessibleName", accessibleName)
            .put("expectedState", expectedState)
            .toString()

    private fun clearHighlightPayload(): String =
        JSONObject()
            .put("schemaVersion", PATCH_SCHEMA_VERSION)
            .put("command", "CLEAR_HIGHLIGHT")
            .toString()

    private fun blockedResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked",
            mapOf(
                "Cache-Control" to "no-store",
                "Content-Security-Policy" to "default-src 'none'",
            ),
            ByteArrayInputStream(ByteArray(0)),
        )

    companion object {
        private val APP_ORIGIN_URI: Uri = Uri.parse(GilnunBridge.APP_ORIGIN)
    }
}

private fun PracticeLayout.allowedForBuild(): PracticeLayout =
    if (BuildConfig.DEBUG) this else PracticeLayout.A

private const val MIN_TEXT_ZOOM = 100
private const val MAX_TEXT_ZOOM = 200
internal const val PATCH_SCHEMA_VERSION = 1
