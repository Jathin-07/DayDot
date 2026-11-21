package com.daydot.lockscreen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DayDotDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF0E0E10),
    secondary = Color(0xFFD98282),
    onSecondary = Color(0xFF1A0E0E),
    tertiary = Color(0xFF777777),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF161619),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF202024),
    onSurfaceVariant = Color(0xFFA0A0A8),
    outline = Color(0xFF2E2E34),
    error = Color(0xFFE57373),
    onError = Color(0xFF1F0A0A)
)

@Composable
fun DayDotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DayDotDarkColorScheme,
        content = content
    )
}
