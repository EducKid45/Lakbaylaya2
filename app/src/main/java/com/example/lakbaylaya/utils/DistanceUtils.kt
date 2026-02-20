package com.example.lakbaylaya.utils

import kotlin.math.*

/**
 * Utility object for distance-related calculations and formatting
 */
object DistanceUtils {

    /**
     * Formats distance in meters to a human-readable string
     *
     * @param meters Distance in meters
     * @return Formatted distance string (e.g., "1.5 km" or "500 m")
     */
    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.roundToInt()} m"
            else -> {
                val kilometers = meters / 1000
                String.format("%.1f km", kilometers)
            }
        }
    }

    /**
     * Calculates the distance between two geographic coordinates using the Haversine formula
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c * 1000 // Convert to meters
    }

    /**
     * Formats distance for accessibility announcements
     *
     * @param meters Distance in meters
     * @return Accessibility-friendly distance description
     */
    fun formatDistanceForAccessibility(meters: Double): String {
        return when {
            meters < 100 -> "nearby, ${meters.roundToInt()} meters"
            meters < 1000 -> "${meters.roundToInt()} meters away"
            else -> {
                val kilometers = meters / 1000
                val roundedKm = String.format("%.1f", kilometers)
                "$roundedKm kilometers away"
            }
        }
    }

    /**
     * Estimates walking time based on distance
     * Average walking speed: 5 km/h (1.39 m/s)
     *
     * @param meters Distance in meters
     * @return Formatted walking time (e.g., "5 min walk" or "1 hr 15 min walk")
     */
    fun estimateWalkingTime(meters: Double): String {
        // Average walking speed in m/s
        val walkingSpeedMps = 1.39

        val totalSeconds = (meters / walkingSpeedMps).roundToInt()
        val minutes = totalSeconds / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            minutes < 1 -> "< 1 min walk"
            minutes < 60 -> "$minutes min walk"
            hours == 1 && remainingMinutes == 0 -> "1 hr walk"
            remainingMinutes == 0 -> "$hours hrs walk"
            hours == 1 -> "1 hr $remainingMinutes min walk"
            else -> "$hours hrs $remainingMinutes min walk"
        }
    }

    /**
     * Estimates number of steps based on distance
     * Average step length: 0.76 meters (roughly 1,315 steps per km)
     *
     * @param meters Distance in meters
     * @return Formatted step count (e.g., "1,500 steps" or "~5k steps")
     */
    fun estimateSteps(meters: Double): String {
        // Average step length in meters
        val averageStepLengthM = 0.76

        val totalSteps = (meters / averageStepLengthM).roundToInt()

        return when {
            totalSteps < 1000 -> "$totalSteps steps"
            totalSteps < 10000 -> "${String.format("%.1f", totalSteps / 1000.0)}k steps"
            else -> "~${(totalSteps / 1000)}k steps"
        }
    }
}
