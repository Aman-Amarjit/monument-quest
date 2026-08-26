package com.monumentquest.data.model

data class Monument(
    val id: String = "",
    val name: String = "",
    val category: String = "HERITAGE",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val points: Int = 0,
    val distanceMeters: Int = 0,
    val custodianId: String? = null,
    val totalContributionPoints: Int = 0
)
