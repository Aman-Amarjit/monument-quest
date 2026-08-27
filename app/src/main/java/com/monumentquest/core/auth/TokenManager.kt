package com.monumentquest.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun clearToken() = prefs.edit()
        .remove("jwt_token")
        .remove("user_id")
        .remove("user_name")
        .remove("user_email")
        .remove("user_avatar_url")
        .remove("is_guest")
        .apply()

    fun saveUserId(id: String) = prefs.edit().putString("user_id", id).apply()
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getUserName(): String? = prefs.getString("user_name", null)

    fun saveUserEmail(email: String) = prefs.edit().putString("user_email", email).apply()
    fun getUserEmail(): String? = prefs.getString("user_email", null)

    fun saveUserAvatarUrl(url: String?) = prefs.edit().apply {
        if (url.isNullOrBlank()) remove("user_avatar_url") else putString("user_avatar_url", url)
    }.apply()
    fun getUserAvatarUrl(): String? = prefs.getString("user_avatar_url", null)

    fun saveGuestStatus(isGuest: Boolean) = prefs.edit().putBoolean("is_guest", isGuest).apply()
    fun isGuest(): Boolean = prefs.getBoolean("is_guest", false) || getUserEmail()?.endsWith("@guest.monumentquest.app", ignoreCase = true) == true || getUserEmail() == "guest@local"

    fun isLoggedIn(): Boolean = getToken() != null
}
