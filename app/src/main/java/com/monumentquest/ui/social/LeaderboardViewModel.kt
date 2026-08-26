package com.monumentquest.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monumentquest.data.model.User
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val monumentApi: MonumentApi
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    init {
        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                val res = monumentApi.getLeaderboard()
                if (res.leaderboard.isNotEmpty()) {
                    val converted = res.leaderboard.mapIndexed { idx, entry ->
                        User(
                            id = "user_${idx + 1}",
                            name = entry.name,
                            points = entry.xp,
                            badges = listOf(entry.badge)
                        )
                    }
                    _users.value = converted
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
            User(id = "1", name = "Aarav Sharma", points = 4850, badges = listOf("Grand Master")),
            User(id = "2", name = "Priya Patel", points = 3920, badges = listOf("Master Explorer")),
            User(id = "3", name = "Explorer Prime (You)", points = 1250, badges = listOf("Heritage Scout")),
            User(id = "4", name = "Vikram Singh", points = 1100, badges = listOf("Scout")),
            User(id = "5", name = "Ananya Roy", points = 950, badges = listOf("Novice"))
        )
    }
}
