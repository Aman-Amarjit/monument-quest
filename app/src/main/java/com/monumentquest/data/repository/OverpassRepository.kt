package com.monumentquest.data.repository

import com.monumentquest.data.model.BuildingFootprint
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.data.model.PartnerHotel
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

    suspend fun fetchTacticalGeometry(lat: Double, lon: Double, radiusMeters: Int = 900): TacticalGeometry = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:15];
            (
              way["highway"](around:$radiusMeters,$lat,$lon);
              way["building"](around:$radiusMeters,$lat,$lon);
            );
            out geom qt;
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
                val elements = json.optJSONArray("elements") ?: return@withContext TacticalGeometry(emptyList(), emptyList())

                val ways = mutableListOf<JSONObject>()
                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    if (elem.optString("type") == "way") ways.add(elem)
                }

                val roads = mutableListOf<List<GeoPoint>>()
                val buildings = mutableListOf<List<GeoPoint>>()

                for (way in ways) {
                    val points = mutableListOf<GeoPoint>()
                    val geometry = way.optJSONArray("geometry")
                    if (geometry != null) {
                        for (j in 0 until geometry.length()) {
                            val point = geometry.optJSONObject(j) ?: continue
                            val pointLat = point.optDouble("lat", Double.NaN)
                            val pointLon = point.optDouble("lon", Double.NaN)
                            if (!pointLat.isNaN() && !pointLon.isNaN()) {
                                points.add(GeoPoint(pointLat, pointLon))
                            }
                        }
                    }

                    if (points.isNotEmpty()) {
                        val tags = way.optJSONObject("tags")
                        if (tags?.has("building") == true) {
                            buildings.add(points)
                        } else if (tags?.has("highway") == true) {
                            roads.add(points)
                        }
                    }
                }
                return@withContext TacticalGeometry(
                    buildings = buildings.map { BuildingFootprint(it, BuildingFootprint.DEFAULT_LEVELS) },
                    roads     = roads.map     { RoadSegment(it, isMajor = true) }
                )
            }
        } catch (e: Exception) {
            // Error
        }
        TacticalGeometry(emptyList(), emptyList())
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

    suspend fun fetchRealHotelsNearby(lat: Double, lon: Double, radiusMeters: Int = 8000): List<PartnerHotel> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:15];
            (
              node["tourism"="hotel"](around:$radiusMeters,$lat,$lon);
              node["tourism"="guest_house"](around:$radiusMeters,$lat,$lon);
              node["tourism"="hostel"](around:$radiusMeters,$lat,$lon);
              node["tourism"="motel"](around:$radiusMeters,$lat,$lon);
            );
            out body 20;
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

                val items = mutableListOf<PartnerHotel>()
                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    val tags = elem.optJSONObject("tags") ?: continue
                    val name = tags.optString("name", tags.optString("name:en", ""))
                    if (name.isBlank()) continue

                    val nodeLat = elem.optDouble("lat", lat)
                    val nodeLon = elem.optDouble("lon", lon)
                    val category = tags.optString("tourism", "HOTEL").uppercase()

                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(lat, lon, nodeLat, nodeLon, results)
                    val dist = results[0].toInt()

                    items.add(
                        PartnerHotel(
                            id = "hotel_osm_" + elem.optLong("id", i.toLong()),
                            name = name,
                            category = category + " • NEARBY",
                            latitude = nodeLat,
                            longitude = nodeLon,
                            rating = 4.5,
                            pricePerNight = 2500 + (i * 300) % 4000,
                            discountPercent = 15 + (i * 5) % 15,
                            perkTitle = "Explorer Quest Pass (" + (15 + (i * 5) % 15) + "% OFF)",
                            perkDesc = "Exclusive Heritage Explorer perk for MonumentQuest travellers.",
                            xpRequired = 200 + (i * 50) % 300,
                            distanceMeters = dist
                        )
                    )
                }
                if (items.isNotEmpty()) return@withContext items.sortedBy { it.distanceMeters }
            }
        } catch (e: Exception) {
            // Fallback
        }
        return@withContext emptyList()
    }
}
