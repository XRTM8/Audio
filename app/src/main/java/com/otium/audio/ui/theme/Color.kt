package com.otium.audio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — AMOLED glass.
// The canvas is true black so an OLED panel switches those pixels off. Panels sit a few
// steps above it and keep a translucent edge, which is what reads as glass on a black
// background: the light comes from the hairline, not from the fill.

// Canvas
val DarkSurface          = Color(0xFF000000)
val DarkSurfaceVariant   = Color(0xD9141419)
val DarkSurfaceContainer = Color(0xE61A1A21)
val DarkSurfaceElevated  = Color(0xD216161C)
val DarkCardBorder       = Color(0x24FFFFFF)
val GlassSheen           = Color(0x1AFFFFFF)
val GlassScrim           = Color(0xCC000000)

// Accents — Audiophile Crimson Ruby (vibrant, elegant contrast on AMOLED true black)
val AccentCrimson        = Color(0xFFFF2A55)
val AccentCrimsonLight   = Color(0xFFFF5E7E)
val AccentCrimsonMuted   = Color(0x33FF2A55)
val AccentCrimsonDim     = Color(0xFF8A1830)

// Main Theme Accent — Audiophile Crimson
val AccentGold           = AccentCrimson
val AccentGoldLight      = AccentCrimsonLight
val AccentGoldMuted      = AccentCrimsonMuted
val AccentGoldDim        = AccentCrimsonDim

val AccentCyan           = Color(0xFFA9B6BC)
val AccentCyanGlow       = Color(0x33A9B6BC)

// Text
val TextPrimary          = Color(0xFFF7F7F9)
val TextSecondary        = Color(0xFF9C9CA8)
val TextTertiary         = Color(0xFF66666F)

// Semantic
val ErrorRed             = Color(0xFFFF6F6F)
val SuccessGreen         = Color(0xFF57D9A3)

/** Barely-there backdrop: enough gradient for glass to read, dark enough for OLED. */
val AmbientBackdrop: Brush = Brush.verticalGradient(
    0.0f to Color(0xFF0B0B12),
    0.42f to Color(0xFF050508),
    1.0f to DarkSurface
)
