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
// Dark Colour Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary                = Gold,
    onPrimary              = Bg,
    primaryContainer       = GoldTint,
    onPrimaryContainer     = Gold,

    secondary              = GreenAccent,
    onSecondary            = Bg,
    secondaryContainer     = Surface2,
    onSecondaryContainer   = GreenAccent,

    tertiary               = RedAccent,
    onTertiary             = TextPrimary,
    tertiaryContainer      = EmberDeep,
    onTertiaryContainer    = EmberGlow,

    background             = Bg,
    onBackground           = TextPrimary,

    surface                = Surface1,
    onSurface              = TextPrimary,
    surfaceVariant         = Surface2,
    onSurfaceVariant       = TextSecondary,

    outline                = Border,
    outlineVariant         = BorderSubtle,

    error                  = RedAccent,
    onError                = TextPrimary,
    errorContainer         = Color(0xFF4D1218),
    onErrorContainer       = Color(0xFFFFB3B3),

    inverseSurface         = TextPrimary,
    inverseOnSurface       = Bg,
    inversePrimary         = GoldDim,
    scrim                  = Color(0x99000000),
    surfaceTint            = Color.Transparent,
)

// ─────────────────────────────────────────────────────────────────────────────
// Light Colour Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary                = Gold,
    onPrimary              = Bg,
    primaryContainer       = Color(0xFFFFE8A0),
    onPrimaryContainer     = GoldDim,

    secondary              = GreenAccent,
    onSecondary            = TextPrimary,
    secondaryContainer     = Color(0xFFB7F5D0),
    onSecondaryContainer   = Color(0xFF14532D),

    tertiary               = RedAccent,
    onTertiary             = TextPrimary,
    tertiaryContainer      = Color(0xFFFFD1C1),
    onTertiaryContainer    = EmberDeep,

    background             = Color(0xFFF5F0E8),
    onBackground           = Bg,

    surface                = TextPrimary,
    onSurface              = Bg,
    surfaceVariant         = Color(0xFFEDE8DC),
    onSurfaceVariant       = Color(0xFF4A4A4A),

    outline                = Color(0xFFAAAAAA),
    error                  = RedAccent,
    onError                = TextPrimary,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shapes
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
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
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
