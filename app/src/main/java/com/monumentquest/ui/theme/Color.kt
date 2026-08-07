package com.monumentquest.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Ultra-Modern Dark Glass Palette ─────────────────────────────────────────────
val ObsidianBlack    = Color(0xFF08090C)   // Deepest luxury dark background
val NightSurface     = Color(0xFF10121A)   // Ultra dark slate surface
val ElevatedSurface  = Color(0xFF181B26)   // Floating surface
val CardSurface      = Color(0xFF202433)   // Modern card container
val GlassSurface     = Color(0xF2141724)   // Frosted glass composite

// ── Primary: Modern Emerald & Mint ───────────────────────────────────────────
val ForestDeep       = Color(0xFF0F382B)
val ForestMid        = Color(0xFF1E5B45)
val ForestLight      = Color(0xFF2D8C68)
val ForestMint       = Color(0xFF4ECCA3)   // Electric Mint Highlight

// ── Secondary: Luxury Imperial Gold ──────────────────────────────────────────
val GoldDark         = Color(0xFF9E771B)
val GoldMid          = Color(0xFFC79822)
val GoldBright       = Color(0xFFF3B61D)   // Vibrant Metallic Gold
val GoldShimmer      = Color(0xFFFFD700)

// ── Tertiary: Ember & Terracotta ─────────────────────────────────────────────
val EmberDeep        = Color(0xFF7B1E0A)
val EmberMid         = Color(0xFFD64527)
val EmberLight       = Color(0xFFFF6F59)
val EmberGlow        = Color(0xFFFF8C6B)

// ── Text & Typography Colors ──────────────────────────────────────────────────
val CreamWhite       = Color(0xFFF8F9FA)
val ParchmentLight   = Color(0xFFE2E8F0)
val MutedGray        = Color(0xFF94A3B8)
val SubtleGray       = Color(0xFF2D3748)

// ── Semantic Colors ───────────────────────────────────────────────────────────
val SuccessGreen     = Color(0xFF4ECCA3)
val ErrorRed         = Color(0xFFFF4D4D)
val WarningAmber     = Color(0xFFFFB703)

// ── Medals ────────────────────────────────────────────────────────────────────
val MedalGold        = Color(0xFFFFD700)
val MedalSilver      = Color(0xFFE2E8F0)
val MedalBronze      = Color(0xFFE08D49)

// ── Modern Gradient Brushes ───────────────────────────────────────────────────
val GoldLinearGradient = Brush.horizontalGradient(listOf(GoldBright, Color(0xFFFFE57F)))
val EmeraldLinearGradient = Brush.horizontalGradient(listOf(ForestMint, ForestMid))
val GlassBorderGradient = Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.6f), ForestMint.copy(alpha = 0.4f)))
