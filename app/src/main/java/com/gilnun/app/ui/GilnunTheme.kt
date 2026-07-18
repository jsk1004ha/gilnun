package com.gilnun.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GilnunNavy = Color(0xFF0B2838)
val GilnunTeal = Color(0xFF147D84)
val GilnunYellow = Color(0xFFF2C94C)
val GilnunBackground = Color(0xFFF7FAFC)
val GilnunError = Color(0xFFD75A5A)

private val GilnunColors =
    lightColorScheme(
        primary = GilnunTeal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9F1F1),
        onPrimaryContainer = GilnunNavy,
        secondary = GilnunNavy,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE4EDF1),
        onSecondaryContainer = GilnunNavy,
        tertiary = Color(0xFF735D00),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFF0B5),
        onTertiaryContainer = Color(0xFF2A2200),
        error = GilnunError,
        background = GilnunBackground,
        onBackground = GilnunNavy,
        surface = Color.White,
        onSurface = GilnunNavy,
        outline = Color(0xFF52656F),
    )

private val GilnunTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontSize = 30.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 28.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            ),
        titleMedium =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Bold,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 31.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 31.sp,
            ),
        labelLarge =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            ),
    )

@Composable
fun GilnunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GilnunColors,
        typography = GilnunTypography,
        content = content,
    )
}
