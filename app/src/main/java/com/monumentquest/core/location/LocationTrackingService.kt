package com.monumentquest.core.location

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

/**
 * Foreground service that tracks location in the background and persists the
 * travel path to SharedPreferences so it survives app restarts.
 */
class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var prefs: android.content.SharedPreferences
    private val pathPoints = mutableListOf<String>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { loc ->
                pathPoints.add("${loc.latitude},${loc.longitude}")
                // Keep last 20 000 points (~100km of dense tracking)
                if (pathPoints.size > 20000) pathPoints.removeAt(0)
                savePoints()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        prefs = getSharedPreferences("travel_history", Context.MODE_PRIVATE)
        loadExisting()
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        startTracking()
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun startTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(5f)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadExisting() {
        val saved = prefs.getString("path_points", "") ?: ""
        if (saved.isNotBlank()) {
            pathPoints.addAll(saved.split("|").filter { it.isNotBlank() })
        }
    }

    private fun savePoints() {
        prefs.edit()
            .putString("path_points", pathPoints.joinToString("|"))
            .putLong("last_updated", System.currentTimeMillis())
            .apply()
    }

    private fun buildNotification(): Notification {
        val channelId = "location_tracking"
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(channelId) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Location Tracking", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "MonumentQuest background expedition tracking"
                    setShowBadge(false)
                }
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MonumentQuest")
            .setContentText("Tracking your expedition in the background…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIF_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, LocationTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }

        /** Returns all saved travel points as lat/lon pairs. */
        fun getSavedPath(context: Context): List<Pair<Double, Double>> {
            val prefs = context.getSharedPreferences("travel_history", Context.MODE_PRIVATE)
            val saved = prefs.getString("path_points", "") ?: ""
            if (saved.isBlank()) return emptyList()
            return saved.split("|").mapNotNull { pt ->
                val parts = pt.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lon = parts[1].toDoubleOrNull()
                    if (lat != null && lon != null) lat to lon else null
                } else null
            }
        }

        /** Total distance in km of the saved path. */
        fun getTotalDistanceKm(context: Context): Double {
            val points = getSavedPath(context)
            if (points.size < 2) return 0.0
            var total = 0.0
            for (i in 1 until points.size) {
                val r = FloatArray(1)
                android.location.Location.distanceBetween(
                    points[i - 1].first, points[i - 1].second,
                    points[i].first,     points[i].second,
                    r
                )
                total += r[0]
            }
            return total / 1000.0
        }
    }
}
