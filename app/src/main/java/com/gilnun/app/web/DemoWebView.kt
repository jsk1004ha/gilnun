package com.gilnun.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebMessage
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.gilnun.app.data.InteractionEvent
import com.gilnun.app.data.PatchV1
import java.io.ByteArrayInputStream
import org.json.JSONObject

sealed interface BridgeStatus {
    data object Available : BridgeStatus

    data object Unavailable : BridgeStatus

    data object PageReady : BridgeStatus

    data class Rejected(val error: BridgeError) : BridgeStatus

    data class PageFailed(val code: String = "LOCAL_LOAD_FAILED") : BridgeStatus
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
        val layoutVariant: String = "A",
        override val requestId: Long = SystemClock.elapsedRealtimeNanos(),
    ) : WebCommand
}

/**
 * Hosts only the APK-owned welfare fixture.
 *
 * The first load waits for the cookie removal callback. Runtime storage, network access, external
 * navigation, file/content access, cache, form retention, and autofill are disabled. The bridge is
 * a WebMessage listener scoped to the exact appassets origin.
 */
@Composable
fun DemoWebView(
    layoutVariant: String,
    modifier: Modifier = Modifier,
    command: WebCommand? = null,
    onEvent: (InteractionEvent) -> Unit,
    onBridgeStatus: (BridgeStatus) -> Unit = {},
    onSecurityEvent: (String) -> Unit = {},
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentOnBridgeStatus by rememberUpdatedState(onBridgeStatus)
    val currentOnSecurityEvent by rememberUpdatedState(onSecurityEvent)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SecureGilnunWebView(
                context = context,
                initialLayout = layoutVariant,
                onEvent = { currentOnEvent(it) },
                onBridgeStatus = { currentOnBridgeStatus(it) },
                onSecurityEvent = { currentOnSecurityEvent(it) },
            )
        },
        update = { webView ->
            webView.requestLayoutVariant(layoutVariant)
            command?.let(webView::dispatch)
        },
        onRelease = { webView ->
            webView.disposeSecurely()
        },
    )
}

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature", "ViewConstructor")
private class SecureGilnunWebView(
    context: Context,
    initialLayout: String,
    private val onEvent: (InteractionEvent) -> Unit,
    private val onBridgeStatus: (BridgeStatus) -> Unit,
    private val onSecurityEvent: (String) -> Unit,
) : WebView(context) {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    private val cookieManager = CookieManager.getInstance()
    private var desiredLayout = normalizeLayout(initialLayout)
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

    fun requestLayoutVariant(layoutVariant: String) {
        val normalized = normalizeLayout(layoutVariant)
        if (normalized == desiredLayout) return
        desiredLayout = normalized
        if (cookiesCleared && !disposed) loadDesiredPage()
    }

    fun dispatch(command: WebCommand) {
        if (lastCommandId == command.requestId || disposed) return
        lastCommandId = command.requestId
        when (command) {
            is WebCommand.Highlight -> {
                activeHighlightPayload = command.patch.toHighlightPayload()
                sendOrQueue(activeHighlightPayload.orEmpty())
            }

            is WebCommand.ClearHighlight -> {
                activeHighlightPayload = null
                pendingHighlightPayload = null
                sendIfReady(clearHighlightPayload())
            }

            is WebCommand.Reset -> {
                desiredLayout = normalizeLayout(command.layoutVariant)
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
        setDownloadListener { _, _, _, _, _ ->
            onSecurityEvent("BLOCKED_DOWNLOAD")
        }
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
            textZoom = (resources.configuration.fontScale * 100).toInt().coerceIn(100, 200)

            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
        }

        cookieManager.setAcceptCookie(false)
        cookieManager.setAcceptThirdPartyCookies(this, false)
    }

    private fun installLocalClient() {
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val allowed = request.isForMainFrame && request.url.isAllowedLocalNavigation()
                if (!allowed) onSecurityEvent("BLOCKED_NAVIGATION")
                return !allowed
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse {
                if (!request.url.isAllowedLocalResource()) {
                    return blockedResponse()
                }
                return assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (Uri.parse(url).isAllowedLocalNavigation()) {
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
            val payload = if (message.type == WebMessageCompat.TYPE_STRING) {
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
        loadUrl("$LOCAL_INDEX?layout=$desiredLayout")
    }

    private fun sendOrQueue(payload: String) {
        if (pageReady) {
            postExactOriginMessage(payload)
        } else {
            pendingHighlightPayload = payload
        }
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
            .put("schemaVersion", GilnunBridge.SCHEMA_VERSION)
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
            .put("schemaVersion", GilnunBridge.SCHEMA_VERSION)
            .put("command", "CLEAR_HIGHLIGHT")
            .toString()

    private fun Uri.isAllowedLocalNavigation(): Boolean =
        isExactAppassetsUri() && path == "/assets/welfare/index.html"

    private fun Uri.isAllowedLocalResource(): Boolean =
        isExactAppassetsUri() && path in ALLOWED_RESOURCE_PATHS

    private fun Uri.isExactAppassetsUri(): Boolean =
        scheme == "https" &&
            host == "appassets.androidplatform.net" &&
            port == -1 &&
            userInfo == null

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
        private const val LOCAL_INDEX =
            "https://appassets.androidplatform.net/assets/welfare/index.html"
        private val APP_ORIGIN_URI: Uri = Uri.parse(GilnunBridge.APP_ORIGIN)
        private val ALLOWED_RESOURCE_PATHS = setOf(
            "/assets/welfare/index.html",
            "/assets/welfare/style.css",
            "/assets/welfare/app.js",
        )

        private fun normalizeLayout(value: String): String =
            if (value.equals("B", ignoreCase = true)) "B" else "A"
    }
}
