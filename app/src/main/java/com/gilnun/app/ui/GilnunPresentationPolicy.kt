package com.gilnun.app.ui

import com.gilnun.app.web.BridgeStatus

internal object GilnunPresentationPolicy {
    fun startupDelayMs(animationsEnabled: Boolean): Long =
        if (animationsEnabled) STARTUP_DURATION_MS else 0L

    fun dismissLoading(status: BridgeStatus): Boolean =
        status == BridgeStatus.PageReady || status is BridgeStatus.PageFailed

    private const val STARTUP_DURATION_MS = 720L
}
