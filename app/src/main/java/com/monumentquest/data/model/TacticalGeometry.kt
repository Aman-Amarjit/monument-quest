package com.monumentquest.data.model

import org.osmdroid.util.GeoPoint

/**
 * Enhanced geometry snapshot used to paint the stylised "extruded city"
 * look on top of base map tiles — grey building blocks, gold arterial roads,
 * green parks, and blue water features.
 */
data class TacticalGeometry(
    val buildings: List<BuildingFootprint> = emptyList(),
    val roads: List<RoadSegment> = emptyList(),
    val parks: List<AreaFeature> = emptyList(),
    val water: List<AreaFeature> = emptyList()
)

/**
 * A single building outline with floor levels for pseudo-3D extrusion.
 */
data class BuildingFootprint(
    val outline: List<GeoPoint>,
    val levels: Int = DEFAULT_LEVELS
) {
    companion object {
        const val DEFAULT_LEVELS = 3
    }
}

/**
 * A road centreline. Only major roads get bold gold styling.
 */
data class RoadSegment(
    val points: List<GeoPoint>,
    val isMajor: Boolean = false
)

/** Filled area feature — used for both parks and water bodies. */
data class AreaFeature(
    val outline: List<GeoPoint>
)
