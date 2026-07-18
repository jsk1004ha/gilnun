package com.gilnun.app.web

import com.gilnun.app.catalog.ServiceId
import java.net.URI

enum class PracticeLayout {
    A,
    B,
}

data class PracticePageRequest(
    val serviceId: ServiceId,
    val layout: PracticeLayout,
)

/**
 * Exact allowlist for APK-owned practice content.
 *
 * No URL decoding or normalization is used when making a trust decision. Encoded paths, encoded
 * query keys or values, extra parameters, duplicate parameters, fragments, ports, and user-info
 * therefore fail closed.
 */
object PracticeUrlPolicy {
    const val APP_ORIGIN = "https://appassets.androidplatform.net"
    const val INDEX_URL = "$APP_ORIGIN/assets/welfare/index.html"

    private val staticResources =
        setOf(
            "/assets/welfare/style.css",
            "/assets/welfare/app.js",
        )

    fun pageUrl(
        serviceId: ServiceId,
        layout: PracticeLayout,
    ): String = "$INDEX_URL?service=${serviceId.persistedKey}&layout=${layout.name}"

    fun parseNavigation(url: String): PracticePageRequest? {
        val uri = exactUri(url) ?: return null
        if (uri.rawPath != INDEX_PATH || uri.rawFragment != null) return null
        val query = uri.rawQuery ?: return null
        if ('%' in query || '+' in query) return null

        val parts = query.split('&')
        if (parts.size != 2 || parts.any(String::isEmpty)) return null
        val values = linkedMapOf<String, String>()
        parts.forEach { part ->
            val separator = part.indexOf('=')
            if (separator <= 0 || separator != part.lastIndexOf('=')) return null
            val key = part.substring(0, separator)
            val value = part.substring(separator + 1)
            if (value.isEmpty() || values.put(key, value) != null) return null
        }
        if (values.keys != REQUIRED_QUERY_KEYS) return null

        val serviceId = ServiceId.fromPersistedKey(values["service"]) ?: return null
        val layout =
            runCatching { PracticeLayout.valueOf(values.getValue("layout")) }
                .getOrNull()
                ?: return null
        return PracticePageRequest(serviceId, layout)
    }

    fun isAllowedResource(url: String): Boolean {
        parseNavigation(url)?.let { return true }
        val uri = exactUri(url) ?: return false
        return uri.rawPath in staticResources &&
            uri.rawQuery == null &&
            uri.rawFragment == null
    }

    fun isExpectedNavigation(
        url: String,
        expected: PracticePageRequest,
    ): Boolean = parseNavigation(url) == expected

    private fun exactUri(url: String): URI? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != "https" ||
            uri.rawAuthority != APP_HOST ||
            uri.host != APP_HOST ||
            uri.port != -1 ||
            uri.rawUserInfo != null ||
            uri.rawPath == null ||
            '%' in uri.rawPath
        ) {
            return null
        }
        return uri
    }

    private const val APP_HOST = "appassets.androidplatform.net"
    private const val INDEX_PATH = "/assets/welfare/index.html"
    private val REQUIRED_QUERY_KEYS = setOf("service", "layout")
}
