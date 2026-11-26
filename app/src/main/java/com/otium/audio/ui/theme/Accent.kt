package com.otium.audio.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * The single moving colour of the interface. The chrome is pure titanium and obsidian;
 * this value is derived from the current artwork and used subtly for active states.
 */
object OtiumAccent {
    var color: Color by mutableStateOf(AccentFallback)
        internal set
}

// Default Fallbacks (Athar Velvet Burgundy)
val AccentFallbackDark = BurgundyAccent
val AccentFallbackLight = BurgundyDeep
val AccentFallback = AccentFallbackDark

/**
 * Clamps a palette colour into an elegant, legible range calibrated for contrast.
 * In Dark Mode: calibrated for crystal-clear readability on AMOLED black surfaces.
 * In Light Mode: calibrated with crisp contrast on studio porcelain / white surfaces.
 */
fun Color.asReadableAccent(isDark: Boolean = true): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    val saturation = hsl[1]
    val lightness = hsl[2]

    if (lightness < 0.12f || lightness > 0.92f || saturation < 0.10f) {
        return if (isDark) AccentFallbackDark else AccentFallbackLight
    }

    hsl[1] = saturation.coerceIn(0.45f, 0.90f)
    hsl[2] = if (isDark) {
        lightness.coerceIn(0.60f, 0.82f)
    } else {
        lightness.coerceIn(0.25f, 0.45f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}
