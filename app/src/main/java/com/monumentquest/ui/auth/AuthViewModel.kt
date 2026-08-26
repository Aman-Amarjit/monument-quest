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
        // Auto-login: restore saved JWT on app start
        if (tokenManager.isLoggedIn()) {
            val session = UserSession(
                uid = tokenManager.getUserId() ?: "",
                name = tokenManager.getUserName() ?: "Explorer",
                email = tokenManager.getUserEmail() ?: "",
                userRank = "Explorer", points = 0, isGuest = false, guildName = ""
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    // Step 1: Send OTP to email
    fun sendOtp(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { monumentApi.sendOtp(SendOtpRequest(email.trim())) }
                if (res.success) {
                    onSuccess()
                } else {
                    onError(res.message.ifBlank { "Could not send OTP. Check email address." })
                }
            } catch (e: Exception) {
                onError("Failed to send verification email. Check network connection.")
            }
        }
    }

    // Step 2a: LOGIN — verify exact OTP, log in existing user
    fun loginWithOtp(email: String, code: String, onNeedsSignup: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.loginWithOtp(LoginWithOtpRequest(email.trim().lowercase(), code.trim()))
                }
                if (res.success) {
                    saveSession(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, false)
                } else if (res.needsSignup) {
                    _uiState.value = AuthUiState.Idle
                    onNeedsSignup()
                } else {
                    _uiState.value = AuthUiState.Error("Invalid or expired OTP code.")
                    onError("Invalid or expired OTP code.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Verification failed. Check the 6-digit code.")
                onError("Verification failed. Check the 6-digit code.")
            }
        }
    }

    // Step 2b: REGISTER — verify exact OTP + create new account with name
    fun registerWithOtp(email: String, code: String, name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            if (name.trim().length < 2) {
                _uiState.value = AuthUiState.Error("Please enter your full name.")
                return@launch
            }
            try {
                val res = withContext(Dispatchers.IO) {
                    monumentApi.registerWithOtp(RegisterWithOtpRequest(email.trim().lowercase(), code.trim(), name.trim()))
                }
                if (res.success) {
                    saveSession(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, false)
                } else if (res.alreadyExists) {
                    _uiState.value = AuthUiState.Error("Email already registered. Please log in instead.")
                } else {
                    _uiState.value = AuthUiState.Error("Registration failed. Invalid OTP code.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Registration failed. Invalid OTP code.")
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
                    saveSession(res.data.token, res.data.user.id, "Guest Explorer", res.data.user.email, true)
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
        val session = UserSession("guest_local", "Guest Explorer", "guest@local", "Wanderer", 0, true, "")
        _currentSession.value = session
        _uiState.value = AuthUiState.Authenticated(session)
    }

    private fun saveSession(token: String, id: String, name: String, email: String, isGuest: Boolean) {
        tokenManager.saveToken(token)
        tokenManager.saveUserId(id)
        tokenManager.saveUserName(name)
        tokenManager.saveUserEmail(email)
        val session = UserSession(uid = id, name = name, email = email,
            userRank = "Explorer", points = 0, isGuest = isGuest, guildName = "")
        _currentSession.value = session
        _uiState.value = AuthUiState.Authenticated(session)
    }
}
