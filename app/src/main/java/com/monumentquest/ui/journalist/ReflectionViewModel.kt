package com.monumentquest.ui.journalist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.monumentquest.data.model.GroqMessage
import com.monumentquest.data.model.GroqRequest
import com.monumentquest.data.remote.GroqApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

sealed class ReflectionState {
    object Writing : ReflectionState()
    object Verifying : ReflectionState()
    data class Success(val score: String) : ReflectionState()
    data class Error(val message: String) : ReflectionState()
}

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val groqApi: GroqApi,
    @Named("groq_api_key") private val apiKey: String,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<ReflectionState>(ReflectionState.Writing)
    val state: StateFlow<ReflectionState> = _state

    fun verifyReflection(monumentName: String, content: String) {
        viewModelScope.launch {
            _state.value = ReflectionState.Verifying
            try {
                if (apiKey.isNotBlank() && apiKey != "dummy_key") {
                    val prompt = "Review this historical reflection about $monumentName: '$content'. Verify if it is historically accurate and high quality. Respond with 'VALID - [Score]' or 'INVALID - [Reason]'."
                    
                    val request = GroqRequest(
                        model = "llama-3.3-70b-versatile",
                        messages = listOf(GroqMessage("user", prompt))
                    )

                    val response = groqApi.getChatCompletion(apiKey, request)
                    val responseText = response.choices.firstOrNull()?.message?.content ?: ""

                    if (responseText.contains("VALID", ignoreCase = true)) {
                        val scoreString = responseText.substringAfter("VALID - ").trim().takeWhile { it.isDigit() }
                        rewardUser(scoreString)
                        _state.value = ReflectionState.Success(scoreString)
                    } else {
                        val reason = responseText.substringAfter("INVALID - ").trim()
                        _state.value = ReflectionState.Error(reason.ifEmpty { "Verification could not confirm accuracy." })
                    }
                } else {
                    delay(1500)
                    val calculatedScore = calculateLocalReflectionScore(content)
                    _state.value = ReflectionState.Success(calculatedScore.toString())
                }
            } catch (e: Exception) {
                delay(1200)
                val calculatedScore = calculateLocalReflectionScore(content)
                _state.value = ReflectionState.Success(calculatedScore.toString())
            }
        }
    }

    private fun calculateLocalReflectionScore(content: String): Int {
        val wordCount = content.split("\\s+".toRegex()).size
        return when {
            wordCount > 30 -> 92
            wordCount > 15 -> 84
            else -> 75
        }
    }

    private suspend fun rewardUser(score: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val userRef = firestore.collection("users").document(userId)
            val pointsToAdd = score.toIntOrNull() ?: 50
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("points") ?: 0
                transaction.update(userRef, "points", currentPoints + pointsToAdd)
            }.await()
        } catch (e: Exception) {
            // Ignore transaction failure when offline
        }
    }
    
    fun resetToWriting() {
        _state.value = ReflectionState.Writing
    }
}
