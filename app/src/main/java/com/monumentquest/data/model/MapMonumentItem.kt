package com.monumentquest.data.model

import org.osmdroid.util.GeoPoint

data class MapMonumentItem(
    val id: String = "",
    val name: String = "",
    val locationName: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val points: Int = 0,
    val category: String = "",
    val distanceMeters: Int = 0
)
