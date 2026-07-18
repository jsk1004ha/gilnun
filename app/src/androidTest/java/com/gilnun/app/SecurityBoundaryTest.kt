package com.gilnun.app

import android.content.Context
import android.content.pm.PackageManager
import android.security.NetworkSecurityPolicy
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityBoundaryTest {
    @Suppress("DEPRECATION")
    @Test
    fun packagedAppDeclaresNoNetworkOverlayCaptureOrAccessibilityBoundary() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES,
            )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(
            "Forbidden permissions found: ${requested.intersect(FORBIDDEN_PERMISSIONS)}",
            requested.intersect(FORBIDDEN_PERMISSIONS).isEmpty(),
        )
        assertTrue(
            "Accessibility services are outside the MVP boundary",
            packageInfo.services.orEmpty().none {
                it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE"
            },
        )
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted)
    }

    private companion object {
        val FORBIDDEN_PERMISSIONS =
            setOf(
                "android.permission.INTERNET",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
            )
    }
}
