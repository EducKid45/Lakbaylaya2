package com.example.lakbaylaya.ui.screens.map.data.api.models

/**
 * Response model for Geoapify Places API
 *
 * Contains a list of places matching the search query
 */
data class PlacesResponse(
    val places: List<Place>
) {
    /**
     * Represents a single place from the search results
     */
    data class Place(
        val placeId: String,
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val category: String,
        val distance: Double
    )
}
