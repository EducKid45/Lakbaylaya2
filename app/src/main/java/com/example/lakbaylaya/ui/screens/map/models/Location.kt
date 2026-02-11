package com.example.lakbaylaya.ui.screens.map.models

/**
 * Data class representing a geographic location
 *
 * @property latitude Latitude coordinate
 * @property longitude Longitude coordinate
 * @property altitude Altitude in meters (optional)
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)
