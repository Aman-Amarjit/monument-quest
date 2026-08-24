package com.monumentquest.ui.map

import com.monumentquest.data.model.AreaFeature
import com.monumentquest.data.model.BuildingFootprint
import com.monumentquest.data.model.RoadSegment
import com.monumentquest.data.model.TacticalGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pulls building / major-road / park / water geometry from the Overpass
 * API for a given viewport and turns it into a [TacticalGeometry] that
 * [Isometric3DOverlay] can paint.
 */
object TacticalGeometryFetcher {

    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    suspend fun fetch(bounds: BoundingBox): TacticalGeometry = withContext(Dispatchers.IO) {
        val bbox = "${bounds.latSouth},${bounds.lonWest},${bounds.latNorth},${bounds.lonEast}"
        val query = """
            [out:json][timeout:20];
            (
              way["building"]($bbox);
              way["highway"~"^(motorway|trunk|primary|secondary|tertiary)$"]($bbox);
              way["leisure"~"^(park|garden)$"]($bbox);
              way["landuse"="grass"]($bbox);
              way["natural"="water"]($bbox);
              way["waterway"="riverbank"]($bbox);
            );
            out body geom;
        """.trimIndent()

        val json = try {
            postQuery(query)
        } catch (e: Exception) {
            return@withContext TacticalGeometry()
        }

        val buildings = mutableListOf<BuildingFootprint>()
        val roads = mutableListOf<RoadSegment>()
        val parks = mutableListOf<AreaFeature>()
        val water = mutableListOf<AreaFeature>()

        val elements = json.optJSONArray("elements") ?: JSONArray()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val geomArray = el.optJSONArray("geometry") ?: continue
            val points = ArrayList<GeoPoint>(geomArray.length())
            for (j in 0 until geomArray.length()) {
                val g = geomArray.optJSONObject(j) ?: continue
                points.add(GeoPoint(g.getDouble("lat"), g.getDouble("lon")))
            }
            if (points.size < 2) continue

            val tags = el.optJSONObject("tags") ?: JSONObject()
            when {
                tags.has("building") -> {
                    val levels = tags.optString("building:levels", "")
                        .toDoubleOrNull()?.toInt()
                        ?: BuildingFootprint.DEFAULT_LEVELS
                    buildings.add(BuildingFootprint(points, levels))
                }
                tags.has("highway") -> {
                    roads.add(RoadSegment(points, isMajor = true))
                }
                tags.optString("leisure") in setOf("park", "garden") ||
                    tags.optString("landuse") == "grass" -> {
                    parks.add(AreaFeature(points))
                }
                tags.optString("natural") == "water" ||
                    tags.optString("waterway") == "riverbank" -> {
                    water.add(AreaFeature(points))
                }
            }
        }

        TacticalGeometry(buildings = buildings, roads = roads, parks = parks, water = water)
    }

    private fun postQuery(query: String): JSONObject {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        connection.outputStream.use {
            it.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray())
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(body)
    }
}
