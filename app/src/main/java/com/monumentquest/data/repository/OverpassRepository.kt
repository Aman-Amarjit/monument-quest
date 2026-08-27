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
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

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

    suspend fun fetchRealMonumentsNearby(lat: Double, lon: Double, radiusMeters: Int = 8000): List<MapMonumentItem> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:15];
            (
              node["historic"](around:$radiusMeters,$lat,$lon);
              node["tourism"="attraction"](around:$radiusMeters,$lat,$lon);
              node["tourism"="museum"](around:$radiusMeters,$lat,$lon);
              node["tourism"="viewpoint"](around:$radiusMeters,$lat,$lon);
              node["amenity"="place_of_worship"](around:$radiusMeters,$lat,$lon);
              node["leisure"="park"](around:$radiusMeters,$lat,$lon);
              node["amenity"="library"](around:$radiusMeters,$lat,$lon);
            );
            out body 60;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter?data=$encodedQuery",
            "https://overpass.kumi.systems/api/interpreter?data=$encodedQuery"
        )

        for (endpointUrl in endpoints) {
            try {
                val url = URL(endpointUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val elements = json.optJSONArray("elements") ?: continue

                    val seen = mutableSetOf<String>()
                    val items = mutableListOf<MapMonumentItem>()

                    for (i in 0 until elements.length()) {
                        val elem = elements.getJSONObject(i)
                        val tags = elem.optJSONObject("tags") ?: continue

                        val name = tags.optString("name:en", tags.optString("name", "")).trim()
                        if (name.isBlank() || seen.contains(name)) continue
                        seen.add(name)

                        val nodeLat = elem.optDouble("lat", Double.NaN)
                        val nodeLon = elem.optDouble("lon", Double.NaN)
                        if (nodeLat.isNaN() || nodeLon.isNaN()) continue

                        val category = when {
                            tags.has("historic") -> tags.optString("historic", "HISTORIC").replace("_", " ").uppercase()
                            tags.optString("tourism") == "museum" -> "MUSEUM"
                            tags.optString("tourism") == "attraction" -> "ATTRACTION"
                            tags.optString("tourism") == "viewpoint" -> "VIEWPOINT"
                            tags.has("amenity") && tags.optString("amenity") == "place_of_worship" -> "PLACE OF WORSHIP"
                            tags.optString("leisure") == "park" -> "PUBLIC PARK"
                            else -> "HERITAGE LANDMARK"
                        }

                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(lat, lon, nodeLat, nodeLon, results)
                        val dist = results[0].toInt()

                        items.add(
                            MapMonumentItem(
                                id = "osm_" + elem.optLong("id", i.toLong()),
                                name = name,
                                locationName = tags.optString("addr:city", tags.optString("addr:suburb", "Local Area")),
                                geoPoint = GeoPoint(nodeLat, nodeLon),
                                points = if (tags.has("historic")) 500 else 300,
                                category = category,
                                distanceMeters = dist
                            )
                        )
                    }

                    if (items.isNotEmpty()) {
                        return@withContext items.sortedBy { it.distanceMeters }
                    }
                }
            } catch (_: Exception) {}
        }

        // Guaranteed Rich Heritage & Public Places Fallback
        return@withContext getFallbackPublicPlaces(lat, lon)
    }

    private fun getFallbackPublicPlaces(userLat: Double, userLon: Double): List<MapMonumentItem> {
        val defaultList = listOf(
            Triple(20.2381, 85.8338, Pair("Lingaraj Temple", "HISTORIC TEMPLE")),
            Triple(20.2435, 85.8327, Pair("Mukteshvara Temple", "ANCIENT SHRINE")),
            Triple(20.2514, 85.8436, Pair("Raja Rani Temple", "HERITAGE MONUMENT")),
            Triple(20.1924, 85.8394, Pair("Dhauli Peace Pagoda", "MEMORIAL PARK")),
            Triple(20.2631, 85.7864, Pair("Khandagiri & Udayagiri Caves", "ARCHAEOLOGICAL SITE")),
            Triple(20.2742, 85.8272, Pair("Ekamra Kanan Botanical Park", "PUBLIC PARK")),
            Triple(20.2541, 85.8351, Pair("Odisha State Museum", "MUSEUM")),
            Triple(20.2901, 85.8456, Pair("Biju Patnaik Park", "PUBLIC PARK")),
            Triple(20.2452, 85.8360, Pair("Parsurameswara Temple", "HISTORIC TEMPLE")),
            Triple(20.2605, 85.8402, Pair("Nicco Park & Lake", "PUBLIC RECREATION"))
        )

        return defaultList.mapIndexed { idx, (lLat, lLon, meta) ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(userLat, userLon, lLat, lLon, results)
            MapMonumentItem(
                id = "public_site_$idx",
                name = meta.first,
                locationName = "Bhubaneswar, Odisha",
                geoPoint = GeoPoint(lLat, lLon),
                points = 500,
                category = meta.second,
                distanceMeters = results[0].toInt()
            )
        }.sortedBy { it.distanceMeters }
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
                    val name = tags.optString("name", tags.optString("name:en", "")).trim()
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
