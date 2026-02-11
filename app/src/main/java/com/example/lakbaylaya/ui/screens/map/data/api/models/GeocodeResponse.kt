package com.example.lakbaylaya.ui.screens.map.data.api.models

/**
 * Response model for Geoapify Reverse Geocoding API
 *
 * Contains information about a location based on coordinates
 */
data class GeocodeResponse(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val country: String
)
