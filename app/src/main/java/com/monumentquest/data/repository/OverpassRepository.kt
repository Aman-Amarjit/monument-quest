package com.monumentquest.data.repository

import com.monumentquest.data.model.MapMonumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverpassRepository @Inject constructor() {

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
