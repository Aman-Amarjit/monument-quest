package com.monumentquest.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.monumentquest.core.utils.ImageUtils
import com.monumentquest.ui.theme.CardSurface
import com.monumentquest.ui.theme.MutedGray

@Composable
fun SmartAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackContent: (@Composable () -> Unit)? = null
) {
    val modelString = model as? String

    if (modelString != null && ImageUtils.isBase64DataUrl(modelString)) {
        val bitmap = remember(modelString) {
            ImageUtils.base64ToBitmap(modelString)
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else if (fallbackContent != null) {
            fallbackContent()
        } else {
            DefaultImagePlaceholder(modifier)
        }
    } else if (model != null && (model !is String || model.isNotBlank())) {
        val context = LocalContext.current
        val imageRequest = remember(model) {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (fallbackContent != null) {
        fallbackContent()
    } else {
        DefaultImagePlaceholder(modifier)
    }
}

@Composable
fun DefaultImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(CardSurface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Landscape,
            contentDescription = "Image Placeholder",
            tint = MutedGray.copy(alpha = 0.5f)
        )
    }
}
