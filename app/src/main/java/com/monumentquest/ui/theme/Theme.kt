package com.monumentquest.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
// Dark Colour Scheme  (primary usage — app defaults to dark)
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary                = ForestMid,
    onPrimary              = CreamWhite,
    primaryContainer       = ForestDeep,
    onPrimaryContainer     = ForestMint,

    secondary              = GoldBright,
    onSecondary            = ObsidianBlack,
    secondaryContainer     = GoldDark,
    onSecondaryContainer   = GoldShimmer,

    tertiary               = EmberMid,
    onTertiary             = CreamWhite,
    tertiaryContainer      = EmberDeep,
    onTertiaryContainer    = EmberGlow,

    background             = ObsidianBlack,
    onBackground           = CreamWhite,

    surface                = NightSurface,
    onSurface              = CreamWhite,
    surfaceVariant         = ElevatedSurface,
    onSurfaceVariant       = ParchmentLight,

    outline                = SubtleGray,
    outlineVariant         = Color(0xFF2A2A2A),

    error                  = ErrorRed,
    onError                = CreamWhite,
    errorContainer         = Color(0xFF4D1218),
    onErrorContainer       = Color(0xFFFFB3B3),

    inverseSurface         = CreamWhite,
    inverseOnSurface       = ObsidianBlack,
    inversePrimary         = ForestLight,
    scrim                  = Color(0x99000000),
    surfaceTint            = ForestMid,
)

// ─────────────────────────────────────────────────────────────────────────────
// Light Colour Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary                = ForestMid,
    onPrimary              = CreamWhite,
    primaryContainer       = Color(0xFFB7E4C7),
    onPrimaryContainer     = ForestDeep,

    secondary              = GoldMid,
    onSecondary            = CreamWhite,
    secondaryContainer     = Color(0xFFFFEEAA),
    onSecondaryContainer   = GoldDark,

    tertiary               = EmberMid,
    onTertiary             = CreamWhite,
    tertiaryContainer      = Color(0xFFFFD1C1),
    onTertiaryContainer    = EmberDeep,

    background             = Color(0xFFF5F0E8),
    onBackground           = ObsidianBlack,

    surface                = CreamWhite,
    onSurface              = ObsidianBlack,
    surfaceVariant         = Color(0xFFEDE8DC),
    onSurfaceVariant       = Color(0xFF4A4A4A),

    outline                = Color(0xFFAAAAAA),
    error                  = ErrorRed,
    onError                = CreamWhite,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shapes — slightly more rounded for a premium feel
// ─────────────────────────────────────────────────────────────────────────────
val MonumentShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonumentQuestTheme(
    darkTheme: Boolean = true,          // default dark — feels more cinematic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            // Fully transparent status and navigation bars
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = MonumentShapes,
        content     = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
