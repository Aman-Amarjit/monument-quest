package com.monumentquest.data.model

data class Guild(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val description: String = "",
    val memberIds: List<String> = emptyList(),
    val totalPoints: Int = 0,
    val membersCount: Int = 0,
    val rank: Int = 0
)
