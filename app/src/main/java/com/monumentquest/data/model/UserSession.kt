package com.monumentquest.data.model

data class UserSession(
    val uid: String = "",
    val name: String = "Guest Explorer",
    val email: String = "guest@monumentquest.app",
    val userRank: String = "Novice Discoverer",
    val points: Int = 100,
    val isGuest: Boolean = true,
    val guildName: String = "Independent Explorer"
)
