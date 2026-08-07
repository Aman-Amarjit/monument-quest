package com.monumentquest.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monumentquest.core.location.LocationManager
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.data.repository.OverpassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject

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

    private var lastLocation: Location? = null
    private var isOverpassFetched = false

    init {
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
                        _userLocation.value = currentGeo

                        detectCityName(location.latitude, location.longitude)

                        if (!isOverpassFetched) {
                            fetchRealOverpassMonuments(location.latitude, location.longitude)
                            isOverpassFetched = true
                        }

                        // Filter speed & pace
                        val rawSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                        val filteredSpeedKmh = if (rawSpeedKmh >= 1.5f) rawSpeedKmh else 0f
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
                            filteredSpeedKmh > 6f  -> "RUNNING"
                            filteredSpeedKmh > 1.5f -> "WALKING"
                            else                   -> "STANDSTILL"
                        }

                        lastLocation?.let { prev ->
                            val stepDist = prev.distanceTo(location).toDouble()
                            if (stepDist >= 2.5 && stepDist < 120.0) {
                                _totalDistanceWalked.value += stepDist

                                val currentPath = _walkPathPoints.value.toMutableList()
                                currentPath.add(currentGeo)
                                _walkPathPoints.value = currentPath
                            }
                        }

                        if (lastLocation == null) {
                            lastLocation = location
                            _walkPathPoints.value = listOf(currentGeo)
                        } else if (lastLocation!!.distanceTo(location) >= 2.5f) {
                            lastLocation = location
                        }

                        recalculateDistances(location)
                    }
            } catch (e: Exception) {
                // Fallback
            }
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
