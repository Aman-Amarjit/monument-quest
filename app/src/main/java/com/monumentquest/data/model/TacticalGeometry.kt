package com.monumentquest.data.model

import org.osmdroid.util.GeoPoint

data class TacticalGeometry(
    val roads: List<List<GeoPoint>>,
    val buildings: List<List<GeoPoint>>
)
