package com.otium.audio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — Studio Mastering Luxury Themes
// 1. Dark Theme: Pure AMOLED Black canvas (#000000) with solid smoky obsidian and titanium specular highlights.
// 2. Light Theme: Velvety studio porcelain canvas (#F7F8FA) with crisp obsidian ink typography and clean slate dividers.
// Calibrated for acoustic listening sessions with zero glare, optimal contrast, and zero AI slop.

@Immutable
data class OtiumThemeColors(
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceElevated: Color,
    val sheetBackground: Color,
    val sheetCard: Color,
    val sheetCardBorder: Color,
    val sheetScrim: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentTitaniumWhite: Color,
    val accentTitaniumLight: Color,
    val accentTitaniumMuted: Color,
    val accentTitaniumDim: Color,
    val accentCyan: Color,
    val accentGold: Color,
    val searchHighlightAmber: Color,
    val glassSheen: Color,
    val glassScrim: Color,
    val isDark: Boolean
)

// ── Dark Mode: Pure AMOLED Black & Titanium Obsidian Luxury ──────────────────
val DarkOtiumColors = OtiumThemeColors(
    surface = Color(0xFF000000),             // Pure AMOLED Black (#000000)
    surfaceVariant = Color(0xFF111116),      // Solid smoky obsidian
    surfaceContainer = Color(0xFF131318),    // Solid dark obsidian container
    surfaceElevated = Color(0xFF181822),     // Solid elevated titanium sheet
    sheetBackground = Color(0xFF0E0E12),     // Deep obsidian dark for bottom sheets
    sheetCard = Color(0xFF131318),           // Elevated grouped cards inside sheets
    sheetCardBorder = Color(0x18FFFFFF),     // 10% subtle titanium hairline border
    sheetScrim = Color(0xD9000000),          // 85% deep AMOLED scrim
    cardBorder = Color(0x1AFFFFFF),          // 10% subtle titanium hairline border
    textPrimary = Color(0xFFFFFFFF),         // Pure titanium white
    textSecondary = Color(0xFF9898A0),       // Cool calibrated slate
    textTertiary = Color(0xFF636366),        // Deep muted slate
    accentTitaniumWhite = Color(0xFFFFFFFF), // Pure crystalline white
    accentTitaniumLight = Color(0xFFF2F2F7),
    accentTitaniumMuted = Color(0x33FFFFFF),
    accentTitaniumDim = Color(0xFF8E8E93),
    accentCyan = Color(0xFFE5E5EA),
    accentGold = Color(0xFFFFFFFF),
    searchHighlightAmber = Color(0xFFFFB800),
    glassSheen = Color(0x14FFFFFF),
    glassScrim = Color(0xD9000000),
    isDark = true
)

// ── Light Mode: Studio Porcelain & Sharp Slate Luxury Edition ────────────────
val LightOtiumColors = OtiumThemeColors(
    surface = Color(0xFFF6F7FB),             // Calibrated smooth studio porcelain (reduces glare & eye fatigue)
    surfaceVariant = Color(0xFFEDF0F5),      // Soft architectural slate for badges & wells
    surfaceContainer = Color(0xFFF1F4F8),    // Balanced platinum container for icon wells & cards
    surfaceElevated = Color(0xFFFFFFFF),     // Pure luminous white for elevated cards, sheets & dialogs
    sheetBackground = Color(0xFFF2F4F8),     // Architectural porcelain sheet backdrop
    sheetCard = Color(0xFFFFFFFF),           // Solid luminous white cards in sheets
    sheetCardBorder = Color(0xFFE2E6EE),     // Clean architectural slate hairline (1dp)
    sheetScrim = Color(0x45000000),          // Gentle studio background scrim
    cardBorder = Color(0xFFE2E6EE),          // Subtle hairline border
    textPrimary = Color(0xFF0F1115),         // Deep obsidian ink (sharp, ultra-legible, WCAG AAA)
    textSecondary = Color(0xFF5B6271),       // Balanced studio slate for artists & metadata
    textTertiary = Color(0xFF8F96A5),        // Muted architectural slate for hints
    accentTitaniumWhite = Color(0xFF0F1115), // In Light Mode, contrasting hero element is Deep Obsidian Ink
    accentTitaniumLight = Color(0xFF222630),
    accentTitaniumMuted = Color(0x18000000),
    accentTitaniumDim = Color(0xFF6B7280),
    accentCyan = Color(0xFF2563EB),          // Refined studio cobalt for faders & active switches
    accentGold = Color(0xFF0F1115),
    searchHighlightAmber = Color(0xFFB45309), // Rich high-contrast amber for light background
    glassSheen = Color(0x08000000),
    glassScrim = Color(0x45000000),
    isDark = false
)

val LocalOtiumColors = staticCompositionLocalOf { DarkOtiumColors }

// Dynamic Theme Color Accessors for Composable Tree
val DarkSurface: Color
    @Composable
    get() = LocalOtiumColors.current.surface

val DarkSurfaceVariant: Color
    @Composable
    get() = LocalOtiumColors.current.surfaceVariant

val DarkSurfaceContainer: Color
    @Composable
    get() = LocalOtiumColors.current.surfaceContainer

val DarkSurfaceElevated: Color
    @Composable
    get() = LocalOtiumColors.current.surfaceElevated

val DarkSheetBackground: Color
    @Composable
    get() = LocalOtiumColors.current.sheetBackground

val DarkSheetCard: Color
    @Composable
    get() = LocalOtiumColors.current.sheetCard

val DarkSheetCardBorder: Color
    @Composable
    get() = LocalOtiumColors.current.sheetCardBorder

val DarkSheetScrim: Color
    @Composable
    get() = LocalOtiumColors.current.sheetScrim

val DarkCardBorder: Color
    @Composable
    get() = LocalOtiumColors.current.cardBorder

val GlassSheen: Color
    @Composable
    get() = LocalOtiumColors.current.glassSheen

val GlassScrim: Color
    @Composable
    get() = LocalOtiumColors.current.glassScrim

val AccentTitaniumWhite: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumWhite

val AccentTitaniumLight: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumLight

val AccentTitaniumMuted: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumMuted

val AccentTitaniumDim: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumDim

val AccentGold: Color
    @Composable
    get() = LocalOtiumColors.current.accentGold

val AccentGoldLight: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumLight

val AccentGoldMuted: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumMuted

val AccentGoldDim: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumDim

val AccentPowerRed: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumWhite

val AccentCrimson: Color
    @Composable
    get() = LocalOtiumColors.current.accentTitaniumWhite

val AccentCyan: Color
    @Composable
    get() = LocalOtiumColors.current.accentCyan

val AccentCyanGlow: Color
    @Composable
    get() = if (LocalOtiumColors.current.isDark) Color(0x26FFFFFF) else Color(0x180F62FE)

val TextPrimary: Color
    @Composable
    get() = LocalOtiumColors.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalOtiumColors.current.textSecondary

val TextTertiary: Color
    @Composable
    get() = LocalOtiumColors.current.textTertiary

val SearchHighlightAmber: Color
    @Composable
    get() = LocalOtiumColors.current.searchHighlightAmber

val ErrorRed: Color = Color(0xFFFF453A)
val SuccessGreen: Color = Color(0xFF30D158)

val AmbientBackdrop: Brush
    @Composable
    get() = Brush.verticalGradient(
        0.0f to LocalOtiumColors.current.surface,
        1.0f to LocalOtiumColors.current.surface
    )
