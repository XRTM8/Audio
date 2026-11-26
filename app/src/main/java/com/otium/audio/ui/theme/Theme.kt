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
    primary = BurgundyAccent,
    onPrimary = PureWhite,
    secondary = BurgundyWarm,
    onSecondary = PureWhite,
    primaryContainer = StudioSurface,
    onPrimaryContainer = PureWhite,
    surface = AmoledBlack,
    onSurface = PureWhite,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = MutedWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
    surfaceTint = Color.Transparent,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BurgundyDeep,
    onPrimary = AmoledWhite,
    secondary = BurgundyWarm,
    onSecondary = AmoledWhite,
    primaryContainer = WhiteSurface,
    onPrimaryContainer = DarkText,
    surface = AmoledWhite,
    onSurface = DarkText,
    surfaceVariant = WhiteSurfaceElevated,
    onSurfaceVariant = DarkTextMuted,
    background = AmoledWhite,
    onBackground = DarkText,
    outline = WhiteBorder,
    outlineVariant = WhiteBorderSubtle,
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
