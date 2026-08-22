package com.monumentquest.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Base surfaces ─────────────────────────────────────────────────────────────
val Bg           = Color(0xFF0A0A0F)   // page background
val Surface1     = Color(0xFF111118)   // card background
val Surface2     = Color(0xFF1A1A24)   // elevated card / modal
val Surface3     = Color(0xFF222232)   // highest elevation
val Border       = Color(0xFF1E1E2E)   // card border
val BorderSubtle = Color(0xFF14141E)   // very subtle divider

// ── Accent ────────────────────────────────────────────────────────────────────
val Gold     = Color(0xFFF0A500)   // primary accent — use sparingly
val GoldDim  = Color(0xFF8A5E00)   // muted gold for secondary uses
val GoldTint = Color(0xFF1A1200)   // gold background tint

// ── Semantic ──────────────────────────────────────────────────────────────────
val GreenAccent = Color(0xFF22C55E)
val RedAccent   = Color(0xFFEF4444)
val BlueAccent  = Color(0xFF3B82F6)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF64748B)
val TextTertiary  = Color(0xFF334155)

// ── Legacy aliases (keep for backward compat with ViewModels / data classes) ──
val ObsidianBlack   = Bg
val NightSurface    = Surface1
val ElevatedSurface = Surface2
val CardSurface     = Surface1
val GlassSurface    = Surface2
val ForestDeep      = Color(0xFF0A0A0F)
val ForestMid       = Color(0xFF1A1A24)
val ForestLight     = Color(0xFF222232)
val ForestMint      = GreenAccent
val GoldBright      = Gold
val GoldMid         = GoldDim
val GoldShimmer     = Gold
val GoldDark        = GoldDim
val CreamWhite      = TextPrimary
val ParchmentLight  = TextPrimary
val MutedGray       = TextSecondary
val SubtleGray      = Surface3
val ErrorRed        = RedAccent
val SuccessGreen    = GreenAccent
val EmberMid        = Color(0xFFEF4444)
val EmberDeep       = Color(0xFF4D1218)
val EmberLight      = Color(0xFFFF6F59)
val EmberGlow       = Color(0xFFFF8C6B)
val MedalGold       = Gold
val MedalSilver     = Color(0xFF94A3B8)
val MedalBronze     = Color(0xFFB45309)
val WarningAmber    = Color(0xFFF59E0B)
val GoldLinearGradient    = Brush.horizontalGradient(listOf(Gold, Color(0xFFFFD97A)))
val EmeraldLinearGradient = Brush.horizontalGradient(listOf(GreenAccent, Color(0xFF16A34A)))
val GlassBorderGradient   = Brush.linearGradient(listOf(Gold.copy(alpha = 0.4f), GreenAccent.copy(alpha = 0.2f)))
