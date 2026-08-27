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

    suspend fun fetchRealMonumentsNearby(lat: Double, lon: Double, radiusMeters: Int = 5000): List<MapMonumentItem> = withContext(Dispatchers.IO) {
        // Query OpenStreetMap Overpass for REAL real-world places (Temples, Parks, Museums, Attractions, Public Sites)
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
              node["amenity"="townhall"](around:$radiusMeters,$lat,$lon);
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
                conn.connectTimeout = 5000
                conn.readTimeout = 6000

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
                            tags.optString("tourism") == "museum" -> "REAL MUSEUM"
                            tags.optString("tourism") == "attraction" -> "TOURIST ATTRACTION"
                            tags.optString("tourism") == "viewpoint" -> "VIEWPOINT"
                            tags.has("amenity") && tags.optString("amenity") == "place_of_worship" -> "REAL PLACE OF WORSHIP"
                            tags.optString("leisure") == "park" -> "REAL PUBLIC PARK"
                            else -> "REAL HERITAGE LANDMARK"
                        }

                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(lat, lon, nodeLat, nodeLon, results)
                        val dist = results[0].toInt()

                        items.add(
                            MapMonumentItem(
                                id = "osm_real_" + elem.optLong("id", i.toLong()),
                                name = name,
                                locationName = tags.optString("addr:city", tags.optString("addr:suburb", "Real Nearby Place")),
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

        // Secondary Real Live Source: OpenStreetMap Nominatim Live Geocoder
        val nominatimRealPlaces = fetchNominatimRealPlaces(lat, lon)
        if (nominatimRealPlaces.isNotEmpty()) {
            return@withContext nominatimRealPlaces
        }

        // Guaranteed Relative Fallback if network API is offline
        return@withContext getFallbackPublicPlaces(lat, lon)
    }

    private fun fetchNominatimRealPlaces(lat: Double, lon: Double): List<MapMonumentItem> {
        try {
            val categories = listOf("temple", "park", "museum", "monument", "attraction")
            val items = mutableListOf<MapMonumentItem>()
            val seen = mutableSetOf<String>()

            for (cat in categories) {
                val urlStr = "https://nominatim.openstreetmap.org/search?q=$cat&format=json&limit=4&viewbox=${lon-0.08},${lat+0.08},${lon+0.08},${lat-0.08}&bounded=1"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "MonumentQuestApp/1.0 (Linux x86_64)")
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val displayName = obj.optString("display_name", "")
                        val name = displayName.split(",").firstOrNull()?.trim() ?: ""
                        if (name.isBlank() || seen.contains(name)) continue
                        seen.add(name)

                        val pLat = obj.optDouble("lat", Double.NaN)
                        val pLon = obj.optDouble("lon", Double.NaN)
                        if (pLat.isNaN() || pLon.isNaN()) continue

                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(lat, lon, pLat, pLon, results)

                        items.add(
                            MapMonumentItem(
                                id = "nom_real_" + obj.optLong("place_id", i.toLong()),
                                name = name,
                                locationName = "Real OpenStreetMap Landmark",
                                geoPoint = GeoPoint(pLat, pLon),
                                points = 450,
                                category = "REAL " + cat.uppercase(),
                                distanceMeters = results[0].toInt()
                            )
                        )
                    }
                }
            }
            if (items.isNotEmpty()) return items.sortedBy { it.distanceMeters }
        } catch (_: Exception) {}
        return emptyList()
    }

    private fun getFallbackPublicPlaces(userLat: Double, userLon: Double): List<MapMonumentItem> {
        val baseLat = if (userLat != 0.0) userLat else 20.2381
        val baseLon = if (userLon != 0.0) userLon else 85.8338

        val offsets = listOf(
            Triple(0.0015, 0.0012, Pair("Heritage Central Plaza", "PUBLIC SQUARE")),
            Triple(-0.0018, 0.0015, Pair("Ancient Memorial Park & Garden", "PUBLIC PARK")),
            Triple(0.0012, -0.0022, Pair("Royal Heritage Temple & Shrine", "HISTORIC TEMPLE")),
            Triple(-0.0021, -0.0019, Pair("Old Town Landmark Clock Tower", "HERITAGE LANDMARK")),
            Triple(0.0032, -0.0008, Pair("National History Museum", "MUSEUM")),
            Triple(-0.0035, 0.0022, Pair("Civic Town Hall & Library", "TOWN HALL")),
            Triple(0.0025, -0.0031, Pair("Ekamra Botanical Eco Park", "PUBLIC PARK")),
            Triple(-0.0011, 0.0038, Pair("Grand Cultural Market", "PUBLIC MARKET")),
            Triple(0.0041, 0.0019, Pair("Victoria Peace Memorial", "HISTORIC MONUMENT")),
            Triple(-0.0028, -0.0035, Pair("Parsurameswara Temple Ruins", "ARCHAEOLOGICAL SITE"))
        )

        return offsets.mapIndexed { idx, (latOffset, lonOffset, meta) ->
            val siteLat = baseLat + latOffset
            val siteLon = baseLon + lonOffset
            val results = FloatArray(1)
            android.location.Location.distanceBetween(userLat, userLon, siteLat, siteLon, results)
            MapMonumentItem(
                id = "public_site_$idx",
                name = meta.first,
                locationName = "Nearby Public Site",
                geoPoint = GeoPoint(siteLat, siteLon),
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
