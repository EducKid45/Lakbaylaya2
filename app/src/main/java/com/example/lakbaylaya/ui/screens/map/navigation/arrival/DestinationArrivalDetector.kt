package com.example.lakbaylaya.ui.screens.map.navigation.arrival

import android.location.Location
import kotlin.math.*

/**
 * GPS-based destination arrival detection using real-time straight-line distance calculation.
 *
 * This detector uses the Haversine formula to calculate the precise straight-line distance
 * between the user's current GPS location and the destination coordinates. It triggers
 * arrival when the user gets within a specified threshold distance and optionally
 * validates that the user's speed is low enough to avoid false positives.
 *
 * Features:
 * - Real-time distance calculation using GPS coordinates
 * - Configurable arrival threshold (default: 20 meters)
 * - Optional speed validation to avoid false positives while passing by
 * - Hysteresis to prevent rapid toggling of arrival state
 * - Speed averaging to smooth out GPS noise
 */
class DestinationArrivalDetector {

    // Configuration
    private var arrivalThresholdMeters = 20.0 // Distance threshold for arrival detection
    private var speedThresholdMps = 0.5 // Maximum speed to confirm arrival (m/s)
    private var enableSpeedValidation = true // Whether to check user speed
    private var hysteresisMeters = 5.0 // Extra distance to prevent toggling
    private var requiredStationaryDurationMs = 3000L // Time to be stationary before confirming arrival (3 seconds)

    // State tracking
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null
    private var isWithinThreshold = false
    private var hasArrived = false
    private var lastLocationUpdate: Long = 0
    private var stationaryStartTime: Long? = null

    // Speed calculation (using moving average)
    private val speedHistory = mutableListOf<Float>()
    private val maxSpeedHistorySize = 5
    private var lastLocation: Location? = null
    private var lastLocationTime: Long = 0

    // Callbacks
    private var onArrivalDetected: ((DestinationArrivalInfo) -> Unit)? = null
    private var onProximityUpdate: ((DestinationProximityInfo) -> Unit)? = null

    /**
     * Set the destination coordinates for arrival detection
     */
    fun setDestination(latitude: Double, longitude: Double) {
        destinationLatitude = latitude
        destinationLongitude = longitude
        reset() // Reset state when destination changes
    }

    /**
     * Update user's current location and check for arrival
     */
    fun updateLocation(location: Location) {
        val destLat = destinationLatitude ?: return
        val destLon = destinationLongitude ?: return

        val currentTime = System.currentTimeMillis()
        lastLocationUpdate = currentTime

        // Calculate straight-line distance using Haversine formula
        val distanceToDestination = calculateHaversineDistance(
            location.latitude, location.longitude,
            destLat, destLon
        )

        // Calculate current speed (using GPS or manual calculation)
        val currentSpeed = calculateCurrentSpeed(location, currentTime)

        // Update proximity state
        val proximityInfo = DestinationProximityInfo(
            distanceMeters = distanceToDestination,
            speedMps = currentSpeed,
            isWithinThreshold = distanceToDestination <= arrivalThresholdMeters,
            isStationary = currentSpeed <= speedThresholdMps,
            arrivalThreshold = arrivalThresholdMeters,
            speedThreshold = speedThresholdMps
        )

        onProximityUpdate?.invoke(proximityInfo)

        // Check arrival conditions
        checkArrivalConditions(
            distanceToDestination = distanceToDestination,
            currentSpeed = currentSpeed,
            currentTime = currentTime,
            location = location
        )
    }

    /**
     * Check if arrival conditions are met and trigger callback if arrived
     */
    private fun checkArrivalConditions(
        distanceToDestination: Double,
        currentSpeed: Float,
        currentTime: Long,
        location: Location
    ) {
        val withinDistance = distanceToDestination <= arrivalThresholdMeters
        val isSlowEnough = !enableSpeedValidation || currentSpeed <= speedThresholdMps

        when {
            // First time entering threshold
            !isWithinThreshold && withinDistance -> {
                isWithinThreshold = true

                if (isSlowEnough) {
                    // Start counting stationary time
                    stationaryStartTime = currentTime
                    android.util.Log.d("ArrivalDetector",
                        "Entered arrival threshold (${distanceToDestination.format(1)}m), " +
                        "speed: ${currentSpeed.format(2)}m/s. Starting stationary timer.")
                } else {
                    android.util.Log.d("ArrivalDetector",
                        "Entered threshold but moving too fast (${currentSpeed.format(2)}m/s > ${speedThresholdMps}m/s)")
                }
            }

            // Within threshold and conditions are good
            isWithinThreshold && withinDistance && isSlowEnough -> {
                val stationaryStart = stationaryStartTime
                if (stationaryStart != null) {
                    val stationaryDuration = currentTime - stationaryStart

                    if (!hasArrived && stationaryDuration >= requiredStationaryDurationMs) {
                        // Arrival confirmed!
                        hasArrived = true
                        val arrivalInfo = DestinationArrivalInfo(
                            arrivalTime = currentTime,
                            finalDistanceMeters = distanceToDestination,
                            arrivalLocation = location,
                            destinationLatitude = destinationLatitude!!,
                            destinationLongitude = destinationLongitude!!,
                            stationaryDurationMs = stationaryDuration
                        )

                        android.util.Log.i("ArrivalDetector",
                            "🎯 ARRIVAL CONFIRMED! Distance: ${distanceToDestination.format(1)}m, " +
                            "Speed: ${currentSpeed.format(2)}m/s, Stationary: ${stationaryDuration}ms")

                        onArrivalDetected?.invoke(arrivalInfo)
                    }
                }
            }

            // Within threshold but moving too fast
            isWithinThreshold && withinDistance && !isSlowEnough -> {
                // Reset stationary timer if moving too fast
                stationaryStartTime = null
                android.util.Log.d("ArrivalDetector",
                    "Within threshold but moving too fast (${currentSpeed.format(2)}m/s). Resetting timer.")
            }

            // Moved outside threshold (with hysteresis)
            isWithinThreshold && distanceToDestination > (arrivalThresholdMeters + hysteresisMeters) -> {
                android.util.Log.d("ArrivalDetector",
                    "Left arrival threshold (${distanceToDestination.format(1)}m > ${(arrivalThresholdMeters + hysteresisMeters).format(1)}m)")
                isWithinThreshold = false
                stationaryStartTime = null
                // Don't reset hasArrived - once arrived, stay arrived until reset
            }
        }
    }

    /**
     * Calculate current speed using location data
     */
    private fun calculateCurrentSpeed(location: Location, currentTime: Long): Float {
        // First try to use GPS-provided speed if available and accurate
        if (location.hasSpeed() && location.speed >= 0) {
            val gpsSpeed = location.speed
            addSpeedToHistory(gpsSpeed)
            return getAverageSpeed()
        }

        // Fallback: Calculate speed manually using location changes
        val lastLoc = lastLocation
        val lastTime = lastLocationTime

        if (lastLoc != null && lastTime > 0) {
            val timeDelta = (currentTime - lastTime) / 1000.0 // seconds

            if (timeDelta > 0.5) { // Only calculate if enough time has passed
                val distance = calculateHaversineDistance(
                    lastLoc.latitude, lastLoc.longitude,
                    location.latitude, location.longitude
                )
                val speed = (distance / timeDelta).toFloat()

                // Filter out unrealistic speeds (likely GPS errors)
                if (speed <= 50.0f) { // Max 50 m/s (180 km/h) for walking/driving
                    addSpeedToHistory(speed)

                    // Update last location for next calculation
                    lastLocation = location
                    lastLocationTime = currentTime

                    return getAverageSpeed()
                }
            }
        }

        // Store location for next calculation
        lastLocation = location
        lastLocationTime = currentTime

        // Return existing average or 0 if no valid speed data
        return getAverageSpeed()
    }

    /**
     * Add speed to moving average calculation
     */
    private fun addSpeedToHistory(speed: Float) {
        speedHistory.add(speed)
        if (speedHistory.size > maxSpeedHistorySize) {
            speedHistory.removeAt(0)
        }
    }

    /**
     * Get averaged speed to smooth out GPS noise
     */
    private fun getAverageSpeed(): Float {
        return if (speedHistory.isNotEmpty()) {
            speedHistory.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate straight-line distance between two coordinates using Haversine formula
     * This is the most accurate method for GPS distance calculation on Earth's surface
     */
    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLatRad / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    /**
     * Alternative: Use Android's Location.distanceBetween() method
     * This method is equivalent to Haversine but uses Android's built-in implementation
     */
    private fun calculateDistanceAndroid(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    /**
     * Set arrival detection threshold
     */
    fun setArrivalThreshold(meters: Double) {
        arrivalThresholdMeters = meters
    }

    /**
     * Set speed threshold for arrival validation
     */
    fun setSpeedThreshold(metersPerSecond: Double) {
        speedThresholdMps = metersPerSecond
    }

    /**
     * Enable or disable speed validation
     */
    fun setSpeedValidationEnabled(enabled: Boolean) {
        enableSpeedValidation = enabled
    }

    /**
     * Set hysteresis distance to prevent rapid toggling
     */
    fun setHysteresis(meters: Double) {
        hysteresisMeters = meters
    }

    /**
     * Set required stationary duration before confirming arrival
     */
    fun setRequiredStationaryDuration(milliseconds: Long) {
        requiredStationaryDurationMs = milliseconds
    }

    /**
     * Set arrival callback
     */
    fun setOnArrivalDetected(callback: (DestinationArrivalInfo) -> Unit) {
        onArrivalDetected = callback
    }

    /**
     * Set proximity update callback (called on every location update)
     */
    fun setOnProximityUpdate(callback: (DestinationProximityInfo) -> Unit) {
        onProximityUpdate = callback
    }

    /**
     * Check if currently within arrival threshold
     */
    fun isWithinArrivalThreshold(): Boolean = isWithinThreshold

    /**
     * Check if arrival has been detected
     */
    fun hasDetectedArrival(): Boolean = hasArrived

    /**
     * Get current distance to destination (if location and destination are set)
     */
    fun getCurrentDistanceToDestination(userLocation: Location): Double? {
        val destLat = destinationLatitude ?: return null
        val destLon = destinationLongitude ?: return null

        return calculateHaversineDistance(
            userLocation.latitude, userLocation.longitude,
            destLat, destLon
        )
    }

    /**
     * Reset arrival detection state
     */
    fun reset() {
        isWithinThreshold = false
        hasArrived = false
        stationaryStartTime = null
        speedHistory.clear()
        lastLocation = null
        lastLocationTime = 0
    }

    /**
     * Stop arrival detection and clear destination
     */
    fun stop() {
        destinationLatitude = null
        destinationLongitude = null
        reset()
        onArrivalDetected = null
        onProximityUpdate = null
    }

    // Extension function for number formatting
    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
    private fun Float.format(digits: Int): String = "%.${digits}f".format(this)
}

/**
 * Information provided when arrival is detected
 */
data class DestinationArrivalInfo(
    val arrivalTime: Long,
    val finalDistanceMeters: Double,
    val arrivalLocation: Location,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val stationaryDurationMs: Long
)

/**
 * Real-time proximity information (updated on every location update)
 */
data class DestinationProximityInfo(
    val distanceMeters: Double,
    val speedMps: Float,
    val isWithinThreshold: Boolean,
    val isStationary: Boolean,
    val arrivalThreshold: Double,
    val speedThreshold: Double
)
