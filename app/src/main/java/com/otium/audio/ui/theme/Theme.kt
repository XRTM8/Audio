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
    primary = Color(0xFF101217),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF101217),
    surface = Color(0xFFF7F8FA),
    onSurface = Color(0xFF101217),
    surfaceVariant = Color(0xFFEBEFF3),
    onSurfaceVariant = Color(0xFF5A6170),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF101217),
    outline = Color(0xFFE2E6EC),
    outlineVariant = Color(0xFFE2E6EC),
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
