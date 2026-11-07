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

// Default Fallback: Pure Titanium White
val AccentFallback = Color(0xFFFFFFFF)

/**
 * Clamps a palette colour into an elegant, legible range that stays crystal-clear on an OLED black surface.
 * Greyscale, dark, or muted artwork gracefully defaults to Titanium White.
 */
fun Color.asReadableAccent(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    val saturation = hsl[1]
    val lightness = hsl[2]

    if (lightness < 0.18f || saturation < 0.12f) return AccentFallback

    hsl[1] = saturation.coerceIn(0.50f, 0.85f)
    hsl[2] = lightness.coerceIn(0.60f, 0.80f)
    return Color(ColorUtils.HSLToColor(hsl))
}
