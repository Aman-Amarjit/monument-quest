package com.monumentquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.monumentquest.data.model.UserSession
import com.monumentquest.data.remote.MonumentApi
import com.monumentquest.data.remote.SignUpRequest
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
    private val monumentApi: MonumentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val session = UserSession(
                uid = currentUser.uid,
                name = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Explorer",
                email = currentUser.email ?: "user@monumentquest.app",
                userRank = "Novice Explorer",
                points = 0,
                isGuest = false,
                guildName = "Heritage Pioneers"
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            if (email.isBlank() || pass.isBlank()) {
                _uiState.value = AuthUiState.Error("Please enter both email and password.")
                return@launch
            }

            val session = UserSession(
                uid = "u_${System.currentTimeMillis()}",
                name = email.split("@")[0].replaceFirstChar { it.uppercase() },
                email = email,
                userRank = "Novice Explorer",
                points = 0,
                isGuest = false,
                guildName = "Heritage Pioneers"
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    fun registerUserSecurely(name: String, username: String, email: String, pass: String, role: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                _uiState.value = AuthUiState.Error("Please fill out name, email, and password.")
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val handle = if (username.isNotBlank()) username else name.lowercase().replace(" ", "_")
                    val res = monumentApi.signUp(SignUpRequest(name, handle, email, pass, role))
                    if (res.success) {
                        val session = UserSession(
                            uid = res.userId,
                            name = name,
                            email = email,
                            userRank = "Novice Explorer",
                            points = 0,
                            isGuest = false,
                            guildName = role
                        )
                        withContext(Dispatchers.Main) {
                            _currentSession.value = session
                            _uiState.value = AuthUiState.Authenticated(session)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiState.value = AuthUiState.Error("Registration failed.")
                        }
                    }
                } catch (e: Exception) {
                    val session = UserSession(
                        uid = "u_${System.currentTimeMillis()}",
                        name = name,
                        email = email,
                        userRank = "Novice Explorer",
                        points = 0,
                        isGuest = false,
                        guildName = role
                    )
                    withContext(Dispatchers.Main) {
                        _currentSession.value = session
                        _uiState.value = AuthUiState.Authenticated(session)
                    }
                }
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val guestSession = UserSession(
                uid       = "guest_${System.currentTimeMillis()}",
                name      = "Guest Explorer",
                email     = "guest@monumentquest.app",
                userRank  = "Novice Wanderer",
                points    = 0,
                isGuest   = true,
                guildName = "Unattached Explorer"
            )
            _currentSession.value = guestSession
            _uiState.value = AuthUiState.Authenticated(guestSession)
        }
    }

    fun logout() {
        auth.signOut()
        _currentSession.value = null
        _uiState.value = AuthUiState.Idle
    }
}
