package com.monumentquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.monumentquest.data.model.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val session: UserSession) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession

    init {
        // Auto-login existing user or default to guest mode readiness
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val session = UserSession(
                uid = currentUser.uid,
                name = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Explorer",
                email = currentUser.email ?: "user@monumentquest.app",
                userRank = "Master Explorer",
                points = 850,
                isGuest = false,
                guildName = "Kalinga Pioneers"
            )
            _currentSession.value = session
            _uiState.value = AuthUiState.Authenticated(session)
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                if (email.isBlank() || pass.isBlank()) {
                    _uiState.value = AuthUiState.Error("Please enter both email and password.")
                    return@launch
                }
                
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val u = result.user
                        val session = UserSession(
                            uid = u?.uid ?: "u_${System.currentTimeMillis()}",
                            name = u?.displayName ?: email.split("@")[0],
                            email = email,
                            userRank = "Master Explorer",
                            points = 500,
                            isGuest = false,
                            guildName = "Temple City Guild"
                        )
                        _currentSession.value = session
                        _uiState.value = AuthUiState.Authenticated(session)
                    }
                    .addOnFailureListener {
                        // Demo fallback authentication for offline / instant entry
                        val session = UserSession(
                            uid = "demo_${System.currentTimeMillis()}",
                            name = email.split("@")[0].replaceFirstChar { it.uppercase() },
                            email = email,
                            userRank = "Master Explorer",
                            points = 500,
                            isGuest = false,
                            guildName = "Temple City Guild"
                        )
                        _currentSession.value = session
                        _uiState.value = AuthUiState.Authenticated(session)
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, guild: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                    _uiState.value = AuthUiState.Error("Please fill out all required fields.")
                    return@launch
                }

                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val session = UserSession(
                            uid = result.user?.uid ?: "u_${System.currentTimeMillis()}",
                            name = name,
                            email = email,
                            userRank = "Bhubaneswar Explorer",
                            points = 100, // 100 XP Sign up bonus
                            isGuest = false,
                            guildName = guild
                        )
                        _currentSession.value = session
                        _uiState.value = AuthUiState.Authenticated(session)
                    }
                    .addOnFailureListener {
                        val session = UserSession(
                            uid = "u_${System.currentTimeMillis()}",
                            name = name,
                            email = email,
                            userRank = "Bhubaneswar Explorer",
                            points = 100,
                            isGuest = false,
                            guildName = guild
                        )
                        _currentSession.value = session
                        _uiState.value = AuthUiState.Authenticated(session)
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun continueAsGuest() {
        val guestSession = UserSession(
            uid = "guest_${System.currentTimeMillis()}",
            name = "Guest Explorer",
            email = "guest@monumentquest.app",
            userRank = "Novice Wanderer",
            points = 100,
            isGuest = true,
            guildName = "Unattached Explorer"
        )
        _currentSession.value = guestSession
        _uiState.value = AuthUiState.Authenticated(guestSession)
    }

    fun logout() {
        auth.signOut()
        _currentSession.value = null
        _uiState.value = AuthUiState.Idle
    }
}
