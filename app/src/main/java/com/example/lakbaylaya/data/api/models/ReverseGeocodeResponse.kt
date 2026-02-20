package com.example.lakbaylaya.data.api.models

/**
 * Response model for reverse geocoding API
 * Converts coordinates to human-readable location information
 *
 * Note: Using simple data classes (not @Serializable) since we parse manually with JSONObject
 */
data class ReverseGeocodeResponse(
    val features: List<ReverseGeocodeFeature> = emptyList()
)

data class ReverseGeocodeFeature(
    val properties: ReverseGeocodeProperties,
    val latitude: Double,
    val longitude: Double
)

data class ReverseGeocodeProperties(
    val name: String? = null,
    val amenity: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val formatted: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val distance: Double? = null
)

