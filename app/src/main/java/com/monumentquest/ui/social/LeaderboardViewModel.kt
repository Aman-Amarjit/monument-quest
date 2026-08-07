package com.monumentquest.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.monumentquest.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    init {
        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val userList = snapshot.toObjects(User::class.java)
                if (userList.isNotEmpty()) {
                    _users.value = userList
                } else {
                    _users.value = getSampleUsers()
                }
            } catch (e: Exception) {
                _users.value = getSampleUsers()
            }
        }
    }

    private fun getSampleUsers(): List<User> {
        return listOf(
            User(id = "1", name = "Elena Rostova", points = 3420, badges = listOf("First Discoverer", "Master Explorer")),
            User(id = "2", name = "Arthur Pendelton", points = 2890, badges = listOf("History Buff", "Scholar")),
            User(id = "3", name = "Amina Bello", points = 2450, badges = listOf("Nature Lover", "Mountaineer")),
            User(id = "4", name = "Kenji Sato", points = 1980, badges = listOf("Ancient Relic")),
            User(id = "5", name = "Carlos Silva", points = 1620, badges = listOf("Global Wanderer")),
            User(id = "6", name = "Adventurer (You)", points = 850, badges = listOf("First Discovery", "Nature Lover")),
            User(id = "7", name = "Sophia Chen", points = 720, badges = listOf("Novice")),
            User(id = "8", name = "Mateo Rossi", points = 540, badges = listOf("Pathfinder"))
        )
    }
}
