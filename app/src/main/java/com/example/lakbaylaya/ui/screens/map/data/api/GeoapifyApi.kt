package com.example.lakbaylaya.ui.screens.map.data.api

import com.example.lakbaylaya.ui.screens.map.data.api.models.GeocodeResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.PlaceDetailsResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.PlacesResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.ReverseGeocodeResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.RoutingResponse

/**
 * Interface defining Geoapify API endpoints
 *
 * This interface follows the Repository pattern and defines all available
 * Geoapify API operations for the application.
 *
 * API Documentation: https://www.geoapify.com/api-documentation
 */
interface GeoapifyApi {

    /**
     * Search for places using text query
     *
     * @param query Search text
     * @param lat User's current latitude
     * @param lon User's current longitude
     * @param limit Maximum number of results (1-20)
     * @param bias Bias search towards user location
     * @return PlacesResponse containing search results
     */
    suspend fun searchPlaces(
        query: String,
        lat: Double?,
        lon: Double?,
        limit: Int = 10,
        bias: String = "proximity"
    ): Result<PlacesResponse>

    /**
     * Reverse geocode coordinates to get place information
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return GeocodeResponse containing place details
     */
    suspend fun reverseGeocode(
        lat: Double,
        lon: Double
    ): Result<GeocodeResponse>

    /**
     * Reverse geocode with amenity search to find closest point of interest
     * Used for displaying meaningful location names (e.g., "Near Starbucks")
     *
     * @param lat Latitude
     * @param lon Longitude
     * @param radius Search radius in meters (default 100m)
     * @return ReverseGeocodeResponse containing closest amenities
     */
    suspend fun reverseGeocodeWithAmenity(
        lat: Double,
        lon: Double,
        radius: Int = 100
    ): Result<ReverseGeocodeResponse>

    /**
     * Get detailed information about a specific place
     *
     * @param placeId Geoapify place ID
     * @return PlaceDetailsResponse containing detailed place information
     */
    suspend fun getPlaceDetails(
        placeId: String
    ): Result<PlaceDetailsResponse>

    /**
     * Get autocomplete suggestions as user types
     *
     * @param query Partial text query
     * @param lat User's current latitude
     * @param lon User's current longitude
     * @param limit Maximum number of suggestions
     * @return PlacesResponse containing suggestions
     */
    suspend fun autocomplete(
        query: String,
        lat: Double?,
        lon: Double?,
        limit: Int = 5
    ): Result<PlacesResponse>

    /**
     * Calculate route between two points
     *
     * @param startLat Starting latitude
     * @param startLon Starting longitude
     * @param endLat Destination latitude
     * @param endLon Destination longitude
     * @param mode Transportation mode (walk, drive, transit)
     * @param alternatives Number of alternative routes (0-2)
     * @return RoutingResponse containing route(s)
     */
    suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        mode: String = "walk",
        alternatives: Int = 2
    ): Result<RoutingResponse>

    /**
     * Calculate route with waypoints (intermediate stops)
     *
     * @param waypoints List of coordinates as [lon, lat] pairs - first is start, last is end
     * @param mode Transportation mode (walk, drive, transit)
     * @param alternatives Number of alternative routes (0-2)
     * @return RoutingResponse containing route(s)
     */
    suspend fun getRouteWithWaypoints(
        waypoints: List<Pair<Double, Double>>, // [latitude, longitude] pairs
        mode: String = "walk",
        alternatives: Int = 2
    ): Result<RoutingResponse>
}
