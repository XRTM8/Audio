package com.otium.audio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — Titanium White & Obsidian Glass Luxury Edition.
// Pure AMOLED black canvas (#000000) eliminates battery waste and provides infinite contrast.
// Surfaces float with smoky frosted obsidian glass and subtle 10% hairline titanium specular highlights.
// The aesthetic is architecturally calm, professional, and free of AI slop:
// The artwork is the sole colorful hero; chrome is pure titanium and obsidian.

// Canvas — Pure AMOLED Black
val DarkSurface          = Color(0xFF000000)

// Frosted Translucent Obsidian Glass (قوائم وزجاج معتم مدخن فاخر)
val DarkSurfaceVariant   = Color(0x80101015) // 50% translucent smoky obsidian glass
val DarkSurfaceContainer = Color(0x9914141A) // 60% translucent frosted dark glass
val DarkSurfaceElevated  = Color(0xB3181822) // 70% elevated frosted titanium sheet
val DarkCardBorder       = Color(0x1AFFFFFF) // 10% subtle titanium hairline border
val GlassSheen           = Color(0x14FFFFFF) // 8% delicate specular sheen
val GlassScrim           = Color(0xCC000000) // 80% deep AMOLED scrim

// Master Accent — Pure Titanium White (أبيض تيتانيوم نقي وفاخر، قمة الأناقة والاستوديو)
val AccentTitaniumWhite  = Color(0xFFFFFFFF) // Pure crystalline titanium white
val AccentTitaniumLight  = Color(0xFFF2F2F7) // Soft titanium platinum
val AccentTitaniumMuted  = Color(0x33FFFFFF) // 20% titanium glow
val AccentTitaniumDim    = Color(0xFF8E8E93) // Muted brushed titanium

// Theme compatibility aliases
val AccentPowerRed       = AccentTitaniumWhite
val AccentGold           = AccentTitaniumWhite
val AccentCrimson        = AccentTitaniumWhite
val AccentGoldLight      = AccentTitaniumLight
val AccentGoldMuted      = AccentTitaniumMuted
val AccentGoldDim        = AccentTitaniumDim

val AccentCyan           = Color(0xFFE5E5EA) // Ice Titanium
val AccentCyanGlow       = Color(0x26FFFFFF)

// Text Hierarchy
val TextPrimary          = Color(0xFFFFFFFF)
val TextSecondary        = Color(0xFF9898A0)
val TextTertiary         = Color(0xFF636366)

// Semantic (Restrained, calibrated studio tones — never raw or noisy)
val ErrorRed             = Color(0xFFFF453A) // Apple/Studio subtle carmine red
val SuccessGreen         = Color(0xFF30D158) // Studio level green

/** Pure AMOLED Black backdrop (true #000000) */
val AmbientBackdrop: Brush = Brush.verticalGradient(
    0.0f to DarkSurface,
    1.0f to DarkSurface
)
