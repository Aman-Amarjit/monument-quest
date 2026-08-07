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
                if (list.isNotEmpty()) {
                    _guilds.value = list
                } else {
                    _guilds.value = getSampleGuilds()
                }
            } catch (e: Exception) {
                _guilds.value = getSampleGuilds()
            }
        }
    }

    private fun fetchUserGuild() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: "sample_user"
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
                    val sample = getSampleGuilds().first()
                    _userGuild.value = sample
                    fetchGuildLeaderboard(sample.memberIds)
                }
            } catch (e: Exception) {
                val sample = getSampleGuilds().first()
                _userGuild.value = sample
                fetchGuildLeaderboard(sample.memberIds)
            }
        }
    }

    private fun fetchGuildLeaderboard(memberIds: List<String>) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereIn("id", memberIds.take(10)) 
                    .orderBy("points", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(User::class.java)
                if (list.isNotEmpty()) {
                    _guildLeaderboard.value = list
                } else {
                    _guildLeaderboard.value = getSampleGuildMembers()
                }
            } catch (e: Exception) {
                _guildLeaderboard.value = getSampleGuildMembers()
            }
        }
    }

    fun joinGuild(guildId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: "sample_user"
                
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
            } catch (e: Exception) {
                val joined = _guilds.value.find { it.id == guildId }
                if (joined != null) {
                    _userGuild.value = joined
                    _guildLeaderboard.value = getSampleGuildMembers()
                }
            }
        }
    }

    private fun getSampleGuilds(): List<Guild> {
        return listOf(
            Guild(id = "g1", name = "Arch-Historians of London", region = "Europe", memberIds = listOf("1", "2", "6"), totalPoints = 14200),
            Guild(id = "g2", name = "Alpine Heritage Order", region = "Central Europe", memberIds = listOf("3", "5"), totalPoints = 11800),
            Guild(id = "g3", name = "Pacific Landmark Watch", region = "Asia-Pacific", memberIds = listOf("4", "7"), totalPoints = 9400),
            Guild(id = "g4", name = "Americas Discovery League", region = "Americas", memberIds = listOf("8"), totalPoints = 7600)
        )
    }

    private fun getSampleGuildMembers(): List<User> {
        return listOf(
            User(id = "1", name = "Elena Rostova", points = 3420),
            User(id = "2", name = "Arthur Pendelton", points = 2890),
            User(id = "6", name = "Adventurer (You)", points = 850)
        )
    }
}
