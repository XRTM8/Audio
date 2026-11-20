package com.otium.audio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF131318),
    onPrimaryContainer = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF111116),
    onSurfaceVariant = Color(0xFF9898A0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    surfaceTint = Color.Transparent,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F1115),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F4F8),
    onPrimaryContainer = Color(0xFF0F1115),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF0F1115),
    surfaceVariant = Color(0xFFEDF0F5),
    onSurfaceVariant = Color(0xFF5B6271),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF0F1115),
    outline = Color(0xFFE2E6EE),
    outlineVariant = Color(0xFFE2E6EE),
    surfaceTint = Color.Transparent,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun OtiumAudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkOtiumColors else LightOtiumColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalOtiumColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
