package com.monumentquest.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.data.model.UserProfile
import com.monumentquest.data.remote.MonumentApi
import com.monumentquest.data.remote.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monumentApi: MonumentApi,
    private val tokenManager: TokenManager,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { monumentApi.getUserProfile() }
                _userProfile.value = profile
            } catch (e: Exception) {}

            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val doc = firestore.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val avatarUrl = doc.getString("avatarUrl")
                        val name = doc.getString("name")
                        if (!avatarUrl.isNullOrBlank() || !name.isNullOrBlank()) {
                            _userProfile.value = _userProfile.value.copy(
                                name = name ?: _userProfile.value.name,
                                avatarUrl = avatarUrl ?: _userProfile.value.avatarUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun updateProfile(name: String? = null, avatarUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // Always update backend first — this is the source of truth
            try {
                monumentApi.updateProfile(UpdateProfileRequest(name = name, avatarUrl = avatarUrl))
            } catch (e: Exception) {}

            // Also update Firestore if Firebase user is available (optional secondary store)
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val updates = hashMapOf<String, Any>()
                    if (!name.isNullOrBlank()) updates["name"] = name
                    if (!avatarUrl.isNullOrBlank()) updates["avatarUrl"] = avatarUrl
                    if (updates.isNotEmpty()) {
                        firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
                    }
                }
            } catch (e: Exception) {}

            loadProfile()
        }
    }
}
