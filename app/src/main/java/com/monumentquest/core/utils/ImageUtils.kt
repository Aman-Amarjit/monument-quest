package com.monumentquest.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object ImageUtils {

    // ---------------------------------------------------------------------------
    // Cloudinary unsigned upload — free tier, no Firebase Storage needed.
    // Sign up at https://cloudinary.com (free, no credit card).
    // Then: Dashboard → Settings → Upload → Add unsigned upload preset → copy name.
    // Fill in CLOUD_NAME and UPLOAD_PRESET below.
    // ---------------------------------------------------------------------------
    private const val CLOUDINARY_CLOUD_NAME = "vukeecmu"
    private const val CLOUDINARY_UPLOAD_PRESET = "monument_unsigned"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Upload an image URI to Cloudinary and return the public HTTPS URL.
     * Compresses to max 800px and 75% quality before uploading to keep bandwidth low.
     * Returns null if upload fails.
     * Must be called from a background thread / coroutine (Dispatchers.IO).
     */
    fun uploadToCloudinary(context: Context, uri: Uri): String? {
        if (CLOUDINARY_CLOUD_NAME == "YOUR_CLOUD_NAME") return null  // not configured yet

        return try {
            // Compress image first
            val compressed = compressImage(context, uri, maxDimension = 800, quality = 75)
                ?: return null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "photo.jpg",
                    compressed.toRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("folder", "post_photos")
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            json.optString("secure_url").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImage(context: Context, uri: Uri, maxDimension: Int, quality: Int): ByteArray? {
        return try {
            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val w = original.width
            val h = original.height
            val scale = maxOf(w, h).toFloat() / maxDimension.toFloat()
            val bitmap = if (scale > 1f) {
                Bitmap.createScaledBitmap(original, (w / scale).toInt(), (h / scale).toInt(), true)
            } else original

            ByteArrayOutputStream().also { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

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

    fun isBase64DataUrl(url: String): Boolean {
        return url.startsWith("data:image/") && url.contains(";base64,")
    }

    fun base64ToBitmap(dataUrl: String): Bitmap? {
        return try {
            val base64Data = if (dataUrl.contains(",")) {
                dataUrl.substringAfter(",")
            } else {
                dataUrl
            }
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
