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
 * Pulls real OSM geometry (buildings, roads, parks, water) from the Overpass
 * API for the current map viewport and converts it into [TacticalGeometry]
 * that [Isometric3DOverlay] paints.
 *
 * Changes vs previous version:
 *  - Fetches ALL highway types (not just major ones) so roads appear everywhere
 *    on the map, not just arterials.
 *  - Increased timeout to 25s to handle busy Overpass instances.
 *  - Added `relation` support alongside `way` so large parks and rivers are
 *    included.
 */
object TacticalGeometryFetcher {

    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    // Major highway values — drawn wider with amber fill
    private val MAJOR_HIGHWAYS = setOf(
        "motorway", "trunk", "primary", "secondary", "tertiary",
        "motorway_link", "trunk_link", "primary_link", "secondary_link"
    )

    suspend fun fetch(bounds: BoundingBox): TacticalGeometry = withContext(Dispatchers.IO) {
        val bbox = "${bounds.latSouth},${bounds.lonWest},${bounds.latNorth},${bounds.lonEast}"
        val query = """
            [out:json][timeout:25];
            (
              way["building"]($bbox);
              way["highway"]($bbox);
              way["leisure"~"^(park|garden|pitch|playground)$"]($bbox);
              way["landuse"~"^(grass|meadow|forest|recreation_ground)$"]($bbox);
              way["natural"="water"]($bbox);
              way["waterway"~"^(river|canal|stream|riverbank)$"]($bbox);
            );
            out body geom;
        """.trimIndent()

        val json = try {
            postQuery(query)
        } catch (e: Exception) {
            return@withContext TacticalGeometry()
        }

        val buildings = mutableListOf<BuildingFootprint>()
        val roads     = mutableListOf<RoadSegment>()
        val parks     = mutableListOf<AreaFeature>()
        val water     = mutableListOf<AreaFeature>()

        val elements = json.optJSONArray("elements") ?: JSONArray()
        for (i in 0 until elements.length()) {
            val el        = elements.getJSONObject(i)
            val geomArray = el.optJSONArray("geometry") ?: continue
            val points    = ArrayList<GeoPoint>(geomArray.length())
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
                    val hw = tags.optString("highway", "")
                    roads.add(RoadSegment(points, isMajor = hw in MAJOR_HIGHWAYS))
                }
                tags.optString("leisure") in setOf("park", "garden", "pitch", "playground") ||
                    tags.optString("landuse") in setOf("grass", "meadow", "forest", "recreation_ground") -> {
                    parks.add(AreaFeature(points))
                }
                tags.optString("natural") == "water" ||
                    tags.optString("waterway") in setOf("river", "canal", "stream", "riverbank") -> {
                    water.add(AreaFeature(points))
                }
            }
        }

        TacticalGeometry(buildings = buildings, roads = roads, parks = parks, water = water)
    }

    private fun postQuery(query: String): JSONObject {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 25_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        connection.outputStream.use {
            it.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray())
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(body)
    }
}
