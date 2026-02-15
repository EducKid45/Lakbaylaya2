package com.example.lakbaylaya.ui.screens.map.viewmodel

import com.example.lakbaylaya.ui.screens.map.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages active navigation lifecycle and GPS-driven updates.
 * Responsibilities moved from MapViewModel for separation of concerns:
 * - startNavigation
 * - updateNavigationLocation
 * - updateNavigationStepCount
 * - stopNavigation
 * - finishNavigation
 *
 * This manager owns the NavigationProgressManager instance and handles
 * step advancement callbacks which update the provided MapState flow.
 */
class MapNavigationManager(
    private val state: MutableStateFlow<MapState>
) {

    private var navigationProgressManager: com.example.lakbaylaya.ui.screens.map.navigation.progress.NavigationProgressManager? =
        null

    /** Start active navigation using the currently selected route in state.directionData */
    fun startNavigation() {
        val dirData = state.value.directionData ?: return
        val selectedRoute = dirData.getSelectedRoute() ?: return

        if (selectedRoute.steps.isEmpty()) {
            android.util.Log.w("MapNavigationManager", "Cannot start navigation: no steps in route")
            return
        }

        // Debug log
        android.util.Log.d(
            "MapNavigationManager",
            "Starting navigation with ${selectedRoute.steps.size} steps:"
        )
        selectedRoute.steps.forEachIndexed { index, step ->
            android.util.Log.d(
                "MapNavigationManager",
                "Step $index: ${step.instruction} at (${step.latitude}, ${step.longitude})"
            )
        }

        // Initialize navigation progress manager
        navigationProgressManager =
            com.example.lakbaylaya.ui.screens.map.navigation.progress.NavigationProgressManager()
                .apply {
                    setStepThreshold(5.0)
                    setGpsAccuracyThreshold(15.0)

                    startNavigation(
                        route = selectedRoute,
                        onStepAdvanced = { step, stepIndex ->
                            onNavigationStepAdvanced(step, stepIndex)
                        },
                        onProgressUpdate = { stepDistance, totalDistance ->
                            onNavigationProgressUpdate(stepDistance, totalDistance)
                        },
                        onNavigationComplete = {
                            onNavigationComplete()
                        },
                        onDestinationArrived = {
                            onNavigationComplete()
                        }
                    )
                }

        // Set navigation state in flow
        val initialStep = selectedRoute.steps.firstOrNull()
        val navigationState = NavigationState.Active(
            currentStepIndex = 0,
            routeOption = selectedRoute,
            currentLocation = state.value.currentLocation?.let { loc ->
                android.location.Location("").apply {
                    latitude = loc.latitude
                    longitude = loc.longitude
                }
            },
            distanceCovered = 0.0,
            totalDistanceCovered = 0.0,
            stepCount = 0,
            isMuted = false,
            mapMode = MapViewMode.MODE_2D,
            isLocationTracking = false
        )

        state.update {
            it.copy(
                navigationState = navigationState,
                bottomSheetState = BottomSheetState.Hidden
            )
        }

        android.util.Log.d(
            "MapNavigationManager",
            "Navigation started with ${selectedRoute.steps.size} steps"
        )
        initialStep?.let { step ->
            android.util.Log.d(
                "MapNavigationManager",
                "Initial step ready: ${step.instruction}"
            )
        }
    }

    /** Called by MapScreen or ViewModel when GPS location updates arrive */
    fun updateNavigationLocation(location: android.location.Location) {
        val navState = state.value.navigationState
        if (navState !is NavigationState.Active) return

        android.util.Log.d(
            "MapNavigationManager",
            "GPS Update: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m"
        )

        // Update navigation state with new location
        state.update { it.copy(navigationState = navState.updateLocation(location)) }

        // Update progress manager
        navigationProgressManager?.updateLocation(location)

        try {
            val withinThreshold = navigationProgressManager?.isWithinDestinationThreshold() == true
            if (withinThreshold) {
                android.util.Log.d(
                    "MapNavigationManager",
                    "Detected within destination threshold - marking navigation complete"
                )
                onNavigationComplete()
            } else {
                val distanceToDest = navigationProgressManager?.getDistanceToDestination()
                if (distanceToDest != null && distanceToDest <= 25.0) {
                    android.util.Log.d(
                        "MapNavigationManager",
                        "Distance to destination ${distanceToDest}m <= 25m - marking navigation complete (fallback)"
                    )
                    onNavigationComplete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "MapNavigationManager",
                "Error checking arrival threshold: ${e.message}",
                e
            )
        }

        android.util.Log.d(
            "MapNavigationManager",
            "Navigation location updated and sent to progress manager"
        )
    }

    /** Update step count from pedometer */
    fun updateNavigationStepCount(stepCount: Int) {
        val navState = state.value.navigationState
        if (navState !is NavigationState.Active) return

        state.update { it.copy(navigationState = navState.updateStepCount(stepCount)) }
    }

    /** Internal callback: navigation step advanced */
    private fun onNavigationStepAdvanced(step: DirectionStep, stepIndex: Int) {
        val navState = state.value.navigationState
        if (navState !is NavigationState.Active) return

        val newNavState = navState.copy(currentStepIndex = stepIndex, distanceCovered = 0.0)
        state.update { it.copy(navigationState = newNavState) }
        android.util.Log.d(
            "MapNavigationManager",
            "Advanced to step $stepIndex: ${step.instruction}"
        )
    }

    /** Internal callback: progress update */
    private fun onNavigationProgressUpdate(stepDistance: Double, totalDistance: Double) {
        val navState = state.value.navigationState
        if (navState !is NavigationState.Active) return

        val newNavState = navState.updateStepDistance(stepDistance, totalDistance)
        state.update { it.copy(navigationState = newNavState) }
    }

    /** Internal: mark navigation complete */
    private fun onNavigationComplete() {
        val navState = state.value.navigationState
        if (navState !is NavigationState.Active) return

        val completedState = navState.copy(isCompleted = true)
        state.update { it.copy(navigationState = completedState) }

        navigationProgressManager?.stopNavigation()
        android.util.Log.d(
            "MapNavigationManager",
            "Navigation completed - marked as completed in state"
        )
    }

    /** Stop active navigation and return to preview mode */
    fun stopNavigation() {
        navigationProgressManager?.stopNavigation()
        state.update {
            it.copy(
                navigationState = NavigationState.Inactive,
                bottomSheetState = it.directionData?.let { data ->
                    BottomSheetState.DirectionInitial(
                        data
                    )
                } ?: BottomSheetState.Hidden
            )
        }
        android.util.Log.d("MapNavigationManager", "Navigation stopped")
    }

    /** Finish navigation explicitly (Done): stop and restore UI */
    fun finishNavigation() {
        navigationProgressManager?.stopNavigation()
        state.update {
            it.copy(
                navigationState = NavigationState.Inactive,
                bottomSheetState = BottomSheetState.Hidden,
                isSearchOverlayActive = false,
                uiMode = UiMode.Normal
            )
        }
        android.util.Log.d(
            "MapNavigationManager",
            "Navigation finished (Done) - returned to normal mode"
        )
    }
}
