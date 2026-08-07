package com.monumentquest.core.location

import android.content.Context
import android.location.Location
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocationManagerTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        locationManager = LocationManager(context)
    }

    @Test
    fun `checkProximity with Location objects should return distance from distanceTo`() {
        // Arrange
        val userLocation = mockk<Location>()
        val monumentLocation = mockk<Location>()
        val expectedDistance = 125.5f

        every { userLocation.distanceTo(monumentLocation) } returns expectedDistance

        // Act
        val result = locationManager.checkProximity(userLocation, monumentLocation)

        // Assert
        assertEquals(expectedDistance.toDouble(), result, 0.0001)
        verify { userLocation.distanceTo(monumentLocation) }
    }

    @Test
    fun `checkProximity with coordinates should return distance from distanceBetween`() {
        // Arrange
        io.mockk.mockkStatic(Location::class)
        val userLat = 40.7128
        val userLon = -74.0060
        val monLat = 40.7484
        val monLon = -73.9857
        val expectedDistance = 500.0f

        io.mockk.every {
            Location.distanceBetween(userLat, userLon, monLat, monLon, any())
        } answers {
            val results = it.invocation.args[4] as FloatArray
            results[0] = expectedDistance
        }

        // Act
        val result = locationManager.checkProximity(userLat, userLon, monLat, monLon)

        // Assert
        assertEquals(expectedDistance.toDouble(), result, 0.0001)
        
        io.mockk.unmockkStatic(Location::class)
    }
}
