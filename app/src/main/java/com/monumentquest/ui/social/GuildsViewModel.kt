package com.monumentquest.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.monumentquest.data.model.Guild
import com.monumentquest.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class GuildsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _guilds = MutableStateFlow<List<Guild>>(emptyList())
    val guilds: StateFlow<List<Guild>> = _guilds

    private val _userGuild = MutableStateFlow<Guild?>(null)
    val userGuild: StateFlow<Guild?> = _userGuild

    private val _guildLeaderboard = MutableStateFlow<List<User>>(emptyList())
    val guildLeaderboard: StateFlow<List<User>> = _guildLeaderboard

    init {
        fetchGuilds()
        fetchUserGuild()
    }

    private fun fetchGuilds() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("guilds")
                    .orderBy("totalPoints", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(Guild::class.java)
                _guilds.value = list
            } catch (e: Exception) {
                _guilds.value = emptyList()
            }
        }
    }

    private fun fetchUserGuild() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                if (userId.isBlank()) {
                    _userGuild.value = null
                    return@launch
                }
                val snapshot = firestore.collection("guilds")
                    .whereArrayContains("memberIds", userId)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val guild = snapshot.documents[0].toObject(Guild::class.java)
                    _userGuild.value = guild
                    if (guild != null) {
                        fetchGuildLeaderboard(guild.memberIds)
                    }
                } else {
                    _userGuild.value = null
                    _guildLeaderboard.value = emptyList()
                }
            } catch (e: Exception) {
                _userGuild.value = null
                _guildLeaderboard.value = emptyList()
            }
        }
    }

    private fun fetchGuildLeaderboard(memberIds: List<String>) {
        if (memberIds.isEmpty()) {
            _guildLeaderboard.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereIn("id", memberIds.take(10)) 
                    .orderBy("points", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(User::class.java)
                _guildLeaderboard.value = list
            } catch (e: Exception) {
                _guildLeaderboard.value = emptyList()
            }
        }
    }

    fun joinGuild(guildId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                val oldGuildSnapshot = firestore.collection("guilds")
                    .whereArrayContains("memberIds", userId)
                    .get()
                    .await()
                
                for (doc in oldGuildSnapshot.documents) {
                    doc.reference.update("memberIds", FieldValue.arrayRemove(userId)).await()
                }

                firestore.collection("guilds").document(guildId)
                    .update("memberIds", FieldValue.arrayUnion(userId))
                    .await()
                
                fetchGuilds()
                fetchUserGuild()
            } catch (e: Exception) {}
        }
    }
}
