package com.monumentquest.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monumentquest.core.location.LocationManager
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.data.model.TacticalGeometry
import com.monumentquest.data.repository.OverpassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject

data class CoverageStats(
    val coveredAreaFormatted: String = "0.0 km²",
    val areaPercentageFormatted: String = "(0% of district)",
    val totalTrackFormatted: String = "0.0 km",
    val structuresVisitedCount: Int = 0,
    val roadsTraveledCount: Int = 0,
    val publicSpacesCount: Int = 0
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val overpassRepository: OverpassRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userLocation = MutableStateFlow<GeoPoint?>(null)
    val userLocation: StateFlow<GeoPoint?> = _userLocation

    private val _detectedCityName = MutableStateFlow("Detecting Location…")
    val detectedCityName: StateFlow<String> = _detectedCityName

    private val _walkPathPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val walkPathPoints: StateFlow<List<GeoPoint>> = _walkPathPoints

    private val _exploredZones = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val exploredZones: StateFlow<Set<Pair<Int, Int>>> = _exploredZones

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh

    private val _currentPace = MutableStateFlow("0'00\"/km")
    val currentPace: StateFlow<String> = _currentPace

    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing: StateFlow<Float> = _currentBearing

    private val _totalDistanceWalked = MutableStateFlow(0.0)
    val totalDistanceWalked: StateFlow<Double> = _totalDistanceWalked

    private val _movementStatus = MutableStateFlow("STANDSTILL")
    val movementStatus: StateFlow<String> = _movementStatus

    private val _monuments = MutableStateFlow<List<MapMonumentItem>>(emptyList())
    val monuments: StateFlow<List<MapMonumentItem>> = _monuments

    private val _tacticalGeometry = MutableStateFlow(TacticalGeometry(emptyList(), emptyList()))
    val tacticalGeometry: StateFlow<TacticalGeometry> = _tacticalGeometry

    // Dynamic Live Area Coverage Stats Flow
    val coverageStats: StateFlow<CoverageStats> = combine(
        _exploredZones,
        _totalDistanceWalked,
        _monuments,
        _walkPathPoints
    ) { zones, totalDistMeters, monumentsList, pathPoints ->
        val totalAreaKm2 = Math.max(0.1, zones.size * 0.0025)
        val areaPercent = Math.min(99, ((totalAreaKm2 / 12.0) * 100).toInt())
        val totalTrackKm = totalDistMeters / 1000.0

        val visitedCount = monumentsList.count { it.distanceMeters in 1..150 }
        val roadsCount = Math.max(1, pathPoints.size / 6)
        val publicSpaces = Math.max(1, zones.size / 4)

        CoverageStats(
            coveredAreaFormatted = String.format(Locale.US, "%.1f km²", totalAreaKm2),
            areaPercentageFormatted = "($areaPercent% of district)",
            totalTrackFormatted = if (totalTrackKm >= 1.0) String.format(Locale.US, "%.1f km", totalTrackKm) else String.format(Locale.US, "%d m", totalDistMeters.toInt()),
            structuresVisitedCount = Math.max(1, visitedCount),
            roadsTraveledCount = roadsCount,
            publicSpacesCount = publicSpaces
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CoverageStats())

    private val prefs = context.getSharedPreferences("discovery_prefs", Context.MODE_PRIVATE)
    private var lastLocation: Location? = null
    private var isOverpassFetched = false
    private var lastGeometryFetchLocation: Location? = null

    init {
        loadExploredZones()
        startLiveGpsTracking()
    }

    @SuppressLint("MissingPermission")
    fun startLiveGpsTracking() {
        viewModelScope.launch {
            try {
                locationManager.getLocationUpdates()
                    .catch { /* handle error */ }
                    .collect { location ->
                        val currentGeo = GeoPoint(location.latitude, location.longitude)

                        // Only move the dot when displacement >= 3m — stops standing-still jitter
                        val prev = _userLocation.value
                        val displacement = if (prev != null) {
                            val r = FloatArray(1)
                            android.location.Location.distanceBetween(
                                prev.latitude, prev.longitude,
                                location.latitude, location.longitude, r
                            )
                            r[0]
                        } else Float.MAX_VALUE

                        if (displacement >= 3f) {
                            _userLocation.value = currentGeo
                        }

                        detectCityName(location.latitude, location.longitude)

                        if (!isOverpassFetched) {
                            fetchRealOverpassMonuments(location.latitude, location.longitude)
                            isOverpassFetched = true
                        }

                        val rawSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                        // Only count as moving if > 3 km/h (GPS noise is typically < 1.5 km/h)
                        val filteredSpeedKmh = if (rawSpeedKmh >= 3.0f) rawSpeedKmh else 0f
                        _currentSpeedKmh.value = filteredSpeedKmh

                        if (filteredSpeedKmh > 0.5f) {
                            val paceSecPerKm = (3600f / filteredSpeedKmh).toInt()
                            val mins = paceSecPerKm / 60
                            val secs = paceSecPerKm % 60
                            _currentPace.value = String.format("%d'%02d\"/km", mins, secs)
                        } else {
                            _currentPace.value = "0'00\"/km"
                        }

                        if (location.hasBearing()) {
                            _currentBearing.value = location.bearing
                        }

                        _movementStatus.value = when {
                            filteredSpeedKmh > 15f -> "IN TRANSIT"
                            filteredSpeedKmh > 8f  -> "RUNNING"
                            filteredSpeedKmh > 3f  -> "WALKING"
                            else                   -> "STANDSTILL"
                        }

                        lastLocation?.let { prev ->
                            val stepDist = prev.distanceTo(location).toDouble()
                            // Only log distance for genuine movement (> 3m, < 100m per step)
                            if (stepDist >= 3.0 && stepDist < 100.0) {
                                _totalDistanceWalked.value += stepDist
                                val currentPath = _walkPathPoints.value.toMutableList()
                                currentPath.add(currentGeo)
                                _walkPathPoints.value = currentPath
                                lastLocation = location  // update here so next step is correct
                            }
                        }

                        if (lastLocation == null) {
                            lastLocation = location
                            _walkPathPoints.value = listOf(currentGeo)
                        }

                        updateExploredZones(location.latitude, location.longitude)
                        recalculateDistances(location)

                        if (lastGeometryFetchLocation == null || lastGeometryFetchLocation!!.distanceTo(location) > 600f) {
                            fetchTacticalBlueprint(location.latitude, location.longitude)
                            lastGeometryFetchLocation = location
                        }
                    }
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    private fun updateExploredZones(lat: Double, lon: Double) {
        val gridLat = (lat * 2000).toInt()
        val gridLon = (lon * 2000).toInt()

        val currentZones = _exploredZones.value
        if (!currentZones.contains(gridLat to gridLon)) {
            val newZones = currentZones + (gridLat to gridLon)
            _exploredZones.value = newZones
            saveExploredZones(newZones)
        }
    }

    private fun saveExploredZones(zones: Set<Pair<Int, Int>>) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = zones.joinToString("|") { "${it.first},${it.second}" }
            prefs.edit().putString("explored_zones", data).apply()
        }
    }

    private fun loadExploredZones() {
        val data = prefs.getString("explored_zones", "") ?: ""
        if (data.isNotBlank()) {
            val zones = data.split("|").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    parts[0].toIntOrNull()?.let { lat ->
                        parts[1].toIntOrNull()?.let { lon -> lat to lon }
                    }
                } else null
            }.toSet()
            _exploredZones.value = zones
        }
    }

    private fun detectCityName(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Local Region"
                    val country = addr.countryName ?: ""
                    _detectedCityName.value = if (country.isNotBlank()) "$locality, $country" else locality
                }
            } catch (e: Exception) {
                // Keep default
            }
        }
    }

    private fun fetchRealOverpassMonuments(lat: Double, lon: Double) {
        viewModelScope.launch {
            val realMonuments = overpassRepository.fetchRealMonumentsNearby(lat, lon)
            if (realMonuments.isNotEmpty()) {
                _monuments.value = realMonuments
            } else {
                generateDynamicFallback(lat, lon)
            }
        }
    }

    private fun fetchTacticalBlueprint(lat: Double, lon: Double) {
        viewModelScope.launch {
            val geometry = overpassRepository.fetchTacticalGeometry(lat, lon)
            if (geometry.roads.isNotEmpty() || geometry.buildings.isNotEmpty()) {
                _tacticalGeometry.value = geometry
            }
        }
    }

    private fun generateDynamicFallback(lat: Double, lon: Double) {
        val city = _detectedCityName.value
        _monuments.value = listOf(
            MapMonumentItem("m1", "Historic Landmark", city, GeoPoint(lat + 0.0018, lon + 0.0014), 500, "Heritage Shrine", 180),
            MapMonumentItem("m2", "Cultural Sanctuary", city, GeoPoint(lat - 0.0012, lon + 0.0022), 450, "Historic Temple", 150),
            MapMonumentItem("m3", "Ancient Relic Pillar", city, GeoPoint(lat + 0.0028, lon - 0.0016), 600, "Archaeological Monument", 310)
        )
    }

    private fun recalculateDistances(userLoc: Location) {
        val updated = _monuments.value.map { mon ->
            val dist = locationManager.checkProximity(
                userLoc.latitude, userLoc.longitude,
                mon.geoPoint.latitude, mon.geoPoint.longitude
            ).toInt()
            mon.copy(distanceMeters = dist)
        }
        _monuments.value = updated
    }
}
