package com.example.lakbaylaya.ui.screens.map.models

import android.location.Location

/**
 * Sealed class representing the navigation state
 *
 * Navigation is a distinct mode from route preview/planning.
 * It activates when the user presses "Start" and ends when they stop navigation.
 */
sealed class NavigationState {
    /**
     * Navigation is not active
     */
    data object Inactive : NavigationState()

    /**
     * Navigation is active with real-time step-by-step guidance
     *
     * @property currentStepIndex Index of the current step being navigated
     * @property routeOption The route being navigated
     * @property currentLocation Current GPS location of the user
     * @property distanceCovered Distance covered in the current step (meters)
     * @property totalDistanceCovered Total distance covered since navigation start (meters)
     * @property stepCount Total steps counted by pedometer
     * @property isMuted Whether TTS voice guidance is muted
     * @property mapMode Current map viewing mode
     * @property isLocationTracking Whether GPS location tracking is active
     */
    data class Active(
        val currentStepIndex: Int = 0,
        val routeOption: RouteOption,
        val currentLocation: Location? = null,
        val distanceCovered: Double = 0.0,
        val totalDistanceCovered: Double = 0.0,
        val stepCount: Int = 0,
        val isMuted: Boolean = false,
        val mapMode: MapViewMode = MapViewMode.MODE_2D,
        val isLocationTracking: Boolean = false,
        val isCompleted: Boolean = false
    ) : NavigationState() {

        /**
         * Get the current navigation step
         */
        fun getCurrentStep(): DirectionStep? {
            return routeOption.steps.getOrNull(currentStepIndex)
        }

        /**
         * Get the next navigation step (if available)
         */
        fun getNextStep(): DirectionStep? {
            return routeOption.steps.getOrNull(currentStepIndex + 1)
        }

        /**
         * Check if there are more steps ahead
         */
        fun hasNextStep(): Boolean {
            return currentStepIndex < routeOption.steps.size - 1
        }

        /**
         * Get total number of steps
         */
        fun getTotalSteps(): Int {
            return routeOption.steps.size
        }

        /**
         * Get remaining distance in the current step
         */
        fun getRemainingDistance(): Double {
            val currentStep = getCurrentStep() ?: return 0.0
            return (currentStep.distanceMeters - distanceCovered).coerceAtLeast(0.0)
        }

        /**
         * Get progress percentage for the current step
         */
        fun getStepProgress(): Float {
            val currentStep = getCurrentStep() ?: return 0f
            if (currentStep.distanceMeters <= 0) return 0f
            return (distanceCovered / currentStep.distanceMeters).toFloat().coerceIn(0f, 1f)
        }

        /**
         * Advance to the next step
         */
        fun advanceToNextStep(): Active? {
            if (!hasNextStep()) return null
            return copy(
                currentStepIndex = currentStepIndex + 1,
                distanceCovered = 0.0 // Reset step distance
            )
        }

        /**
         * Update GPS location
         */
        fun updateLocation(location: Location): Active {
            return copy(currentLocation = location, isLocationTracking = true)
        }

        /**
         * Update distance covered in current step
         */
        fun updateStepDistance(stepDistance: Double, totalDistance: Double): Active {
            return copy(
                distanceCovered = stepDistance,
                totalDistanceCovered = totalDistance
            )
        }

        /**
         * Update step count from pedometer
         */
        fun updateStepCount(steps: Int): Active {
            return copy(stepCount = steps)
        }

        /**
         * Jump to specific step (for manual control or debugging)
         */
        fun jumpToStep(stepIndex: Int): Active? {
            if (stepIndex !in 0 until routeOption.steps.size) return null
            return copy(
                currentStepIndex = stepIndex,
                distanceCovered = 0.0
            )
        }

        /**
         * Toggle mute state
         */
        fun toggleMute(): Active {
            return copy(isMuted = !isMuted)
        }

        /**
         * Toggle map mode
         */
        fun toggleMapMode(): Active {
            val newMode = when (mapMode) {
                MapViewMode.MODE_2D -> MapViewMode.MODE_3D
                MapViewMode.MODE_3D -> MapViewMode.MODE_2D
            }
            return copy(mapMode = newMode)
        }
    }
}

/**
 * Enum representing map viewing modes during navigation
 */
enum class MapViewMode {
    MODE_2D,  // Top-down view
    MODE_3D   // Perspective/tilted view
}
