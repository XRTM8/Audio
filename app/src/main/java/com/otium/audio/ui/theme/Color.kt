package com.otium.audio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Otium Audio — Athar Luxury Visual Identity (Pure AMOLED & Velvet Burgundy)
// 1. Dark Theme: Pure AMOLED Black (#000000) with deep studio surfaces (#0A0B0E / #14151B) and Velvet Burgundy (#9E1B42).
// 2. Light Theme: Pure AMOLED White (#FFFFFF) with crisp obsidian ink (#0F1014) and Deep Royal Burgundy (#72142B).

// ==========================================
// ATHAR DESIGN TOKENS
// ==========================================
val AmoledBlack = Color(0xFF000000)
val StudioBackground = Color(0xFF0A0B0E)
val StudioSurface = Color(0xFF14151B)
val StudioSurfaceElevated = Color(0xFF1E2028)
val StudioSurfaceActive = Color(0xFF282A36)

val AmoledWhite = Color(0xFFFFFFFF)
val WhiteSurface = Color(0xFFF6F7F9)
val WhiteSurfaceElevated = Color(0xFFECEEF2)
val WhiteSurfaceActive = Color(0xFFDFE2E8)

val PureWhite = Color(0xFFF5F6F8)
val MutedWhite = Color(0xFFA1A3AF)
val SubtleGray = Color(0xFF636674)

val DarkText = Color(0xFF0F1014)
val DarkTextMuted = Color(0xFF5A606E)
val DarkTextSubtle = Color(0xFF8B92A0)

val BurgundyAccent = Color(0xFF9E1B42)
val BurgundyDeep = Color(0xFF72142B)
val BurgundyWarm = Color(0xFF8B1538)
val BurgundyCrimson = Color(0xFFE02447)
val BurgundyMuted = Color(0x249E1B42)
val BurgundyBorderActive = Color(0x669E1B42)

val StudioBorder = Color(0x14FFFFFF)
val StudioBorderSubtle = Color(0x24FFFFFF)
val WhiteBorder = Color(0x1F000000)
val WhiteBorderSubtle = Color(0x14000000)

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

// ── Dark Mode: Pure AMOLED Black & Velvet Burgundy Luxury ───────────────────
val DarkOtiumColors = OtiumThemeColors(
    surface = AmoledBlack,                    // Pure AMOLED Black (#000000)
    surfaceVariant = StudioSurface,           // Card & panel surface (#14151B)
    surfaceContainer = StudioSurface,         // Container surface (#14151B)
    surfaceElevated = StudioSurfaceElevated,  // Elevated interactive fill (#1E2028)
    sheetBackground = StudioBackground,       // Deep studio background (#0A0B0E)
    sheetCard = StudioSurface,                // Elevated grouped cards inside sheets
    sheetCardBorder = StudioBorder,           // Crisp hairline border (8%)
    sheetScrim = Color(0xD90A0B0E),           // 85% deep studio scrim
    cardBorder = StudioBorder,                // Crisp hairline border (8%)
    textPrimary = PureWhite,                  // Primary text (#F5F6F8)
    textSecondary = MutedWhite,               // Secondary labels (#A1A3AF)
    textTertiary = SubtleGray,                // Tertiary & hints (#636674)
    accentTitaniumWhite = PureWhite,          // High-contrast platinum white
    accentTitaniumLight = Color(0xFFE6E8EE),
    accentTitaniumMuted = Color(0x33F5F6F8),
    accentTitaniumDim = SubtleGray,
    accentCyan = BurgundyCrimson,             // Vibrant crimson highlight (#E02447)
    accentGold = BurgundyAccent,              // Velvet Burgundy (#9E1B42)
    searchHighlightAmber = Color(0xFFFFB800),
    glassSheen = Color(0x14FFFFFF),
    glassScrim = Color(0xD90A0B0E),
    isDark = true
)

// ── Light Mode: Pure AMOLED White & Deep Royal Burgundy ─────────────────────
val LightOtiumColors = OtiumThemeColors(
    surface = AmoledWhite,                    // Pure AMOLED White (#FFFFFF)
    surfaceVariant = WhiteSurfaceElevated,    // Elevated slate surface (#ECEEF2)
    surfaceContainer = WhiteSurface,          // Crisp porcelain container (#F6F7F9)
    surfaceElevated = AmoledWhite,            // Pure white elevated cards & sheets
    sheetBackground = WhiteSurface,           // Clean studio white sheet backdrop
    sheetCard = AmoledWhite,                  // Solid luminous white cards in sheets
    sheetCardBorder = WhiteBorderSubtle,      // Hairline dark border (8%)
    sheetScrim = Color(0x45000000),           // Gentle studio background scrim
    cardBorder = WhiteBorder,                 // Hairline dark border (12%)
    textPrimary = DarkText,                   // Deep obsidian ink (#0F1014)
    textSecondary = DarkTextMuted,            // Secondary dark labels (#5A606E)
    textTertiary = DarkTextSubtle,            // Tertiary hints (#8B92A0)
    accentTitaniumWhite = DarkText,           // High-contrast ink on white
    accentTitaniumLight = Color(0xFF22252E),
    accentTitaniumMuted = Color(0x18000000),
    accentTitaniumDim = DarkTextMuted,
    accentCyan = BurgundyWarm,                // Warm Burgundy (#8B1538)
    accentGold = BurgundyDeep,                // Deep Royal Burgundy (#72142B)
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
