package com.otium.audio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — Pure AMOLED Black Canvas with Translucent Frosted Dark Glass.
// True black switches OLED pixels off completely.
// Panels and sheets float above it with translucent frosted glass and hairline edge sheen.

// Canvas — Pure AMOLED Black
val DarkSurface          = Color(0xFF000000)

// Frosted Translucent Dark Glass (قوائم وزجاج معتم شفاف)
val DarkSurfaceVariant   = Color(0x66181824) // 40% translucent smoky obsidian glass
val DarkSurfaceContainer = Color(0x8014141E) // 50% translucent frosted dark glass
val DarkSurfaceElevated  = Color(0x99161622) // 60% translucent frosted elevated sheet
val DarkCardBorder       = Color(0x33FFFFFF) // 20% crisp white frosted glass edge highlight
val GlassSheen           = Color(0x24FFFFFF) // 14% subtle glass specular sheen
val GlassScrim           = Color(0xCC000000) // 80% deep AMOLED scrim

// Accents — Audiophile Ultra Power Red (أحمر قوي جداً وساطع يتناسب مع السواد الخالص)
val AccentPowerRed       = Color(0xFFFF0033) // Blazing pure vivid red (0% pink, 100% intensity)
val AccentPowerRedLight  = Color(0xFFFF3355)
val AccentPowerRedMuted  = Color(0x33FF0033)
val AccentPowerRedDim    = Color(0xFF990022)

// Theme compatibility aliases
val AccentGold           = AccentPowerRed
val AccentCrimson        = AccentPowerRed
val AccentGoldLight      = AccentPowerRedLight
val AccentGoldMuted      = AccentPowerRedMuted
val AccentGoldDim        = AccentPowerRedDim

val AccentCyan           = Color(0xFF00E5FF) // Electric Cyber Cyan
val AccentCyanGlow       = Color(0x3300E5FF)

// Text
val TextPrimary          = Color(0xFFF7F7F9)
val TextSecondary        = Color(0xFF9C9CA8)
val TextTertiary         = Color(0xFF66666F)

// Semantic
val ErrorRed             = Color(0xFFFF0033)
val SuccessGreen         = Color(0xFF00E676)

/** Pure AMOLED Black backdrop (true #000000) */
val AmbientBackdrop: Brush = Brush.verticalGradient(
    0.0f to DarkSurface,
    1.0f to DarkSurface
)
