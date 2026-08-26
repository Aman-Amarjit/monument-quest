package com.monumentquest.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monumentquest.data.model.User
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { monumentApi.getLeaderboard() }
                val items = res.data ?: res.leaderboard ?: emptyList()
                if (items.isNotEmpty()) {
                    val converted = items.mapIndexed { idx, entry ->
                        User(
                            id = entry.id ?: "user_${idx + 1}",
                            name = entry.name,
                            points = entry.xp,
                            badges = listOf(entry.badge)
                        )
                    }
                    _users.value = converted
                } else {
                    _users.value = emptyList()
                }
            } catch (e: Exception) {
                _users.value = emptyList()
            }
        }
    }
}
