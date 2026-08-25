package com.monumentquest.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// MonumentQuest visual system: midnight ink, warm brass, and field green.
val Bg           = Color(0xFF0B1117)
val Surface1     = Color(0xFF111A22)
val Surface2     = Color(0xFF17232D)
val Surface3     = Color(0xFF20313C)
val Border       = Color(0xFF29404A)
val BorderSubtle = Color(0xFF1B2A33)

val Gold         = Color(0xFFE5A83B)
val GoldDim      = Color(0xFF9A6C22)
val GoldTint     = Color(0xFF2B2110)

val GreenAccent  = Color(0xFF47C49A)
val RedAccent    = Color(0xFFE56B6F)
val BlueAccent   = Color(0xFF68A8C6)

val TextPrimary   = Color(0xFFF5F1E8)
val TextSecondary = Color(0xFFA7B6BA)
val TextTertiary  = Color(0xFF6E858C)

// Legacy aliases retained so existing screens keep compiling while adopting the new system.
val ObsidianBlack   = Bg
val NightSurface    = Surface1
val ElevatedSurface = Surface2
val CardSurface     = Surface1
val GlassSurface    = Surface2
val ForestDeep      = Bg
val ForestMid       = Surface2
val ForestLight     = Surface3
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
val EmberMid        = RedAccent
val EmberDeep       = Color(0xFF3A1D25)
val EmberLight      = Color(0xFFF08B7E)
val EmberGlow       = Color(0xFFFFB19D)
val MedalGold       = Gold
val MedalSilver     = Color(0xFFA7B6BA)
val MedalBronze     = Color(0xFFB87945)
val WarningAmber    = Color(0xFFF2B84B)

val GoldLinearGradient    = Brush.horizontalGradient(listOf(Color(0xFFF0C66D), Gold))
val EmeraldLinearGradient = Brush.horizontalGradient(listOf(Color(0xFF6DE0B5), GreenAccent))
val GlassBorderGradient   = Brush.linearGradient(listOf(Gold.copy(alpha = 0.45f), GreenAccent.copy(alpha = 0.25f)))
