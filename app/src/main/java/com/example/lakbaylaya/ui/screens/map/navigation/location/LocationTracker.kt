package com.example.lakbaylaya.ui.screens.map.navigation.location

import android.location.Location

/**
 * Interface for GPS location tracking during navigation
 *
 * Provides abstraction over location tracking implementation,
 * allowing for testing and alternative implementations.
 */
interface LocationTracker {

    /**
     * Start tracking user location
     *
     * @param onLocationUpdate Callback invoked when location is updated
     * @param onAccuracyChanged Callback invoked when accuracy changes
     */
    fun startTracking(
        onLocationUpdate: (location: Location) -> Unit,
        onAccuracyChanged: (accuracy: Float) -> Unit = {}
    )

    /**
     * Stop tracking location
     */
    fun stopTracking()

    /**
     * Get last known location
     */
    fun getLastLocation(): Location?

    /**
     * Check if tracker is currently active
     */
    fun isTracking(): Boolean

    /**
     * Set minimum distance for location updates (in meters)
     * Helps reduce GPS noise and battery usage
     */
    fun setMinimumDistance(distanceMeters: Float)

    /**
     * Set minimum time interval for location updates (in milliseconds)
     */
    fun setMinimumInterval(intervalMs: Long)
}
