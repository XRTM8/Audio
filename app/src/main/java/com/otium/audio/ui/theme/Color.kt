package com.otium.audio.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════
//  OTIUM AUDIO — Poweramp-grade Dark Palette
//  Inspired by Poweramp v3 / Neutron Player
// ═══════════════════════════════════════════════

// Base dark layers (NOT pure black — Poweramp uses warm-dark grays)
val DarkSurface          = Color(0xFF101014)   // Main background
val DarkSurfaceVariant   = Color(0xFF18181E)   // Card / item background
val DarkSurfaceContainer = Color(0xFF1F1F27)   // Elevated surfaces
val DarkSurfaceElevated  = Color(0xFF26262F)   // Bottom sheets, dialogs
val DarkCardBorder       = Color(0xFF2C2C38)   // Subtle dividers

// Poweramp signature amber/orange accent
val AccentGold           = Color(0xFFE8A020)   // Primary amber (Poweramp orange)
val AccentGoldLight      = Color(0xFFFFBD45)   // Hover / lighter accent
val AccentGoldMuted      = Color(0x28E8A020)   // 16% alpha for tinted surfaces
val AccentGoldDim        = Color(0xFF7A5010)   // Dim amber for indicators

// Secondary accents
val AccentCyan           = Color(0xFF00D4FF)   // Visualizer / special states

// Typography hierarchy
val TextPrimary          = Color(0xFFF0F0F5)   // Bright white
val TextSecondary        = Color(0xFF8A8A99)   // Grey subtitle
val TextTertiary         = Color(0xFF55555E)   // Very dim captions

// Semantic
val ErrorRed             = Color(0xFFFF453A)   // Favorite / error
val SuccessGreen         = Color(0xFF34C759)   // Positive state
