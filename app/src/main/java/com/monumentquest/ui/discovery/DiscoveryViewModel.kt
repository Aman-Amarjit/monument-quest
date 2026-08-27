package com.monumentquest.ui.discovery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.monumentquest.data.remote.CaptureRequest
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class DiscoveryResult(
    val monumentName: String = "",
    val previousUploadersCount: Int = 0,
    val multiplier: Float = 1.0f,
    val pointsEarned: Int = 100,
    val rarityBadge: String = "COMMON LANDMARK",
    val message: String = "Monument captured successfully!"
)

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Loading : DiscoveryState()
    data class Success(val result: DiscoveryResult) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val monumentApi: MonumentApi
) : ViewModel() {

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state

    fun uploadDiscovery(
        name: String,
        imageUri: Uri,
        latitude: Double = 20.2381,
        longitude: Double = 85.8338,
        monumentId: String? = null
    ) {
        viewModelScope.launch {
            _state.value = DiscoveryState.Loading

            var photoUrl: String? = null
            try {
                val imageRef = storage.reference.child("discoveries/${UUID.randomUUID()}.jpg")
                imageRef.putFile(imageUri).await()
                photoUrl = imageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                photoUrl = imageUri.toString()
            }

            // Call backend server's geofenced capture endpoint
            try {
                val response = withContext(Dispatchers.IO) {
                    monumentApi.captureMonument(
                        CaptureRequest(
                            monumentId = monumentId,
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            imageUrl = photoUrl
                        )
                    )
                }

                val captureData = response.data
                if (response.success && captureData != null && captureData.success) {
                    val points = captureData.pointsEarned ?: 100
                    val rarity = captureData.rarity ?: "COMMON"
                    val badge = when (rarity.uppercase()) {
                        "LEGENDARY" -> "✦ FIRST DISCOVERER (+1000 XP)"
                        "RARE" -> "🛡️ EARLY PIONEER (+600 XP)"
                        "UNCOMMON" -> "📍 ACTIVE EXPLORER (+300 XP)"
                        else -> "🏛️ POPULAR LANDMARK (+100 XP)"
                    }

                    val result = DiscoveryResult(
                        monumentName = captureData.monumentName ?: name,
                        previousUploadersCount = 0,
                        multiplier = 1.0f,
                        pointsEarned = points,
                        rarityBadge = badge,
                        message = captureData.message ?: "Monument captured successfully!"
                    )

                    // Write audit discovery record to Firestore if online
                    try {
                        val discovery = hashMapOf(
                            "name" to name,
                            "imageUrl" to photoUrl,
                            "timestamp" to System.currentTimeMillis(),
                            "userId" to (auth.currentUser?.uid ?: "anonymous"),
                            "pointsEarned" to points,
                            "rarityBadge" to badge
                        )
                        firestore.collection("discoveries").add(discovery)
                    } catch (e: Exception) {}

                    _state.value = DiscoveryState.Success(result)
                } else {
                    val errorMsg = captureData?.message
                        ?: response.error
                        ?: "Capture failed: You must be within 500 meters of the monument to capture it."
                    _state.value = DiscoveryState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = DiscoveryState.Error(
                    e.localizedMessage ?: "Server network error: Could not verify monument capture with server."
                )
            }
        }
    }
}
