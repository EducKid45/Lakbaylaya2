package com.example.lakbaylaya.ui.screens.map.models

/**
 * Data class representing a search result item
 *
 * @property id Unique identifier for the search result
 * @property placeName Name of the place (e.g., "Intramuros")
 * @property address Full address or description
 * @property latitude Latitude coordinate
 * @property longitude Longitude coordinate
 * @property distanceMeters Distance from current location in meters
 * @property category Category of place (e.g., "Restaurant", "Park", "Historic Site")
 * @property isRecent Whether this is from recent searches
 * @property iconType Type of icon to display (for visual categorization)
 */
data class SearchResult(
    val id: String,
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val category: String = "",
    val isRecent: Boolean = false,
    val iconType: PlaceIconType = PlaceIconType.LOCATION
)

/**
 * Enum representing different place icon types
 */
enum class PlaceIconType {
    LOCATION,
    RESTAURANT,
    PARK,
    MUSEUM,
    SHOPPING,
    HOTEL,
    HISTORIC,
    RELIGIOUS,
    ATTRACTION,
    TRANSPORT,
    RECENT
}


