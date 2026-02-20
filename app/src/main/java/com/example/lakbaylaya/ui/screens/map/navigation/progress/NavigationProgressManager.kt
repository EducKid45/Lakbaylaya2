package com.example.lakbaylaya.ui.screens.map.navigation.progress

import android.location.Location
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.example.lakbaylaya.ui.screens.map.models.RouteOption
import com.example.lakbaylaya.ui.screens.map.navigation.arrival.DestinationArrivalDetector
import com.example.lakbaylaya.ui.screens.map.navigation.arrival.DestinationArrivalInfo
import com.example.lakbaylaya.ui.screens.map.navigation.arrival.DestinationProximityInfo
import kotlin.math.*

/**
 * Manages navigation progress using hybrid GPS + step detection
 *
 * Combines GPS location tracking with step counting to determine
 * when to advance to the next navigation step based on proximity thresholds.
 * Also includes destination arrival detection using precise GPS distance calculation.
 */
class NavigationProgressManager {

    // Current navigation state
    private var currentRoute: RouteOption? = null
    private var currentStepIndex = 0
    private var userLocation: Location? = null

    // Destination coordinates (separate from route steps)
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null

    // Threshold configuration
    private var stepThresholdMeters = 10.0 // Distance threshold to advance step
    private var gpsAccuracyThreshold = 20.0 // Only use GPS if accuracy is better than this
    private var finalDestinationThresholdMeters = 25.0 // Threshold for final arrival

    // Progress tracking
    private var totalDistanceCovered = 0.0
    private var stepDistanceCovered = 0.0

    // Destination arrival detection
    private val arrivalDetector = DestinationArrivalDetector()

    // Callbacks
    private var onStepAdvanced: ((DirectionStep, Int) -> Unit)? = null
    private var onProgressUpdate: ((Double, Double) -> Unit)? = null // (stepDistance, totalDistance)
    private var onNavigationComplete: (() -> Unit)? = null
    private var onDestinationArrived: ((DestinationArrivalInfo) -> Unit)? = null
    private var onDestinationProximity: ((DestinationProximityInfo) -> Unit)? = null

    /**
     * Initialize navigation with route and destination arrival detection
     */
    fun startNavigation(
        route: RouteOption,
        destinationLatitude: Double,
        destinationLongitude: Double,
        onStepAdvanced: (DirectionStep, Int) -> Unit,
        onProgressUpdate: (Double, Double) -> Unit,
        onNavigationComplete: () -> Unit,
        onDestinationArrived: ((DestinationArrivalInfo) -> Unit)? = null,
        onDestinationProximity: ((DestinationProximityInfo) -> Unit)? = null
    ) {
        currentRoute = route
        currentStepIndex = 0
        totalDistanceCovered = 0.0
        stepDistanceCovered = 0.0

        // Store the actual destination coordinates
        this.destinationLatitude = destinationLatitude
        this.destinationLongitude = destinationLongitude

        this.onStepAdvanced = onStepAdvanced
        this.onProgressUpdate = onProgressUpdate
        this.onNavigationComplete = onNavigationComplete
        this.onDestinationArrived = onDestinationArrived
        this.onDestinationProximity = onDestinationProximity

        // Configure destination arrival detection using provided destination coordinates
        if (route.steps.isNotEmpty()) {
            android.util.Log.d(
                "NavigationProgressManager",
                "Setting destination coordinates to: ($destinationLatitude, $destinationLongitude)"
            )

            arrivalDetector.setDestination(destinationLatitude, destinationLongitude)
            arrivalDetector.setArrivalThreshold(finalDestinationThresholdMeters) // 25 meters for destination arrival
            arrivalDetector.setSpeedThreshold(1.0) // Max 1.0 m/s for arrival confirmation (less strict)
            arrivalDetector.setSpeedValidationEnabled(false) // Disable speed validation for more reliable detection
            arrivalDetector.setRequiredStationaryDuration(2000L) // 2 seconds stationary (reduced)

            // Set up arrival detection callbacks
            arrivalDetector.setOnArrivalDetected { arrivalInfo ->
                android.util.Log.i("NavigationProgressManager", "🎯 DESTINATION ARRIVAL DETECTED!")
                onDestinationArrived?.invoke(arrivalInfo)
                onNavigationComplete?.invoke()
            }

            arrivalDetector.setOnProximityUpdate { proximityInfo ->
                android.util.Log.d(
                    "NavigationProgressManager",
                    "Proximity update: ${proximityInfo.distanceMeters.format(1)}m to destination, " +
                            "within threshold: ${proximityInfo.isWithinThreshold}"
                )
                onDestinationProximity?.invoke(proximityInfo)
            }
        }

        // Trigger initial step
        getCurrentStep()?.let { step ->
            onStepAdvanced(step, currentStepIndex)
        }

        android.util.Log.d("NavigationProgressManager", "Navigation started with destination arrival detection enabled")
    }

    /**
     * Update user location and check if step should advance or destination reached
     */
    fun updateLocation(location: Location) {
        val previousLocation = userLocation
        userLocation = location
        val route = currentRoute ?: return

        android.util.Log.d("NavigationProgressManager",
            "GPS Update: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}")

        // Always update destination arrival detection first
        arrivalDetector.updateLocation(location)

        // Check if we're at the final step and handle arrival detection
        val isAtFinalStep = currentStepIndex >= route.steps.size - 1
        if (isAtFinalStep) {
            // Use the actual destination coordinates instead of last step coordinates
            val destLat = destinationLatitude
            val destLng = destinationLongitude
            if (destLat != null && destLng != null) {
                val distanceToFinalDestination = calculateDistance(
                    location.latitude, location.longitude,
                    destLat, destLng
                )

                android.util.Log.i(
                    "NavigationProgressManager",
                    "🎯 AT FINAL STEP! Current step: $currentStepIndex/${route.steps.size - 1}, " +
                            "Distance to destination: ${distanceToFinalDestination.format(1)}m (threshold: ${finalDestinationThresholdMeters}m)"
                )

                // If close enough to final destination, mark as complete immediately
                if (distanceToFinalDestination <= finalDestinationThresholdMeters) {
                    android.util.Log.i(
                        "NavigationProgressManager",
                        "🎯 FINAL DESTINATION REACHED! Triggering completion..."
                    )
                    onNavigationComplete?.invoke()
                    return // Exit early, don't process regular step advancement
                }
            }
        }

        // Handle regular step progression (only if not at final step)
        if (!isAtFinalStep) {
            // Calculate distance moved since last update for regular step progression
            if (previousLocation != null) {
                val distanceMoved = calculateDistance(
                    previousLocation.latitude, previousLocation.longitude,
                    location.latitude, location.longitude
                )

                android.util.Log.d(
                    "NavigationProgressManager",
                    "Distance moved: ${distanceMoved}m, accuracy threshold: ${gpsAccuracyThreshold}m"
                )

                // Only count meaningful movements (filter GPS noise)
                if (distanceMoved > 0.5 && location.accuracy <= gpsAccuracyThreshold) {
                    stepDistanceCovered += distanceMoved
                    totalDistanceCovered += distanceMoved

                    android.util.Log.d(
                        "NavigationProgressManager",
                        "Step distance: ${stepDistanceCovered}m, Total: ${totalDistanceCovered}m"
                    )

                    // Trigger progress update
                    onProgressUpdate?.invoke(stepDistanceCovered, totalDistanceCovered)

                    // Check if we should advance to next step
                    checkStepAdvancement(location)
                } else {
                    android.util.Log.d(
                        "NavigationProgressManager",
                        "Movement filtered out - too small (${distanceMoved}m) or low accuracy (${location.accuracy}m)"
                    )
                }
            } else {
                android.util.Log.d(
                    "NavigationProgressManager",
                    "First location update - no previous location"
                )
            }
        }
    }

    /**
     * Update step counter progress (for display purposes)
     */
    fun updateStepCount(stepCount: Int, estimatedDistance: Double) {
        // Step counter provides additional progress info for UI
        // GPS is primary for navigation advancement
        // This could be used for fitness tracking display
    }

    /**
     * Check if user is close enough to advance to next step
     */
    private fun checkStepAdvancement(location: Location) {
        val route = currentRoute ?: return
        val nextStepIndex = currentStepIndex + 1

        // Check if there's a next step
        if (nextStepIndex >= route.steps.size) {
            return // Already at last step
        }

        val nextStep = route.steps[nextStepIndex]

        // Calculate distance to NEXT step's endpoint (where we need to go)
        val distanceToNextPoint = calculateDistance(
            location.latitude, location.longitude,
            nextStep.latitude, nextStep.longitude
        )

        android.util.Log.d("NavigationProgressManager",
            "Distance to next step (${nextStepIndex}): ${distanceToNextPoint}m, threshold: ${stepThresholdMeters}m")

        // Check if within threshold to advance
        if (distanceToNextPoint <= stepThresholdMeters) {
            android.util.Log.d("NavigationProgressManager", "Threshold reached! Advancing to step $nextStepIndex")
            advanceToNextStep()
        } else {
            // Additional check: if user has walked a reasonable distance past current step,
            // advance anyway (fallback for GPS inaccuracy)
            val currentStep = getCurrentStep()
            if (currentStep != null) {
                val distanceFromCurrentStep = calculateDistance(
                    location.latitude, location.longitude,
                    currentStep.latitude, currentStep.longitude
                )

                // If user is far from current step and has covered significant distance
                if (distanceFromCurrentStep > stepThresholdMeters * 2 && stepDistanceCovered > currentStep.distanceMeters * 0.8) {
                    android.util.Log.d("NavigationProgressManager",
                        "Fallback advancement: User is ${distanceFromCurrentStep}m from current step and covered ${stepDistanceCovered}m/${currentStep.distanceMeters}m")
                    advanceToNextStep()
                }
            }
        }
    }

    /**
     * Advance to the next navigation step
     */
    private fun advanceToNextStep() {
        val route = currentRoute ?: return

        if (currentStepIndex < route.steps.size - 1) {
            currentStepIndex++
            stepDistanceCovered = 0.0 // Reset step distance

            val nextStep = getCurrentStep()
            if (nextStep != null) {
                android.util.Log.d("NavigationProgressManager",
                    "Successfully advanced to step $currentStepIndex: ${nextStep.instruction}")
                onStepAdvanced?.invoke(nextStep, currentStepIndex)
            }
        } else {
            // Navigation complete
            android.util.Log.d("NavigationProgressManager", "Navigation complete!")
            onNavigationComplete?.invoke()
        }
    }

    /**
     * Manually jump to specific step (for debugging or user interaction)
     */
    fun jumpToStep(stepIndex: Int) {
        val route = currentRoute ?: return

        if (stepIndex in route.steps.indices) {
            currentStepIndex = stepIndex
            stepDistanceCovered = 0.0

            val step = getCurrentStep()
            if (step != null) {
                onStepAdvanced?.invoke(step, currentStepIndex)
            }
        }
    }

    /**
     * Get current navigation step
     */
    fun getCurrentStep(): DirectionStep? {
        val route = currentRoute ?: return null
        return if (currentStepIndex < route.steps.size) {
            route.steps[currentStepIndex]
        } else null
    }

    /**
     * Get current step index
     */
    fun getCurrentStepIndex(): Int = currentStepIndex

    /**
     * Get total steps in route
     */
    fun getTotalSteps(): Int = currentRoute?.steps?.size ?: 0

    /**
     * Get progress percentage for current step
     */
    fun getStepProgress(): Float {
        val step = getCurrentStep() ?: return 0f
        val stepTotalDistance = step.distanceMeters.toDouble()

        return if (stepTotalDistance > 0) {
            (stepDistanceCovered / stepTotalDistance).coerceIn(0.0, 1.0).toFloat()
        } else 0f
    }

    /**
     * Get remaining distance to complete current step
     */
    fun getRemainingStepDistance(): Double {
        val step = getCurrentStep() ?: return 0.0
        val remaining = step.distanceMeters - stepDistanceCovered
        return maxOf(0.0, remaining)
    }

    /**
     * Stop navigation and reset state
     */
    fun stopNavigation() {
        currentRoute = null
        currentStepIndex = 0
        totalDistanceCovered = 0.0
        stepDistanceCovered = 0.0
        userLocation = null

        // Stop arrival detection
        arrivalDetector.stop()

        onStepAdvanced = null
        onProgressUpdate = null
        onNavigationComplete = null
        onDestinationArrived = null
        onDestinationProximity = null
    }

    /**
     * Configure destination arrival detection thresholds
     */
    fun configureArrivalDetection(
        arrivalThresholdMeters: Double = 20.0,
        speedThresholdMps: Double = 0.5,
        enableSpeedValidation: Boolean = true,
        requiredStationaryDurationMs: Long = 3000L
    ) {
        arrivalDetector.setArrivalThreshold(arrivalThresholdMeters)
        arrivalDetector.setSpeedThreshold(speedThresholdMps)
        arrivalDetector.setSpeedValidationEnabled(enableSpeedValidation)
        arrivalDetector.setRequiredStationaryDuration(requiredStationaryDurationMs)
    }

    /**
     * Check if user has arrived at destination
     */
    fun hasArrivedAtDestination(): Boolean {
        return arrivalDetector.hasDetectedArrival()
    }

    /**
     * Get current distance to destination
     */
    fun getDistanceToDestination(): Double? {
        val location = userLocation ?: return null
        return arrivalDetector.getCurrentDistanceToDestination(location)
    }

    /**
     * Check if currently within arrival threshold
     */
    fun isWithinDestinationThreshold(): Boolean {
        return arrivalDetector.isWithinArrivalThreshold()
    }

    /**
     * Configure thresholds
     */
    fun setStepThreshold(meters: Double) {
        stepThresholdMeters = meters
    }

    fun setGpsAccuracyThreshold(meters: Double) {
        gpsAccuracyThreshold = meters
    }

    fun setFinalDestinationThreshold(meters: Double) {
        finalDestinationThresholdMeters = meters
        arrivalDetector.setArrivalThreshold(meters)
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLatRad / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Get navigation statistics
     */
    data class NavigationStats(
        val totalDistance: Double,
        val stepDistance: Double,
        val currentStep: Int,
        val totalSteps: Int,
        val stepProgress: Float,
        val remainingDistance: Double
    )

    fun getNavigationStats(): NavigationStats {
        return NavigationStats(
            totalDistance = totalDistanceCovered,
            stepDistance = stepDistanceCovered,
            currentStep = currentStepIndex,
            totalSteps = getTotalSteps(),
            stepProgress = getStepProgress(),
            remainingDistance = getRemainingStepDistance()
        )
    }

    /**
     * Manual trigger for arrival testing (for debugging purposes)
     * This bypasses all distance checks and immediately triggers completion
     */
    fun forceArrival() {
        android.util.Log.i("NavigationProgressManager", "🎯 FORCE ARRIVAL TRIGGERED (DEBUG)")
        onNavigationComplete?.invoke()
    }

    /**
     * Get debug information about current navigation state
     */
    fun getDebugInfo(): String {
        val route = currentRoute
        val location = userLocation
        if (route == null || location == null) {
            return "Navigation not active or no location data"
        }

        val destLat = destinationLatitude
        val destLng = destinationLongitude
        val isAtFinalStep = currentStepIndex >= route.steps.size - 1
        val distanceToDestination = if (destLat != null && destLng != null) {
            calculateDistance(location.latitude, location.longitude, destLat, destLng)
        } else null

        return buildString {
            appendLine("=== Navigation Debug Info ===")
            appendLine("Current step: $currentStepIndex/${route.steps.size - 1}")
            appendLine("Is at final step: $isAtFinalStep")
            appendLine("Distance to destination: ${distanceToDestination?.format(1)}m")
            appendLine("Threshold: ${finalDestinationThresholdMeters}m")
            appendLine("Within threshold: ${(distanceToDestination ?: Double.MAX_VALUE) <= finalDestinationThresholdMeters}")
            appendLine("Location accuracy: ${location.accuracy}m")
            if (destLat != null && destLng != null) {
                appendLine("Destination coords: ($destLat, $destLng)")
            }
            appendLine("User coords: (${location.latitude}, ${location.longitude})")
            appendLine("Arrival detector state: within=${arrivalDetector.isWithinArrivalThreshold()}, arrived=${arrivalDetector.hasDetectedArrival()}")
        }
    }

    // Extension function for number formatting
    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}
