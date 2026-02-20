package com.example.lakbaylaya.data.api.models

/**
 * Response model for Geoapify Place Details API
 *
 * Contains detailed information about a specific place
 */
data class PlaceDetailsResponse(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val phone: String,
    val website: String,
    val openingHours: String
)
