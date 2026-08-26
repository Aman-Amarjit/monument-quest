package com.monumentquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.monumentquest.ui.theme.*

@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    size: Dp = 40.dp,
    borderColor: Color = GoldBright,
    modifier: Modifier = Modifier
) {
    val cleanUrl = avatarUrl?.trim()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        if (!cleanUrl.isNullOrEmpty()) {
            AsyncImage(
                model = cleanUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = name.trim().split(" ").let { parts ->
                if (parts.size >= 2) "${parts[0].firstOrNull() ?: ' '}${parts[1].firstOrNull() ?: ' '}"
                else name.take(2)
            }.uppercase().trim()

            val colorPair = when (kotlin.math.abs(name.hashCode()) % 4) {
                0 -> listOf(ForestMid, ForestDeep)
                1 -> listOf(GoldDark, EmberDeep)
                2 -> listOf(Color(0xFF2C3E50), Color(0xFF000000))
                else -> listOf(EmberMid, GoldDark)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colorPair)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (initials.isNotEmpty()) initials else "MQ",
                    fontWeight = FontWeight.Black,
                    color = CreamWhite,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }
    }
}
