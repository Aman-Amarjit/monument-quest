package com.monumentquest.core.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun clearToken() = prefs.edit().remove("jwt_token").apply()

    fun saveUserId(id: String) = prefs.edit().putString("user_id", id).apply()
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getUserName(): String? = prefs.getString("user_name", null)

    fun saveUserEmail(email: String) = prefs.edit().putString("user_email", email).apply()
    fun getUserEmail(): String? = prefs.getString("user_email", null)

    fun isLoggedIn(): Boolean = getToken() != null
}
