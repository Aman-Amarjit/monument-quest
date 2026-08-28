package com.monumentquest.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun uriToBase64DataUrl(context: Context, uri: Uri, maxDimension: Int = 300, quality: Int = 75): String? {
        return try {
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = maxOf(width, height).toFloat() / maxDimension.toFloat()

            val resizedBitmap = if (scale > 1.0f) {
                val targetW = (width / scale).toInt()
                val targetH = (height / scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else originalBitmap

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
