package com.monumentquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.data.model.UserSession
import com.monumentquest.data.remote.LoginRequest
import com.monumentquest.data.remote.MonumentApi
import com.monumentquest.data.remote.RegisterRequest
import com.monumentquest.data.remote.SendOtpRequest
import com.monumentquest.data.remote.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        // Auto-login: restore saved token on app start
        if (tokenManager.isLoggedIn()) {
            val session = UserSession(
                uid      = tokenManager.getUserId() ?: "",
                name     = tokenManager.getUserName() ?: "Explorer",
                email    = tokenManager.getUserEmail() ?: "",
                userRank = "Explorer",
                points   = 0,
                isGuest  = false,
                guildName = null
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    // Step 1: Send real OTP via Resend email
    fun sendOtp(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { monumentApi.sendOtp(SendOtpRequest(email)) }
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to send OTP. Check your internet connection.")
            }
        }
    }

    // Step 2: Verify OTP against server
    fun verifyOtp(email: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { monumentApi.verifyOtp(VerifyOtpRequest(email, code)) }
                if (res.success) onSuccess()
                else onError("Invalid or expired OTP code")
            } catch (e: Exception) {
                onError("OTP verification failed. Try again.")
            }
        }
    }

    // Step 3: Register — creates account + saves token
    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.register(RegisterRequest(email = email, password = password, name = name))
                }
                if (res.success) {
                    saveSessionFromAuth(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, res.data.user.isGuest)
                } else {
                    _uiState.value = AuthUiState.Error("Registration failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    // Login — authenticates + saves token
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            if (email.isBlank() || password.isBlank()) {
                _uiState.value = AuthUiState.Error("Please enter email and password")
                return@launch
            }
            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.login(LoginRequest(email.trim(), password))
                }
                if (res.success) {
                    saveSessionFromAuth(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, res.data.user.isGuest)
                } else {
                    _uiState.value = AuthUiState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Login failed. Check your internet.")
            }
        }
    }

    // Guest — creates anonymous account + saves token
    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = withContext(Dispatchers.IO) { monumentApi.loginAsGuest() }
                if (res.success) {
                    saveSessionFromAuth(res.data.token, res.data.user.id, "Guest Explorer", res.data.user.email, true)
                } else {
                    // Offline guest fallback
                    val session = UserSession("guest_local", "Guest Explorer", "guest@local", "Wanderer", 0, true, null)
                    _currentSession.value = session
                    _uiState.value = AuthUiState.Authenticated(session)
                }
            } catch (e: Exception) {
                val session = UserSession("guest_local", "Guest Explorer", "guest@local", "Wanderer", 0, true, null)
                _currentSession.value = session
                _uiState.value = AuthUiState.Authenticated(session)
            }
        }
    }

    // Legacy compat
    fun registerUserSecurely(name: String, username: String, email: String, pass: String, role: String) {
        register(name, email, pass)
    }

    fun logout() {
        auth.signOut()
        tokenManager.clearToken()
        _currentSession.value = null
        _uiState.value = AuthUiState.Idle
    }

    private fun saveSessionFromAuth(token: String, id: String, name: String, email: String, isGuest: Boolean) {
        tokenManager.saveToken(token)
        tokenManager.saveUserId(id)
        tokenManager.saveUserName(name)
        tokenManager.saveUserEmail(email)
        val session = UserSession(
            uid = id, name = name, email = email,
            userRank = "Explorer", points = 0, isGuest = isGuest, guildName = null
        )
        _currentSession.value = session
        _uiState.value = AuthUiState.Authenticated(session)
    }
}
