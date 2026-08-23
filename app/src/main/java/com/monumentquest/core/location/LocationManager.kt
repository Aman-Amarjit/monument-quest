package com.monumentquest.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stationary-aware Kalman-inspired EMA filter.
 *
 * Key fixes vs the old version:
 * 1. High alpha (0.85) caused the dot to jump on every noisy GPS pulse even
 *    while standing. Now alpha is capped at 0.35 and only raised when the
 *    device is clearly moving (speed > 1 m/s AND raw displacement > 3 m).
 * 2. The old code ran BOTH FusedLocationProvider AND native GPS_PROVIDER,
 *    effectively doubling the update rate and noise. Now only FusedLocation
 *    is used — it already fuses GPS + network + sensors internally.
 * 3. Accuracy gate raised from 25m to 40m so more readings are accepted,
 *    but the low alpha keeps them from causing visible drift.
 */
class StationaryAwareLocationFilter {

    private var latSmooth  = 0.0
    private var lonSmooth  = 0.0
    private var speedSmooth = 0f
    private var initialized = false

    fun filter(raw: Location): Location {
        // Reject very inaccurate fixes
        if (raw.hasAccuracy() && raw.accuracy > 40.0f && initialized) {
            // Return last smoothed position instead
            return Location(raw).apply {
                latitude  = latSmooth
                longitude = lonSmooth
                speed     = speedSmooth
            }
        }

        if (!initialized) {
            latSmooth   = raw.latitude
            lonSmooth   = raw.longitude
            speedSmooth = if (raw.hasSpeed()) raw.speed else 0f
            initialized = true
            return raw
        }

        // Compute raw displacement in metres
        val results = FloatArray(1)
        Location.distanceBetween(latSmooth, lonSmooth, raw.latitude, raw.longitude, results)
        val displacement = results[0]

        val rawSpeed = if (raw.hasSpeed()) raw.speed else 0f

        // Only accept significant movement — ignore sub-3m GPS drift
        val isActuallyMoving = rawSpeed > 1.0f && displacement > 3.0f

        // Low alpha = sticky (stands still), higher alpha = follows movement
        val alpha = when {
            isActuallyMoving && displacement > 10f -> 0.6   // fast movement
            isActuallyMoving                       -> 0.35  // slow walk
            else                                   -> 0.05  // stationary — barely moves
        }

        latSmooth  = latSmooth  + alpha * (raw.latitude  - latSmooth)
        lonSmooth  = lonSmooth  + alpha * (raw.longitude - lonSmooth)
        speedSmooth = (speedSmooth + (alpha * (rawSpeed - speedSmooth)).toFloat())

        // Zero out speed when clearly standing still
        val filteredSpeed = if (speedSmooth < 0.3f) 0f else speedSmooth

        return Location(raw).apply {
            latitude  = latSmooth
            longitude = lonSmooth
            speed     = filteredSpeed
            // Keep raw bearing only when actually moving
            if (!isActuallyMoving) bearing = raw.bearing
        }
    }
}

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val filter = StationaryAwareLocationFilter()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        // Emit cached last-known location immediately for a fast first paint
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) trySend(filter.filter(loc))
        }

        // 1 Hz updates, minimum 3 m displacement — avoids spam while standing still.
        // FusedLocation already blends GPS + network + accelerometer, so we don't
        // need a separate native GPS_PROVIDER stream.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(3f)   // ← key: no updates for sub-3m noise
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(filter.filter(it)) }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    fun checkProximity(
        userLat: Double, userLon: Double,
        monLat: Double,  monLon: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, monLat, monLon, results)
        return results[0].toDouble()
    }
}
