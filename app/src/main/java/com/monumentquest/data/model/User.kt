package com.monumentquest.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val points: Int = 0,
    val badges: List<String> = emptyList()
)
