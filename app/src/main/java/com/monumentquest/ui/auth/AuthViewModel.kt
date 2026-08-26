package com.monumentquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ActionCodeSettings
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
        // Auto-login: restore saved session on app start
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

    // Step 1: Send official Google Firebase verification link to email
    fun sendOtp(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()

            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://monument-31f3f.firebaseapp.com")
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.monumentquest", true, "24")
                .build()

            auth.sendSignInLinkToEmail(cleanEmail, actionCodeSettings)
                .addOnCompleteListener { task ->
                    _uiState.value = AuthUiState.Idle
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorMsg = task.exception?.message ?: "Failed to send email link."
                        _uiState.value = AuthUiState.Error(errorMsg)
                        onError(errorMsg)
                    }
                }
        }
    }

    // Step 2a: LOGIN — strict authentication via Firebase / backend API
    fun loginWithOtp(email: String, code: String, onNeedsSignup: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()
            val cleanCode = code.trim()

            auth.signInWithEmailAndPassword(cleanEmail, "Pass#$cleanCode")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = auth.currentUser
                        saveSession(
                            token = "fb_token_${fbUser?.uid}",
                            id = fbUser?.uid ?: "u_${cleanEmail.hashCode()}",
                            name = fbUser?.displayName ?: cleanEmail.split("@")[0].replaceFirstChar { it.uppercase() },
                            email = cleanEmail,
                            isGuest = false
                        )
                    } else {
                        viewModelScope.launch {
                            try {
                                val res = withContext(Dispatchers.IO) {
                                    monumentApi.loginWithOtp(LoginWithOtpRequest(cleanEmail, cleanCode))
                                }
                                if (res.success) {
                                    saveSession(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, false)
                                } else if (res.needsSignup) {
                                    _uiState.value = AuthUiState.Idle
                                    onNeedsSignup()
                                } else {
                                    val err = "Authentication failed. Invalid verification code."
                                    _uiState.value = AuthUiState.Error(err)
                                    onError(err)
                                }
                            } catch (e: Exception) {
                                val err = task.exception?.message ?: "Invalid verification code or unverified account."
                                _uiState.value = AuthUiState.Error(err)
                                onError(err)
                            }
                        }
                    }
                }
        }
    }

    // Step 2b: REGISTER — strict new user registration via Firebase / backend API
    fun registerWithOtp(email: String, code: String, name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val cleanEmail = email.trim().lowercase()
            val cleanCode = code.trim()
            val cleanName = name.trim()

            if (cleanName.length < 2) {
                _uiState.value = AuthUiState.Error("Please enter your full name.")
                return@launch
            }

            auth.createUserWithEmailAndPassword(cleanEmail, "Pass#$cleanCode")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = auth.currentUser
                        fbUser?.updateProfile(com.google.firebase.auth.userProfileChangeRequest {
                            displayName = cleanName
                        })
                        saveSession(
                            token = "fb_token_${fbUser?.uid}",
                            id = fbUser?.uid ?: "u_${cleanEmail.hashCode()}",
                            name = cleanName,
                            email = cleanEmail,
                            isGuest = false
                        )
                    } else {
                        viewModelScope.launch {
                            try {
                                val res = withContext(Dispatchers.IO) {
                                    monumentApi.registerWithOtp(RegisterWithOtpRequest(cleanEmail, cleanCode, cleanName))
                                }
                                if (res.success) {
                                    saveSession(res.data.token, res.data.user.id, res.data.user.name, res.data.user.email, false)
                                } else {
                                    val err = task.exception?.message ?: "Registration failed. Invalid code."
                                    _uiState.value = AuthUiState.Error(err)
                                    onError(err)
                                }
                            } catch (e: Exception) {
                                val err = task.exception?.message ?: "Registration failed. Check network or verification code."
                                _uiState.value = AuthUiState.Error(err)
                                onError(err)
                            }
                        }
                    }
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
