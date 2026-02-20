package com.example.lakbaylaya.maplibre.config

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/**
 * Configuration for MapLibre camera positions and settings
 *
 * Provides predefined camera positions for common locations and default settings.
 */
object MapCameraConfig {

    // Default camera settings
    const val DEFAULT_ZOOM = 12.0
    const val DEFAULT_TILT = 0.0
    const val DEFAULT_BEARING = 0.0
    const val ANIMATION_DURATION_MEDIUM = 600
    const val ZOOM_STREET = 14.0

    // Fast default camera position for the Philippines
    val DEFAULT_POSITION = CameraPosition.Builder()
        .target(LatLng(12.8797, 121.7740)) // Geographic center of Philippines
        .zoom(DEFAULT_ZOOM)
        .tilt(DEFAULT_TILT)
        .bearing(DEFAULT_BEARING)
        .build()

    /**
     * Create a camera position for a specific location
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param zoom Zoom level (default: DEFAULT_ZOOM)
     * @param tilt Tilt angle in degrees (default: 0)
     * @param bearing Bearing angle in degrees (default: 0)
     * @return CameraPosition object
     */
    fun createPosition(
        latitude: Double,
        longitude: Double,
        zoom: Double = DEFAULT_ZOOM,
        tilt: Double = DEFAULT_TILT,
        bearing: Double = DEFAULT_BEARING
    ): CameraPosition {
        return CameraPosition.Builder()
            .target(LatLng(latitude, longitude))
            .zoom(zoom)
            .tilt(tilt)
            .bearing(bearing)
            .build()
    }
}
