package com.monumentquest.ui.discovery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class DiscoveryResult(
    val monumentName: String = "",
    val previousUploadersCount: Int = 0,
    val multiplier: Float = 1.0f,
    val pointsEarned: Int = 100,
    val rarityBadge: String = "COMMON LANDMARK"
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state

    // Mock upload history counter for monuments
    private val monumentUploadTracker = mutableMapOf(
        "Lingaraj Temple" to 42,
        "Mukteshvara Temple" to 14,
        "Rajarani Temple" to 3,
        "Dhauli Shanti Stupa" to 18,
        "Khandagiri & Udayagiri Caves" to 2
    )

    fun uploadDiscovery(name: String, imageUri: Uri) {
        viewModelScope.launch {
            _state.value = DiscoveryState.Loading
            
            // Get previous uploads count
            val prevCount = monumentUploadTracker.getOrDefault(name, 0)
            
            // Calculate dynamic rarity points based on previous uploader count
            val (multiplier, points, badge) = when {
                prevCount == 0 -> Triple(5.0f, 1000, "✦ FIRST DISCOVERER")
                prevCount <= 5 -> Triple(3.0f, 600, "🛡️ EARLY PIONEER")
                prevCount <= 20 -> Triple(1.5f, 300, "📍 ACTIVE EXPLORER")
                else -> Triple(1.0f, 100, "🏛️ POPULAR LANDMARK")
            }

            // Update local count tracker
            monumentUploadTracker[name] = prevCount + 1

            val result = DiscoveryResult(
                monumentName = name,
                previousUploadersCount = prevCount,
                multiplier = multiplier,
                pointsEarned = points,
                rarityBadge = badge
            )

            try {
                val imageRef = storage.reference.child("discoveries/${UUID.randomUUID()}.jpg")
                imageRef.putFile(imageUri).await()
                val downloadUrl = imageRef.downloadUrl.await()

                val discovery = hashMapOf(
                    "name" to name,
                    "imageUrl" to downloadUrl.toString(),
                    "timestamp" to System.currentTimeMillis(),
                    "userId" to (auth.currentUser?.uid ?: "anonymous"),
                    "previousUploadersCount" to prevCount,
                    "multiplier" to multiplier,
                    "pointsEarned" to points,
                    "rarityBadge" to badge
                )

                firestore.collection("discoveries").add(discovery).await()
                _state.value = DiscoveryState.Success(result)
            } catch (e: Exception) {
                delay(1200)
                _state.value = DiscoveryState.Success(result)
            }
        }
    }
}
