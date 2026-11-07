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
 *
 * It is a state object rather than a CompositionLocal because it is updated from the
 * activity and read from anywhere below it without threading it through every signature.
 */
object OtiumAccent {
    var color: Color by mutableStateOf(AccentFallback)
        internal set
}

val AccentFallback = Color(0xFFFF2A55)

/**
 * Clamps a palette colour into a range that stays legible on an OLED black surface.
 * Greyscale or nearly black artwork falls back to the neutral accent instead of producing
 * a muddy highlight.
 */
fun Color.asReadableAccent(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    val lightness = hsl[2]
    val saturation = hsl[1]

    if (lightness < 0.18f || saturation < 0.10f) return AccentFallback

    hsl[1] = saturation.coerceIn(0.42f, 0.68f)
    hsl[2] = lightness.coerceIn(0.62f, 0.74f)
    return Color(ColorUtils.HSLToColor(hsl))
}
