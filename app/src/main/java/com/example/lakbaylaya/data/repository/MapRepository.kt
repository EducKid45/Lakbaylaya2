package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.ui.screens.map.models.ReverseGeocodedLocation
import com.example.lakbaylaya.ui.screens.map.models.RouteOption
import com.example.lakbaylaya.ui.screens.map.models.RoutePoint
import com.example.lakbaylaya.ui.screens.map.models.SearchResult

/**
 * Repository interface for map-related data operations
 *
 * This interface defines the contract for accessing map and location data.
 * It abstracts the data sources (API, local storage, etc.) from the UI layer.
 *
 * Follows Repository Pattern and Dependency Inversion Principle.
 */
interface MapRepository {

    /**
     * Search for places by text query
     *
     * @param query Search text
     * @param latitude Current user latitude (optional)
     * @param longitude Current user longitude (optional)
     * @param limit Maximum number of results
     * @return List of search results
     */
    suspend fun searchPlaces(
        query: String,
        latitude: Double?,
        longitude: Double?,
        limit: Int = 10
    ): Result<List<SearchResult>>

    /**
     * Get autocomplete suggestions for a partial query
     *
     * @param query Partial search text
     * @param latitude Current user latitude (optional)
     * @param longitude Current user longitude (optional)
     * @param limit Maximum number of suggestions
     * @return List of search results
     */
    suspend fun getAutocompleteSuggestions(
        query: String,
        latitude: Double?,
        longitude: Double?,
        limit: Int = 5
    ): Result<List<SearchResult>>

    /**
     * Reverse geocode coordinates to get location information with closest amenity
     * Returns a user-friendly location description (e.g., "Near Starbucks Coffee")
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @param radius Search radius for amenities in meters
     * @return ReverseGeocodedLocation with place name and details
     */
    suspend fun reverseGeocodeLocation(
        latitude: Double,
        longitude: Double,
        radius: Int = 100
    ): Result<ReverseGeocodedLocation>

    /**
     * Get place name from coordinates (reverse geocoding)
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Place name or address
     */
    suspend fun getPlaceFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<String>

    /**
     * Calculate route between two points
     *
     * @param origin Starting point
     * @param destination End point
     * @param mode Transportation mode (walk, drive, transit)
     * @return List of route options
     */
    suspend fun calculateRoute(
        origin: RoutePoint,
        destination: RoutePoint,
        mode: String = "walk"
    ): Result<List<RouteOption>>

    /**
     * Calculate route with intermediate stops
     *
     * @param origin Starting point
     * @param destination End point
     * @param stops List of intermediate stops
     * @param mode Transportation mode (walk, drive, transit)
     * @return List of route options
     */
    suspend fun calculateRouteWithStops(
        origin: RoutePoint,
        destination: RoutePoint,
        stops: List<RoutePoint>,
        mode: String = "walk"
    ): Result<List<RouteOption>>
}