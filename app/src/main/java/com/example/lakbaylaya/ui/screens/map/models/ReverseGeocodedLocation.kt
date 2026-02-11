package com.example.lakbaylaya.ui.screens.map.models

/**
 * Domain model representing a reverse geocoded location
 * Simplified representation of location details for UI display
 *
 * @property placeName Name of the closest amenity or location
 * @property amenityType Type of amenity (cafe, restaurant, park, etc.)
 * @property address Formatted address string
 * @property city City name
 * @property distance Distance from requested coordinates in meters
 */
data class ReverseGeocodedLocation(
    val placeName: String,
    val amenityType: String? = null,
    val address: String,
    val city: String? = null,
    val distance: Double = 0.0
) {
    companion object {
        /**
         * Creates a default "Unknown Location" instance
         */
        fun unknown() = ReverseGeocodedLocation(
            placeName = "Unknown Location",
            amenityType = null,
            address = "Unable to determine location",
            city = null,
            distance = 0.0
        )

        /**
         * Creates a "Loading..." placeholder instance
         */
        fun loading() = ReverseGeocodedLocation(
            placeName = "Loading...",
            amenityType = null,
            address = "Fetching location...",
            city = null,
            distance = 0.0
        )
    }

    /**
     * Returns a display-friendly label for the location
     * Prioritizes amenity name, falls back to formatted address
     */
    fun getDisplayLabel(): String {
        return when {
            placeName.isNotBlank() && placeName != "Unknown Location" -> placeName
            city != null -> city
            address.isNotBlank() -> address
            else -> "Current Location"
        }
    }

    /**
     * Returns a secondary description line
     * Shows amenity type and city if available
     */
    fun getSecondaryInfo(): String {
        val parts = mutableListOf<String>()

        amenityType?.let {
            parts.add(it.replaceFirstChar { char -> char.uppercase() })
        }

        city?.let {
            if (it != placeName) parts.add(it)
        }

        return parts.joinToString(" • ")
    }
}

