package com.monumentquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.data.model.UserSession
import com.monumentquest.data.remote.LoginWithOtpRequest
import com.monumentquest.data.remote.MonumentApi
import com.monumentquest.data.remote.RegisterWithOtpRequest
import com.monumentquest.data.remote.SendOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val session: UserSession) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val monumentApi: MonumentApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession

    init {
        // Auto-login: restore saved session on app start
        if (tokenManager.isLoggedIn()) {
            val session = UserSession(
                uid = tokenManager.getUserId() ?: "",
                name = tokenManager.getUserName() ?: "Explorer",
                email = tokenManager.getUserEmail() ?: "",
                userRank = "Explorer", points = 0, isGuest = tokenManager.isGuest(), guildName = "", avatarUrl = tokenManager.getUserAvatarUrl()
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    // Step 1: Send real 6-digit OTP email via Gmail SMTP API (No Firebase Quota limits)
    fun sendOtp(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()

            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.sendOtp(SendOtpRequest(cleanEmail))
                }
                _uiState.value = AuthUiState.Idle
                if (res.success) {
                    onSuccess()
                } else {
                    val err = res.message.ifBlank { "Could not send OTP email. Please try again." }
                    _uiState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Idle
                val err = e.message ?: "Network error. Could not reach server."
                _uiState.value = AuthUiState.Error(err)
                onError(err)
            }
        }
    }

    // Step 2a: LOGIN — strict 6-digit OTP verification via backend API & Supabase DB
    fun loginWithOtp(email: String, code: String, onNeedsSignup: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()
            val cleanCode = code.trim()

            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.loginWithOtp(LoginWithOtpRequest(cleanEmail, cleanCode))
                }
                if (res.success) {
                    saveSession(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, false, res.data.user.avatarUrl)
                } else if (res.needsSignup) {
                    _uiState.value = AuthUiState.Idle
                    onNeedsSignup()
                } else {
                    val err = "Authentication failed. Invalid verification code."
                    _uiState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    // New user! OTP was verified, transition smoothly to Name Signup
                    _uiState.value = AuthUiState.Idle
                    onNeedsSignup()
                } else {
                    val err = "Invalid or expired 6-digit verification code."
                    _uiState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: Exception) {
                val err = "Invalid or expired 6-digit verification code."
                _uiState.value = AuthUiState.Error(err)
                onError(err)
            }
        }
    }

    // Step 2b: REGISTER — strict 6-digit OTP verification & unique username check
    fun registerWithOtp(email: String, code: String, name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()
            val cleanCode = code.trim()
            val cleanName = name.trim()

            if (cleanName.length < 2) {
                val err = "Username must be at least 2 characters long."
                _uiState.value = AuthUiState.Error(err)
                onError(err)
                return@launch
            }

            try {
                val backendRes = withContext(Dispatchers.IO) {
                    monumentApi.registerWithOtp(RegisterWithOtpRequest(cleanEmail, cleanCode, cleanName))
                }
                if (backendRes.success) {
                    saveSession(backendRes.data.token, backendRes.data.user.id, cleanName, cleanEmail, false, backendRes.data.user.avatarUrl)
                } else if (backendRes.alreadyExists) {
                    val err = "Username \"$cleanName\" is already taken. Please choose another username."
                    _uiState.value = AuthUiState.Error(err)
                    onError(err)
                } else {
                    val err = "Registration failed. Invalid code or username."
                    _uiState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: HttpException) {
                val err = if (e.code() == 409) {
                    "Username \"$cleanName\" is already taken. Please choose another username."
                } else {
                    "Registration failed. Invalid 6-digit code."
                }
                _uiState.value = AuthUiState.Error(err)
                onError(err)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val err = if (msg.contains("taken", ignoreCase = true) || msg.contains("409")) {
                    "Username \"$cleanName\" is already taken. Please choose another username."
                } else {
                    "Registration failed. Invalid 6-digit code."
                }
                _uiState.value = AuthUiState.Error(err)
                onError(err)
            }
        }
    }

    // Guest — anonymous account
    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = withContext(Dispatchers.IO) { monumentApi.loginAsGuest() }
                if (res.success) {
                    saveSession(res.data.token, res.data.user.id, "Guest Explorer", res.data.user.email, true, res.data.user.avatarUrl)
                } else offlineGuest()
            } catch (e: Exception) { offlineGuest() }
        }
    }

    fun logout() {
        auth.signOut()
        tokenManager.clearToken()
        _currentSession.value = null
        _uiState.value = AuthUiState.Idle
    }

    // Legacy compat
    fun login(email: String, pass: String) { continueAsGuest() }
    fun registerUserSecurely(name: String, username: String, email: String, pass: String, role: String) {}

    private fun offlineGuest() {
        tokenManager.saveGuestStatus(true)
        tokenManager.saveUserAvatarUrl(null)
        val session = UserSession("guest_local", "Guest Explorer", "guest@local", "Wanderer", 0, true, "", null)
        _currentSession.value = session
        _uiState.value = AuthUiState.Authenticated(session)
    }

    private fun saveSession(token: String, id: String, name: String, email: String, isGuest: Boolean, avatarUrl: String? = null) {
        tokenManager.saveToken(token)
        tokenManager.saveUserId(id)
        tokenManager.saveUserName(name)
        tokenManager.saveUserEmail(email)
        tokenManager.saveUserAvatarUrl(avatarUrl)
        tokenManager.saveGuestStatus(isGuest)
        val session = UserSession(uid = id, name = name, email = email,
            userRank = "Explorer", points = 0, isGuest = isGuest, guildName = "", avatarUrl = avatarUrl)
        _currentSession.value = session
        _uiState.value = AuthUiState.Authenticated(session)
    }
}
