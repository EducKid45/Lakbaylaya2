package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.data.api.GeoapifyApi
import com.example.lakbaylaya.data.api.GeoapifyApiImpl
import com.example.lakbaylaya.data.api.models.GeocodeResponse
import com.example.lakbaylaya.data.api.models.ReverseGeocodeFeature
import com.example.lakbaylaya.data.api.models.ReverseGeocodeProperties
import com.example.lakbaylaya.data.api.models.RoutingResponse
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.example.lakbaylaya.ui.screens.map.models.ManeuverType
import com.example.lakbaylaya.ui.screens.map.models.ReverseGeocodedLocation
import com.example.lakbaylaya.ui.screens.map.models.RouteOption
import com.example.lakbaylaya.ui.screens.map.models.RoutePoint
import com.example.lakbaylaya.ui.screens.map.models.SearchResult
import com.example.lakbaylaya.utils.DistanceUtils
import java.util.Locale
import kotlin.math.abs

/**
 * Implementation of MapRepository using Geoapify API
 *
 * This class acts as a mediator between the ViewModel and the API layer.
 * It transforms API responses into UI models and handles errors gracefully.
 *
 * Follows Repository Pattern and Single Responsibility Principle.
 */
class MapRepositoryImpl(
    private val geoapifyApi: GeoapifyApi = GeoapifyApiImpl()
) : MapRepository {

    override suspend fun searchPlaces(
        query: String,
        latitude: Double?,
        longitude: Double?,
        limit: Int
    ): Result<List<SearchResult>> {
        val apiResult = geoapifyApi.searchPlaces(query, latitude, longitude, limit)
        return apiResult.fold(
            onSuccess = { response ->
                val list = response.places.map { place ->
                    SearchResult(
                        id = place.placeId,
                        placeName = place.name,
                        address = place.address,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        distanceMeters = place.distance,
                        category = place.category,
                        isRecent = false
                    )
                }
                Result.success(list)
            },
            onFailure = { throwable -> Result.failure(throwable) }
        )
    }

    override suspend fun getAutocompleteSuggestions(
        query: String,
        latitude: Double?,
        longitude: Double?,
        limit: Int
    ): Result<List<SearchResult>> {
        val apiResult = geoapifyApi.autocomplete(query, latitude, longitude, limit)
        return apiResult.fold(
            onSuccess = { response ->
                val list = response.places.map { place ->
                    SearchResult(
                        id = place.placeId,
                        placeName = place.name,
                        address = place.address,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        distanceMeters = place.distance,
                        category = place.category,
                        isRecent = false
                    )
                }
                Result.success(list)
            },
            onFailure = { throwable -> Result.failure(throwable) }
        )
    }

    override suspend fun reverseGeocodeLocation(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<ReverseGeocodedLocation> {
        // If API key is missing, return fallback location result
        val amenityResult = geoapifyApi.reverseGeocodeWithAmenity(latitude, longitude, radius)

        return amenityResult.fold(
            onSuccess = { response ->
                // If response is empty, return fallback location result
                if (response.features.isEmpty()) {
                    return@fold getFallbackLocation(latitude, longitude)
                }

                // For each returned place that has coordinates: compute distance using Haversine
                val placesWithDistance = computeDistancesForPlaces(response.features, latitude, longitude)

                // Sort all places by distance (nearest first)
                val sortedPlaces = placesWithDistance.sortedBy { it.computedDistance }

                // Select the best place using priority logic
                val selectedPlace = selectBestPlace(sortedPlaces)

                // Construct and return the location info using selected place fields
                Result.success(constructLocationInfo(selectedPlace, latitude, longitude))
            },
            onFailure = { error ->
                // Preserve error handling behavior: return fallback on failure
                getFallbackLocation(latitude, longitude)
            }
        )
    }

    /**
     * Helper data class to hold place with computed distance
     */
    private data class PlaceWithDistance(
        val feature: ReverseGeocodeFeature,
        val computedDistance: Double
    )

    /**
     * Compute distance from user using Haversine formula for each place
     */
    private fun computeDistancesForPlaces(
        features: List<ReverseGeocodeFeature>,
        userLat: Double,
        userLon: Double
    ): List<PlaceWithDistance> {
        return features
            .filter { it.latitude != 0.0 && it.longitude != 0.0 } // Only places with valid coordinates
            .map { feature ->
                val distance = DistanceUtils.calculateDistance(
                    lat1 = userLat,
                    lon1 = userLon,
                    lat2 = feature.latitude,
                    lon2 = feature.longitude
                )
                PlaceWithDistance(feature, distance)
            }
    }

    /**
     * Select best place using priority:
     * 1. First: nearest place with both amenity and name
     * 2. Second: nearest place with any meaningful name or street
     * 3. Last: the first available result
     */
    private fun selectBestPlace(sortedPlaces: List<PlaceWithDistance>): PlaceWithDistance? {
        if (sortedPlaces.isEmpty()) return null

        // Priority 1: nearest place with both amenity and name
        val withAmenityAndName = sortedPlaces.firstOrNull { place ->
            val props = place.feature.properties
            !props.amenity.isNullOrBlank() && !props.name.isNullOrBlank()
        }
        if (withAmenityAndName != null) return withAmenityAndName

        // Priority 2: nearest place with any meaningful name or street
        val withMeaningfulInfo = sortedPlaces.firstOrNull { place ->
            val props = place.feature.properties
            !props.name.isNullOrBlank() || !props.street.isNullOrBlank()
        }
        if (withMeaningfulInfo != null) return withMeaningfulInfo

        // Priority 3: the first available result (already sorted by distance)
        return sortedPlaces.firstOrNull()
    }

    /**
     * Construct location info from selected place
     */
    private fun constructLocationInfo(
        placeWithDistance: PlaceWithDistance?,
        userLat: Double,
        userLon: Double
    ): ReverseGeocodedLocation {
        if (placeWithDistance == null) {
            return ReverseGeocodedLocation.Companion.unknown()
        }

        val props = placeWithDistance.feature.properties

        return ReverseGeocodedLocation(
            placeName = props.name ?: props.street ?: props.city ?: "Unknown",
            amenityType = props.amenity,
            address = props.formatted ?: buildAddress(props),
            city = props.city,
            distance = placeWithDistance.computedDistance
        )
    }

    /**
     * Get fallback location when API fails or returns empty
     */
    private suspend fun getFallbackLocation(
        latitude: Double,
        longitude: Double
    ): Result<ReverseGeocodedLocation> {
        // Try basic geocoding as fallback
        val basicResult = geoapifyApi.reverseGeocode(latitude, longitude)

        return basicResult.fold(
            onSuccess = { geocode ->
                Result.success(createLocationFromGeocode(geocode))
            },
            onFailure = {
                // Complete fallback: return unknown location
                Result.success(ReverseGeocodedLocation.Companion.unknown())
            }
        )
    }

    private fun createLocationFromGeocode(geocode: GeocodeResponse): ReverseGeocodedLocation {
        return ReverseGeocodedLocation(
            placeName = when {
                geocode.city.isNotBlank() -> geocode.city
                geocode.name.isNotBlank() -> geocode.name
                else -> "Current Location"
            },
            amenityType = null,
            address = geocode.address,
            city = geocode.city,
            distance = 0.0
        )
    }

    private fun buildAddress(props: ReverseGeocodeProperties): String {
        val parts = mutableListOf<String>()
        props.street?.let { if (it.isNotBlank()) parts.add(it) }
        props.suburb?.let { if (it.isNotBlank()) parts.add(it) }
        props.city?.let { if (it.isNotBlank()) parts.add(it) }
        return parts.joinToString(", ").ifEmpty { "Address not available" }
    }

    override suspend fun getPlaceFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<String> {
        val apiResult = geoapifyApi.reverseGeocode(latitude, longitude)
        return apiResult.fold(
            onSuccess = { response -> Result.success(response.name.ifEmpty { response.address }) },
            onFailure = { throwable -> Result.failure(throwable) }
        )
    }

    override suspend fun calculateRoute(
        origin: RoutePoint,
        destination: RoutePoint,
        mode: String
    ): Result<List<RouteOption>> {
        // Validate distance for walking mode (Geoapify limit is ~50km)
        if (mode == "walk") {
            val distance = DistanceUtils.calculateDistance(
                origin.latitude,
                origin.longitude,
                destination.latitude,
                destination.longitude
            )

            // Walking API limit is approximately 50,000 meters (50 km)
            if (distance > 50000) {
                return Result.failure(
                    IllegalArgumentException(
                        "Walking route distance (${
                            String.format(
                                Locale.US,
                                "%.1f",
                                distance / 1000
                            )
                        } km) exceeds maximum limit of 50 km. " +
                        "Please choose a closer destination or add intermediate stops."
                    )
                )
            }
        }

        val apiResult = geoapifyApi.getRoute(
            startLat = origin.latitude,
            startLon = origin.longitude,
            endLat = destination.latitude,
            endLon = destination.longitude,
            mode = mode,
            alternatives = 2
        )

        return apiResult.fold(
            onSuccess = { response ->
                val routes = convertRoutingResponseToRouteOptions(response)
                Result.success(routes)
            },
            onFailure = { throwable -> Result.failure(throwable) }
        )
    }

    override suspend fun calculateRouteWithStops(
        origin: RoutePoint,
        destination: RoutePoint,
        stops: List<RoutePoint>,
        mode: String
    ): Result<List<RouteOption>> {
        // Build ordered points: [start, stop1, stop2, ..., destination]
        val orderedPoints = buildList {
            add(origin)
            addAll(stops)
            add(destination)
        }
        if (orderedPoints.size < 2) {
            return Result.failure(IllegalArgumentException("At least origin and destination required"))
        }

        // Validate each segment distance for walking mode
        if (mode == "walk") {
            for (i in 0 until orderedPoints.size - 1) {
                val from = orderedPoints[i]
                val to = orderedPoints[i + 1]
                val distance = DistanceUtils.calculateDistance(
                    from.latitude,
                    from.longitude,
                    to.latitude,
                    to.longitude
                )

                if (distance > 50000) {
                    val segmentName = if (i == 0) {
                        "from origin to first stop"
                    } else if (i == orderedPoints.size - 2) {
                        "from last stop to destination"
                    } else {
                        "between stops ${i} and ${i + 1}"
                    }

                    return Result.failure(
                        IllegalArgumentException(
                            "Walking segment $segmentName (${
                                String.format(
                                    Locale.US,
                                    "%.1f",
                                    distance / 1000
                                )
                            } km) exceeds maximum limit of 50 km. " +
                            "Please add intermediate stops or choose closer locations."
                        )
                    )
                }
            }
        }

        // Loop through consecutive pairs and compute segment routes
        val segmentOptions = mutableListOf<RouteOption>()
        for (i in 0 until orderedPoints.size - 1) {
            val from = orderedPoints[i]
            val to = orderedPoints[i + 1]
            val segmentResult = calculateRoute(from, to, mode)
            val segment = segmentResult.getOrElse { err ->
                return Result.failure(err)
            }
            // Take primary route of this segment (first option)
            val primary = segment.firstOrNull()
                ?: return Result.failure(Exception("No route returned for segment ${i + 1}"))
            segmentOptions.add(primary)
        }

        // Merge segments into a single route option
        val merged = mergeSegmentRouteOptions(segmentOptions)
        return Result.success(listOf(merged))
    }

    // --- Helpers for merging ---

    private fun mergeSegmentRouteOptions(segments: List<RouteOption>): RouteOption {
        var totalDistance = 0.0
        var totalDurationMin = 0
        val mergedSteps = mutableListOf<DirectionStep>()
        val mergedPolyline = mutableListOf<Pair<Double, Double>>()

        segments.forEachIndexed { index, seg ->
            totalDistance += seg.totalDistanceMeters
            totalDurationMin += seg.totalDurationMinutes

            // Merge steps
            mergedSteps.addAll(seg.steps)

            // Merge polyline coordinates (dedupe join point)
            if (mergedPolyline.isEmpty()) {
                mergedPolyline.addAll(seg.polylineCoordinates)
            } else {
                val last = mergedPolyline.lastOrNull()
                val firstOfSeg = seg.polylineCoordinates.firstOrNull()
                val rest = if (last != null && firstOfSeg != null && areCoordinatesEqual(last, firstOfSeg)) {
                    seg.polylineCoordinates.drop(1) // skip duplicate join
                } else {
                    seg.polylineCoordinates
                }
                mergedPolyline.addAll(rest)
            }
        }

        // Create merged route option
        return RouteOption(
            id = "route_merged",
            name = "Combined Route",
            totalDistanceMeters = totalDistance,
            totalDurationMinutes = totalDurationMin.coerceAtLeast(1),
            steps = mergedSteps,
            polylineCoordinates = mergedPolyline,
            isPrimary = true
        )
    }

    private fun areCoordinatesEqual(a: Pair<Double, Double>, b: Pair<Double, Double>): Boolean {
        // Dedup using small tolerance to account for floating point differences
        val latEqual = abs(a.first - b.first) < 1e-6
        val lonEqual = abs(a.second - b.second) < 1e-6
        return latEqual && lonEqual
    }

    private fun convertRoutingResponseToRouteOptions(response: RoutingResponse): List<RouteOption> {
        return response.features.mapIndexed { index, feature ->
            val props = feature.properties

            // Convert route steps
            val steps = mutableListOf<DirectionStep>()
            props.legs.forEach { leg ->
                leg.steps.forEach { step ->
                    steps.add(
                        DirectionStep(
                            instruction = step.instruction.text,
                            distanceMeters = step.distance,
                            durationMinutes = (step.time / 60).toInt()
                                .coerceAtLeast(1), // Convert seconds to minutes
                            maneuver = mapInstructionTypeToManeuver(step.instruction.type ?: 0),
                            latitude = step.location.getOrNull(1) ?: 0.0,
                            longitude = step.location.getOrNull(0) ?: 0.0
                        )
                    )
                }
            }

            // Convert geometry coordinates to polyline
            val polylineCoordinates = feature.geometry.coordinates.map { coord ->
                Pair(coord[1], coord[0]) // Convert [lon, lat] to [lat, lon]
            }

            RouteOption(
                id = "route_${index}",
                name = when (index) {
                    0 -> "Route A"
                    1 -> "Route B"
                    2 -> "Route C"
                    else -> "Route ${('A' + index)}"
                },
                totalDistanceMeters = props.distance,
                totalDurationMinutes = (props.time / 60).toInt()
                    .coerceAtLeast(1), // Convert seconds to minutes
                steps = steps,
                polylineCoordinates = polylineCoordinates,
                isPrimary = index == 0 // First route is primary
            )
        }
    }

    private fun mapInstructionTypeToManeuver(instructionType: Int): ManeuverType {
        return when (instructionType) {
            0 -> ManeuverType.START
            1 -> ManeuverType.CONTINUE
            2 -> ManeuverType.TURN_RIGHT
            3 -> ManeuverType.TURN_LEFT
            4 -> ManeuverType.TURN_SLIGHT_RIGHT
            5 -> ManeuverType.TURN_SLIGHT_LEFT
            6 -> ManeuverType.TURN_SHARP_RIGHT
            7 -> ManeuverType.TURN_SHARP_LEFT
            8 -> ManeuverType.UTURN_LEFT
            9 -> ManeuverType.UTURN_RIGHT
            10 -> ManeuverType.KEEP_RIGHT
            11 -> ManeuverType.KEEP_LEFT
            12 -> ManeuverType.ROUNDABOUT
            13 -> ManeuverType.MERGE
            14 -> ManeuverType.ARRIVE
            else -> ManeuverType.CONTINUE
        }
    }
}