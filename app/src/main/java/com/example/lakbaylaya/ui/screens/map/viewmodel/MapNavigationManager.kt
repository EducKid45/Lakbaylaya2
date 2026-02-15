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
            "Starting navigation with ${selectedRoute.steps.size} steps to destination: " +
                    "(${dirData.destination.latitude}, ${dirData.destination.longitude}) '${dirData.destination.name}'"
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
                    setStepThreshold(10.0) // 10m threshold for regular steps
                    setGpsAccuracyThreshold(20.0) // 20m accuracy threshold
                    setFinalDestinationThreshold(25.0) // 25m threshold for final arrival

                    startNavigation(
                        route = selectedRoute,
                        destinationLatitude = dirData.destination.latitude,
                        destinationLongitude = dirData.destination.longitude,
                        onStepAdvanced = { step, stepIndex ->
                            onNavigationStepAdvanced(step, stepIndex)
                        },
                        onProgressUpdate = { stepDistance, totalDistance ->
                            onNavigationProgressUpdate(stepDistance, totalDistance)
                        },
                        onNavigationComplete = {
                            android.util.Log.i(
                                "MapNavigationManager",
                                "🎯 Navigation complete callback triggered!"
                            )
                            onNavigationComplete()
                        },
                        onDestinationArrived = { arrivalInfo ->
                            android.util.Log.i(
                                "MapNavigationManager",
                                "🎯 Destination arrival callback triggered!"
                            )
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
            isLocationTracking = false,
            isCompleted = false // Start with not completed
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

        // Check if already completed to avoid redundant processing
        if (navState.isCompleted) {
            android.util.Log.d(
                "MapNavigationManager",
                "Navigation already completed, ignoring location update"
            )
            return
        }

        android.util.Log.d(
            "MapNavigationManager",
            "GPS Update: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m, " +
                    "current step: ${navState.currentStepIndex}/${navState.getTotalSteps() - 1}"
        )

        // Update navigation state with new location
        state.update { it.copy(navigationState = navState.updateLocation(location)) }

        // Update progress manager (this will trigger arrival detection automatically)
        navigationProgressManager?.let { progressManager ->
            progressManager.updateLocation(location)

            // Log debug info periodically (every few updates)
            if (System.currentTimeMillis() % 5000 < 1000) { // Roughly every 5 seconds
                android.util.Log.d("MapNavigationManager", progressManager.getDebugInfo())
            }
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

        // Mark navigation as completed so the UI shows arrival message and Done button
        val completedState = navState.copy(isCompleted = true)
        state.update { it.copy(navigationState = completedState) }

        android.util.Log.i(
            "MapNavigationManager",
            "🎯 Navigation marked as completed - UI should now show 'You have arrived!' and Done button"
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

    /** Test method to manually trigger arrival for debugging */
    fun testArrival() {
        android.util.Log.i("MapNavigationManager", "🧪 TEST: Force triggering arrival detection")
        navigationProgressManager?.forceArrival()
    }
}
