package com.monumentquest.data.model

data class DiscovererStory(
    val id: String = "",
    val userName: String = "",
    val avatarUrl: String = ""
)

data class SocialPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRank: String = "Explorer",
    val monumentName: String = "",
    val locationName: String = "",
    val imageUrl: String? = null,
    val caption: String = "",
    val postType: String = "CHECKIN", // DISCOVERY, CHECKIN, TIME_CAPSULE, REFLECTION
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val timestampFormatted: String = "15m ago"
)
