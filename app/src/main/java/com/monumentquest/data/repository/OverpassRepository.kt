package com.monumentquest.data.repository

import com.monumentquest.data.model.AreaFeature
import com.monumentquest.data.model.BuildingFootprint
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.data.model.RoadSegment
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
              way["leisure"~"^(park|garden)$"](around:$radiusMeters,$lat,$lon);
              way["landuse"="grass"](around:$radiusMeters,$lat,$lon);
              way["natural"="water"](around:$radiusMeters,$lat,$lon);
              way["waterway"="riverbank"](around:$radiusMeters,$lat,$lon);
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
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val elements = json.optJSONArray("elements") ?: return@withContext generateProceduralCity(lat, lon)

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

                val roads = mutableListOf<RoadSegment>()
                val buildings = mutableListOf<BuildingFootprint>()
                val parks = mutableListOf<AreaFeature>()
                val water = mutableListOf<AreaFeature>()

                for (way in ways) {
                    val nodeIds = way.optJSONArray("nodes") ?: continue
                    val points = mutableListOf<GeoPoint>()
                    for (j in 0 until nodeIds.length()) {
                        nodesMap[nodeIds.getLong(j)]?.let { points.add(it) }
                    }

                    if (points.isNotEmpty()) {
                        val tags = way.optJSONObject("tags") ?: JSONObject()
                        when {
                            tags.has("building") -> {
                                val levels = tags.optString("building:levels", "").toIntOrNull() ?: 4
                                buildings.add(BuildingFootprint(points, levels))
                            }
                            tags.has("highway") -> {
                                val hType = tags.optString("highway", "")
                                val isMajor = hType in setOf("motorway", "trunk", "primary", "secondary", "tertiary")
                                roads.add(RoadSegment(points, isMajor))
                            }
                            tags.optString("leisure") in setOf("park", "garden") || tags.optString("landuse") == "grass" -> {
                                parks.add(AreaFeature(points))
                            }
                            tags.optString("natural") == "water" || tags.optString("waterway") == "riverbank" -> {
                                water.add(AreaFeature(points))
                            }
                        }
                    }
                }

                if (buildings.isNotEmpty()) {
                    return@withContext TacticalGeometry(buildings = buildings, roads = roads, parks = parks, water = water)
                }
            }
        } catch (e: Exception) {
            // Fallback to dense 3D procedural layout matching reference image
        }
        return@withContext generateProceduralCity(lat, lon)
    }

    private fun generateProceduralCity(lat: Double, lon: Double): TacticalGeometry {
        val buildings = mutableListOf<BuildingFootprint>()
        val roads = mutableListOf<RoadSegment>()
        val parks = mutableListOf<AreaFeature>()
        val water = mutableListOf<AreaFeature>()

        // 1. Water Canals
        val canal1 = listOf(
            GeoPoint(lat - 0.008, lon - 0.005),
            GeoPoint(lat - 0.008, lon + 0.008),
            GeoPoint(lat - 0.0072, lon + 0.008),
            GeoPoint(lat - 0.0072, lon - 0.005)
        )
        val canal2 = listOf(
            GeoPoint(lat - 0.008, lon + 0.004),
            GeoPoint(lat + 0.008, lon + 0.004),
            GeoPoint(lat + 0.008, lon + 0.0048),
            GeoPoint(lat - 0.008, lon + 0.0048)
        )
        water.add(AreaFeature(canal1))
        water.add(AreaFeature(canal2))

        // 2. Green Parks & Lawns
        val park1 = listOf(
            GeoPoint(lat + 0.002, lon - 0.006),
            GeoPoint(lat + 0.006, lon - 0.006),
            GeoPoint(lat + 0.006, lon - 0.002),
            GeoPoint(lat + 0.002, lon - 0.002)
        )
        val park2 = listOf(
            GeoPoint(lat - 0.006, lon - 0.006),
            GeoPoint(lat - 0.002, lon - 0.006),
            GeoPoint(lat - 0.002, lon - 0.001),
            GeoPoint(lat - 0.006, lon - 0.001)
        )
        parks.add(AreaFeature(park1))
        parks.add(AreaFeature(park2))

        // 3. Golden Highways & Overpasses
        val highway1 = listOf(
            GeoPoint(lat - 0.007, lon - 0.007),
            GeoPoint(lat - 0.002, lon - 0.002),
            GeoPoint(lat + 0.001, lon + 0.001),
            GeoPoint(lat + 0.004, lon + 0.007)
        )
        val highway2 = listOf(
            GeoPoint(lat + 0.005, lon - 0.007),
            GeoPoint(lat + 0.001, lon - 0.001),
            GeoPoint(lat - 0.003, lon + 0.005)
        )
        roads.add(RoadSegment(highway1, isMajor = true))
        roads.add(RoadSegment(highway2, isMajor = true))

        // 4. Dense 3D Extruded Building Grid matrix (60+ blocks around user location)
        val blockSizes = listOf(0.0007 to 0.0006, 0.0009 to 0.0005, 0.0005 to 0.0008, 0.0010 to 0.0007)
        val levelsList = listOf(4, 7, 3, 9, 5, 12, 6, 8, 4, 10, 5, 3, 11, 7, 4)

        var count = 0
        for (row in -4..4) {
            for (col in -4..4) {
                if (row == 0 || col == 0) continue
                if (row == -3 && col < 0) continue

                val bLat = lat + row * 0.0015
                val bLon = lon + col * 0.0015
                val (w, h) = blockSizes[count % blockSizes.size]
                val lvl = levelsList[count % levelsList.size]
                count++

                val footprint = listOf(
                    GeoPoint(bLat, bLon),
                    GeoPoint(bLat + h, bLon),
                    GeoPoint(bLat + h, bLon + w),
                    GeoPoint(bLat, bLon + w)
                )
                buildings.add(BuildingFootprint(footprint, lvl))
            }
        }

        return TacticalGeometry(buildings = buildings, roads = roads, parks = parks, water = water)
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
