package com.monumentquest.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Bg,
    primaryContainer = GoldTint,
    onPrimaryContainer = Color(0xFFF6D58C),
    secondary = GreenAccent,
    onSecondary = Bg,
    secondaryContainer = Color(0xFF123229),
    onSecondaryContainer = Color(0xFF9DE8CA),
    tertiary = BlueAccent,
    onTertiary = Bg,
    tertiaryContainer = Color(0xFF18313D),
    onTertiaryContainer = Color(0xFFB7DDEC),
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = BorderSubtle,
    error = RedAccent,
    onError = Color.White,
    errorContainer = EmberDeep,
    onErrorContainer = Color(0xFFFFC1B6),
    inverseSurface = TextPrimary,
    inverseOnSurface = Bg,
    inversePrimary = GoldDim,
    scrim = Color(0xB8000000),
    surfaceTint = Color.Transparent
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5C13),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2A5),
    onPrimaryContainer = Color(0xFF2D1A00),
    secondary = Color(0xFF187A5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F2D7),
    onSecondaryContainer = Color(0xFF002116),
    tertiary = Color(0xFF356A83),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7EAF8),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF7F5EF),
    onBackground = Color(0xFF181D20),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF181D20),
    surfaceVariant = Color(0xFFE9EFEB),
    onSurfaceVariant = Color(0xFF46565A),
    outline = Color(0xFF76878A),
    outlineVariant = Color(0xFFC4D0CE),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

val MonumentShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val MonumentTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

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
        typography = MonumentTypography,
        shapes = MonumentShapes,
        content = content
    )
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
