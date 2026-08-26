package com.monumentquest.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monumentApi: MonumentApi,
    private val tokenManager: TokenManager
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
        }
    }

    fun updateProfile(name: String? = null, avatarUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                monumentApi.updateProfile(UpdateProfileRequest(name = name, avatarUrl = avatarUrl))
                loadProfile()
            } catch (e: Exception) {}
        }
    }
}
