package com.monumentquest.ui.custodian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.monumentquest.data.model.Monument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CustodianViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _heritageCoins = MutableStateFlow(0)
    val heritageCoins: StateFlow<Int> = _heritageCoins

    private val _currentCustodianId = MutableStateFlow<String?>(null)
    val currentCustodianId: StateFlow<String?> = _currentCustodianId

    fun checkCustodianStatus(monumentId: String) {
        viewModelScope.launch {
            try {
                // Fetch the monument to see the current custodian
                val monumentDoc = firestore.collection("monuments").document(monumentId).get().await()
                val monument = monumentDoc.toObject(Monument::class.java)
                _currentCustodianId.value = monument?.custodianId

                // Find the top contributor for this monument
                val topContribution = firestore.collection("contributions")
                    .whereEqualTo("monumentId", monumentId)
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

                if (!topContribution.isEmpty) {
                    val topContributorId = topContribution.documents[0].getString("userId")
                    val topPoints = topContribution.documents[0].getLong("points")?.toInt() ?: 0

                    // If there's a new top contributor, update the custodian
                    if (topContributorId != null && topContributorId != monument?.custodianId) {
                        firestore.collection("monuments").document(monumentId)
                            .update("custodianId", topContributorId, "totalContributionPoints", topPoints)
                            .await()
                        _currentCustodianId.value = topContributorId
                    }
                }
                
                // Heritage coins could be calculated based on time or contributions
                // For simplicity, let's say it's related to total points
                _heritageCoins.value = (monument?.totalContributionPoints ?: 0) / 10
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
