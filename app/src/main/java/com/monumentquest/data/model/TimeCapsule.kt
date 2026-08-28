package com.monumentquest.data.model

data class TimeCapsule(
    val id: String = "",
    val monumentId: String = "",
    val authorName: String = "",
    val message: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
