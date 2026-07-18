package com.gilnun.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GilnunColors =
    lightColorScheme(
        primary = Color(0xFF0B6B5B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD5EEE7),
        onPrimaryContainer = Color(0xFF063F35),
        secondary = Color(0xFF49645D),
        secondaryContainer = Color(0xFFDCE7E3),
        tertiary = Color(0xFFD97706),
        tertiaryContainer = Color(0xFFFFE2B8),
        error = Color(0xFF9F3A38),
        background = Color(0xFFF7F3EA),
        onBackground = Color(0xFF172A3A),
        surface = Color(0xFFFFFBF4),
        onSurface = Color(0xFF172A3A),
        outline = Color(0xFF718079),
    )

@Composable
fun GilnunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GilnunColors,
        content = content,
    )
}
