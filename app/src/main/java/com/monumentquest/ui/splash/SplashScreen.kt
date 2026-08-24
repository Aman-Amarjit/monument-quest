package com.monumentquest.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monumentquest.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var progress         by remember { mutableStateOf(0f) }
    var loadingText      by remember { mutableStateOf("Initializing GPS & Map Engine…") }
    var iconVisible      by remember { mutableStateOf(false) }

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "progress"
    )

    // Pulsing glow on the icon
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue   = 0.15f,
        targetValue    = 0.45f,
        animationSpec  = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val iconScale by animateFloatAsState(
        targetValue   = if (iconVisible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label         = "iconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue   = if (iconVisible) 1f else 0f,
        animationSpec = tween(400),
        label         = "iconAlpha"
    )

    LaunchedEffect(Unit) {
        iconVisible = true
        delay(400)
        progress = 0.30f
        loadingText = "Connecting to OpenStreetMap Overpass API…"
        delay(450)
        progress = 0.65f
        loadingText = "Loading CartoDB Voyager Tiles…"
        delay(450)
        progress = 1.0f
        loadingText = "Ready — let's explore"
        delay(250)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0D18),
                        Color(0xFF0A0A0F),
                        Color(0xFF0C0C16)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background decorative grid dots
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.04f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Gold.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(horizontal = 40.dp)
        ) {
            // ── Icon with glow rings ──────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .scale(iconScale)
                    .graphicsLayer { alpha = iconAlpha }
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = glowAlpha * 0.4f))
                )
                // Mid ring
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = glowAlpha * 0.6f))
                )
                // Icon container
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1E2E), Color(0xFF141420))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Explore,
                        contentDescription = "Monument Quest",
                        tint               = Gold,
                        modifier           = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text       = "Monument Quest",
                color      = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 30.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Gold accent rule
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Gold, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "Discover heritage around you",
                color     = TextSecondary,
                fontSize  = 14.sp,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── Progress bar ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Gold.copy(alpha = 0.7f), Gold)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text     = loadingText,
                color    = TextSecondary,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp
            )
        }

        // ── Version badge ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text     = "v1.0 · OSM + CartoDB",
                color    = TextSecondary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
