package com.monumentquest.data.repository

import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.data.model.TacticalGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverpassRepository @Inject constructor() {

    suspend fun fetchTacticalGeometry(lat: Double, lon: Double, radiusMeters: Int = 1200): TacticalGeometry = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:20];
            (
              way["highway"](around:$radiusMeters,$lat,$lon);
              way["building"](around:$radiusMeters,$lat,$lon);
            );
            out body;
            >;
            out skel qt;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val overpassUrl = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

        try {
            val url = URL(overpassUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val elements = json.optJSONArray("elements") ?: return@withContext TacticalGeometry()

                val nodesMap = mutableMapOf<Long, GeoPoint>()
                val ways = mutableListOf<JSONObject>()

                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    when (elem.optString("type")) {
                        "node" -> {
                            val id = elem.optLong("id")
                            val nLat = elem.optDouble("lat")
                            val nLon = elem.optDouble("lon")
                            nodesMap[id] = GeoPoint(nLat, nLon)
                        }
                        "way" -> ways.add(elem)
                    }
                }

                val roads = mutableListOf<com.monumentquest.data.model.RoadSegment>()
                val buildings = mutableListOf<com.monumentquest.data.model.BuildingFootprint>()

                for (way in ways) {
                    val nodeIds = way.optJSONArray("nodes") ?: continue
                    val points = mutableListOf<GeoPoint>()
                    for (j in 0 until nodeIds.length()) {
                        nodesMap[nodeIds.getLong(j)]?.let { points.add(it) }
                    }

                    if (points.isNotEmpty()) {
                        val tags = way.optJSONObject("tags")
                        if (tags?.has("building") == true) {
                            val levels = tags.optString("building:levels", "").toIntOrNull() ?: 3
                            buildings.add(com.monumentquest.data.model.BuildingFootprint(points, levels))
                        } else if (tags?.has("highway") == true) {
                            val hType = tags.optString("highway", "")
                            val isMajor = hType in setOf("motorway", "trunk", "primary", "secondary", "tertiary")
                            roads.add(com.monumentquest.data.model.RoadSegment(points, isMajor))
                        }
                    }
                }
                return@withContext TacticalGeometry(buildings = buildings, roads = roads)
            }
        } catch (e: Exception) {
            // Error
        }
        TacticalGeometry()
    }

    suspend fun fetchRealMonumentsNearby(lat: Double, lon: Double, radiusMeters: Int = 10000): List<MapMonumentItem> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:15];
            (
              node["historic"](around:$radiusMeters,$lat,$lon);
              way["historic"](around:$radiusMeters,$lat,$lon);
              node["tourism"="attraction"](around:$radiusMeters,$lat,$lon);
              node["amenity"="place_of_worship"](around:$radiusMeters,$lat,$lon);
            );
            out body 15;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val overpassUrl = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

        try {
            val url = URL(overpassUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val elements = json.optJSONArray("elements") ?: return@withContext emptyList()

                val items = mutableListOf<MapMonumentItem>()
                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    val tags = elem.optJSONObject("tags") ?: continue

                    val name = tags.optString("name", tags.optString("name:en", ""))
                    if (name.isBlank()) continue

                    val nodeLat = elem.optDouble("lat", lat)
                    val nodeLon = elem.optDouble("lon", lon)
                    val historicType = tags.optString("historic", tags.optString("amenity", "Heritage Landmark"))

                    items.add(
                        MapMonumentItem(
                            id = "osm_" + elem.optLong("id", i.toLong()),
                            name = name,
                            locationName = tags.optString("addr:city", "Local Region"),
                            geoPoint = GeoPoint(nodeLat, nodeLon),
                            points = 500,
                            category = historicType.replace("_", " ").uppercase(),
                            distanceMeters = 0
                        )
                    )
                }
                if (items.isNotEmpty()) return@withContext items
            }
        } catch (e: Exception) {
            // Fallback
        }
        return@withContext emptyList()
    }
}
