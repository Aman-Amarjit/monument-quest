package com.monumentquest.core.ar

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeospatialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var session: Session? = null

    fun setupSession(session: Session) {
        this.session = session
        val config = session.config
        config.geospatialMode = Config.GeospatialMode.ENABLED
        session.configure(config)
    }

    fun checkGeospatialAvailability(): Boolean {
        return session?.earth != null
    }

    fun createAnchorAt(latitude: Double, longitude: Double, altitude: Double): Anchor? {
        val earth = session?.earth ?: return null
        return earth.createAnchor(latitude, longitude, altitude, 0f, 0f, 0f, 1f)
    }

    fun updateSession(frame: Frame) {
        // Handle per-frame updates if necessary
    }
}
