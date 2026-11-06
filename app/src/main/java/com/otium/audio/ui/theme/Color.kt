package com.otium.audio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — dark glass.
// Only DarkSurface is opaque. Everything else is translucent on purpose, so panels read as
// frosted glass over the ambient backdrop instead of as flat rectangles.

// Canvas
val DarkSurface          = Color(0xFF06060A)
val DarkSurfaceVariant   = Color(0x9E15151F)
val DarkSurfaceContainer = Color(0xC414141D)
val DarkSurfaceElevated  = Color(0xE6101017)
val DarkCardBorder       = Color(0x24FFFFFF)
val GlassSheen           = Color(0x14FFFFFF)
val GlassScrim           = Color(0x99000000)

// Accents
val AccentGold           = Color(0xFFE9C37C)
val AccentGoldLight      = Color(0xFFFFDFA6)
val AccentGoldMuted      = Color(0x29E9C37C)
val AccentGoldDim        = Color(0xFF8A6A38)

val AccentCyan           = Color(0xFF6FE3E9)
val AccentCyanGlow       = Color(0x336FE3E9)

// Text
val TextPrimary          = Color(0xFFF6F6F8)
val TextSecondary        = Color(0xFFA6A6B3)
val TextTertiary         = Color(0xFF6C6C7A)

// Semantic
val ErrorRed             = Color(0xFFFF6B6B)
val SuccessGreen         = Color(0xFF4ADE9B)

/** The backdrop the translucent panels sit on. */
val AmbientBackdrop: Brush = Brush.verticalGradient(
    0.0f to Color(0xFF0C0C15),
    0.35f to Color(0xFF090A0F),
    1.0f to DarkSurface
)
