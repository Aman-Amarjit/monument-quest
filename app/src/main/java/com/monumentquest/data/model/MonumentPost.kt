package com.monumentquest.data.model

data class MonumentPost(
    val id: String = "",
    val monumentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
