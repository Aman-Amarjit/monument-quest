package com.monumentquest.data.model

data class ProfileGuild(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val description: String = ""
)

data class UserProfile(
    val id: String = "user_1",
    val name: String = "Explorer Prime",
    val email: String = "explorer@monumentquest.com",
    val xp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val visitedCount: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val areaUnlockedKm2: Double = 0.0,
    val avatarUrl: String? = null,
    val guild: ProfileGuild? = null
)
