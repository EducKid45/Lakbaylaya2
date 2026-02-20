package com.example.lakbaylaya.ui.screens.map.components.markerEdit

/**
 * Represents an edited marker location with geographic coordinates and address.
 * Used to store the new location when user confirms a marker relocation.
 * @property latitude Geographic latitude coordinate of the new marker location
 * @property longitude Geographic longitude coordinate of the new marker location
 * @property address Human-readable address string for the new location
 */
data class MarkerLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)



