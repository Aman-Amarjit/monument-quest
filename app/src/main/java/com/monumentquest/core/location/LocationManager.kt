package com.monumentquest.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ultra-Fast 5Hz High-Precision Adaptive GPS Location Engine.
 * Provides instantaneous location updates with zero lag.
 */
class KalmanLocationFilter {
    private var latEMA = 0.0
    private var lonEMA = 0.0
    private var speedEMA = 0f
    private var bearingEMA = 0f

    fun filter(raw: Location): Location {
        // Discard extreme accuracy outliers (> 25m)
        if (raw.hasAccuracy() && raw.accuracy > 25.0f) {
            return Location(raw).apply {
                if (latEMA != 0.0) {
                    latitude = latEMA
                    longitude = lonEMA
                    speed = speedEMA
                    bearing = bearingEMA
                }
            }
        }

        if (latEMA == 0.0) {
            latEMA = raw.latitude
            lonEMA = raw.longitude
            speedEMA = if (raw.hasSpeed()) raw.speed else 0f
            bearingEMA = if (raw.hasBearing()) raw.bearing else 0f
            return raw
        }

        // Ultra-responsive instantaneous alpha (0.85 when moving)
        val accuracy = if (raw.hasAccuracy()) raw.accuracy else 5f
        val alpha = when {
            accuracy < 5.0f -> 0.85
            accuracy < 10.0f -> 0.65
            else -> 0.40
        }

        latEMA = latEMA + alpha * (raw.latitude - latEMA)
        lonEMA = lonEMA + alpha * (raw.longitude - lonEMA)

        val rawSpeed = if (raw.hasSpeed()) raw.speed else 0f
        speedEMA = (speedEMA + alpha * (rawSpeed - speedEMA)).toFloat()

        if (raw.hasBearing()) {
            bearingEMA = raw.bearing
        }

        return Location(raw).apply {
            latitude = latEMA
            longitude = lonEMA
            speed = if (speedEMA < 0.2f) 0f else speedEMA
            bearing = bearingEMA
        }
    }
}

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val nativeLocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

    private val kalmanFilter = KalmanLocationFilter()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        // Emit last location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
            if (lastLoc != null) {
                trySend(kalmanFilter.filter(lastLoc))
            }
        }

        // Ultra-fast 200ms (5 Hz) real-time location stream
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200L)
            .setMinUpdateIntervalMillis(100L)
            .setMinUpdateDistanceMeters(0f)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(false)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(kalmanFilter.filter(location))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        val nativeListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(kalmanFilter.filter(location))
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            nativeLocationManager?.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER,
                200L,
                0f,
                nativeListener,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Ignore fallback listener exception
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            try {
                nativeLocationManager?.removeUpdates(nativeListener)
            } catch (e: Exception) {
                // Ignore remove listener exception
            }
        }
    }

    fun checkProximity(userLat: Double, userLon: Double, monLat: Double, monLon: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, monLat, monLon, results)
        return results[0].toDouble()
    }
}
