package com.example.lakbaylaya.ui.screens.map.models

import java.util.Locale

/**
 * Data class representing complete direction information for a route
 *
 * @property origin Starting location
 * @property destination End location
 * @property stops Optional intermediate stops (can be edited/removed/reordered)
 * @property routes List of available route options (e.g., Route A, Route B)
 * @property selectedRouteIndex Index of currently selected route (default 0)
 * @property sourcePlace Original place that triggered directions (to restore on back)
 */
data class DirectionData(
    val origin: RoutePoint,
    val destination: RoutePoint,
    val stops: List<RoutePoint> = emptyList(),
    val routes: List<RouteOption> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val sourcePlace: SearchResult? = null
) {
    /**
     * Get currently selected route
     */
    fun getSelectedRoute(): RouteOption? {
        return routes.getOrNull(selectedRouteIndex)
    }

    /**
     * Create new DirectionData with selected route changed
     */
    fun withSelectedRoute(index: Int): DirectionData {
        return copy(selectedRouteIndex = index.coerceIn(0, routes.size - 1))
    }

    /**
     * Add a stop at specified index
     */
    fun addStop(stop: RoutePoint, index: Int = stops.size): DirectionData {
        val newStops = stops.toMutableList()
        newStops.add(index.coerceIn(0, stops.size), stop)
        return copy(stops = newStops)
    }

    /**
     * Remove stop at specified index
     */
    fun removeStop(index: Int): DirectionData {
        if (index !in stops.indices) return this
        val newStops = stops.toMutableList()
        newStops.removeAt(index)
        return copy(stops = newStops)
    }

    /**
     * Swap origin and destination
     */
    fun swapOriginDestination(): DirectionData {
        return copy(origin = destination, destination = origin)
    }
}

/**
 * Data class representing a point in a route (origin, destination, or stop)
 *
 * @property latitude Latitude coordinate
 * @property longitude Longitude coordinate
 * @property name Display name (e.g., "Your Location", place name)
 * @property address Full address
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val address: String = ""
)

/**
 * Data class representing a route option (e.g., Route A, Route B)
 *
 * @property id Unique identifier
 * @property name Display name (e.g., "Route A", "Route B")
 * @property totalDistanceMeters Total distance in meters
 * @property totalDurationMinutes Estimated total duration in minutes
 * @property steps List of turn-by-turn steps
 * @property polylineCoordinates List of coordinates for drawing route line on map
 * @property isPrimary Whether this is the primary/recommended route
 */
data class RouteOption(
    val id: String,
    val name: String,
    val totalDistanceMeters: Double,
    val totalDurationMinutes: Int,
    val steps: List<DirectionStep> = emptyList(),
    val polylineCoordinates: List<Pair<Double, Double>> = emptyList(),
    val isPrimary: Boolean = false
) {
    /**
     * Get formatted total distance
     */
    fun getFormattedDistance(): String {
        return when {
            totalDistanceMeters < 1000 -> "${formatNumber(totalDistanceMeters.toInt())} m"
            else -> String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000)
        }
    }

    /**
     * Get formatted total duration
     */
    fun getFormattedDuration(): String {
        return when {
            totalDurationMinutes < 60 -> "${formatNumber(totalDurationMinutes)} min"
            else -> {
                val hours = totalDurationMinutes / 60
                val mins = totalDurationMinutes % 60
                if (mins == 0) "${formatNumber(hours)} hr" else "${formatNumber(hours)} hr ${formatNumber(mins)} min"
            }
        }
    }

    /**
     * Get estimated steps count
     */
    fun getEstimatedSteps(): String {
        val steps = (totalDistanceMeters / 0.762).toInt()
        return formatNumber(steps)
    }

    /**
     * Format number with comma separators for thousands
     */
    private fun formatNumber(number: Int): String {
        return String.format(Locale.US, "%,d", number)
    }
}

/**
 * Data class representing a single turn-by-turn direction step
 *
 * @property instruction Human-readable instruction (e.g., "Turn left onto Main St")
 * @property distanceMeters Distance for this step in meters
 * @property durationMinutes Estimated duration for this step in minutes
 * @property maneuver Type of maneuver (turn, continue, etc.)
 * @property latitude Latitude where this step occurs
 * @property longitude Longitude where this step occurs
 * @property bearingBefore Compass bearing at the start of this step (0–359°).
 * @property bearingAfter  Compass bearing at the end of this step (0–359°).
 * @property street        Street or road name for this step (null if unknown).
 * @property osmHighway    OSM highway tag (e.g. "primary", "crossing", "traffic_signals").
 * @property osmJunction   OSM junction tag (e.g. "roundabout").
 * @property osmFootway    OSM footway tag (e.g. "sidewalk").
 * @property spokenInstruction Pre-built TTS instruction string.
 */
data class DirectionStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationMinutes: Int,
    val maneuver: ManeuverType,
    val latitude: Double,
    val longitude: Double,
    val bearingBefore: Double = 0.0,
    val bearingAfter: Double = 0.0,
    val street: String? = null,
    val osmHighway: String? = null,
    val osmJunction: String? = null,
    val osmFootway: String? = null,
    val spokenInstruction: String = ""
) {
    /**
     * Get formatted distance for this step
     */
    fun getFormattedDistance(): String {
        return when {
            distanceMeters < 1000 -> "${formatNumber(distanceMeters.toInt())} m"
            else -> String.format(Locale.US, "%.1f km", distanceMeters / 1000)
        }
    }

    /**
     * Get estimated steps for this segment
     */
    fun getEstimatedSteps(): String {
        val steps = (distanceMeters / 0.762).toInt()
        return formatNumber(steps)
    }

    /**
     * Format number with comma separators for thousands
     */
    private fun formatNumber(number: Int): String {
        return String.format(Locale.US, "%,d", number)
    }
}

/**
 * Enum representing different maneuver types for direction icons
 */
enum class ManeuverType {
    START,
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    CONTINUE,
    MERGE,
    ROUNDABOUT,
    ARRIVE,
    UTURN_LEFT,
    UTURN_RIGHT,
    KEEP_LEFT,
    KEEP_RIGHT
}