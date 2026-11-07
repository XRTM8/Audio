package com.otium.audio.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * The single moving colour of the interface. The chrome is neutral; this value is derived
 * from the current artwork and used only for things that mean "this is playing": progress,
 * the play control, active states.
 */
object OtiumAccent {
    var color: Color by mutableStateOf(AccentFallback)
        internal set
}

// Default Fallback: Blazing Vivid Power Red
val AccentFallback = Color(0xFFFF0033)

/**
 * Clamps a palette colour into an intense, punchy range that stays vibrant on an OLED black surface.
 * Strictly eliminates any yellow or gold tones, shifting them to bold Power Red.
 */
fun Color.asReadableAccent(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    val hue = hsl[0]
    val saturation = hsl[1]
    val lightness = hsl[2]

    if (lightness < 0.15f || saturation < 0.10f) return AccentFallback

    // Strict filter: banish yellow, amber, and gold (hue 24° to 72°)
    // Replace with intense pure vivid red
    if (hue in 24f..72f) {
        hsl[0] = 352f // Pure Power Red
    }

    hsl[1] = saturation.coerceIn(0.70f, 1.0f) // Maximum punchy saturation
    hsl[2] = lightness.coerceIn(0.48f, 0.65f)  // High-contrast legible luminosity
    return Color(ColorUtils.HSLToColor(hsl))
}
