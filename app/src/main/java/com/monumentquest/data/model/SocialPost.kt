package com.monumentquest.data.model

data class DiscovererStory(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String? = null,
    val mediaUrl: String? = null,
    val caption: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val viewsCount: Int = 0,
    val isViewed: Boolean = false
)

data class SocialPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val userRank: String = "Explorer",
    val monumentName: String = "",
    val locationName: String = "",
    val imageUrl: String? = null,
    val caption: String = "",
    val postType: String = "CHECKIN",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isFollowing: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = 0L,
    val timestampFormatted: String = "Just now"
)
