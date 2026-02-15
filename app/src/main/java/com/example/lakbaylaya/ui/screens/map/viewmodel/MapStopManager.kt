package com.example.lakbaylaya.ui.screens.map.viewmodel

import com.example.lakbaylaya.data.repository.MapRepository
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.utils.DistanceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Responsible for stop and route management extracted from MapViewModel:
 * - onAddStopFromSearch
 * - onRemoveStop
 * - onSwapStops
 * - onMoveStopUp / onMoveStopDown
 * - onSwapOriginDestination / onSwapStopWithDestination / onMoveDestinationUp
 *
 * This centralizes direction/stop related behavior so the ViewModel stays thin.
 */
class MapStopManager(
    private val repository: MapRepository,
    private val state: MutableStateFlow<MapState>,
    private val coroutineScope: CoroutineScope,
    private val addToRecentCallback: (SearchResult) -> List<SearchResult>
) {

    /**
     * Adds a stop from a search result into the current directionData, validates distances,
     * updates recent searches and requests route calculation from repository.
     */
    fun onAddStopFromSearch(result: SearchResult) {
        val currentData = state.value.directionData ?: return

        // Check duplicate coordinate (epsilon tolerance)
        val epsilon = 0.0001
        val isDuplicate = listOf(currentData.origin, currentData.destination)
            .plus(currentData.stops)
            .any { point ->
                kotlin.math.abs(point.latitude - result.latitude) < epsilon &&
                        kotlin.math.abs(point.longitude - result.longitude) < epsilon
            }

        if (isDuplicate) {
            state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "This location is already in your route"
                    )
                )
            }
            return
        }

        // Validate distances
        val lastPoint = currentData.stops.lastOrNull() ?: currentData.origin
        val distanceFromLast = DistanceUtils.calculateDistance(
            lastPoint.latitude,
            lastPoint.longitude,
            result.latitude,
            result.longitude
        )
        val distanceToDestination = DistanceUtils.calculateDistance(
            result.latitude,
            result.longitude,
            currentData.destination.latitude,
            currentData.destination.longitude
        )

        if (distanceFromLast > 50000) {
            state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "Stop too far from previous location (${
                            DistanceUtils.formatDistance(
                                distanceFromLast
                            )
                        }). Maximum walking distance is 50 km per segment."
                    )
                )
            }
            return
        }

        if (distanceToDestination > 50000) {
            state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "Stop too far from destination (${
                            DistanceUtils.formatDistance(
                                distanceToDestination
                            )
                        }). Maximum walking distance is 50 km per segment."
                    )
                )
            }
            return
        }

        val newStop = RoutePoint(
            latitude = result.latitude,
            longitude = result.longitude,
            name = result.placeName,
            address = result.address
        )

        val updatedData = currentData.addStop(newStop)

        // Add to recent searches via callback provided by MapViewModel
        val updatedRecent = addToRecentCallback(result)

        // Close search and show loading state
        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                isSearchOverlayActive = false,
                searchQuery = "",
                recentSearches = updatedRecent,
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        // Fetch new routes with the added stop
        coroutineScope.launch {
            val apiResult = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )

            apiResult.onSuccess { newRoutes ->
                val finalData = updatedData.copy(routes = newRoutes, selectedRouteIndex = 0)
                val newPolylines = newRoutes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }

                state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route with stops: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed. Please try again."
                        ),
                        bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
                    )
                }
            }
        }
    }

    /** Remove stop at index and re-calc routes */
    fun onRemoveStop(index: Int) {
        val currentData = state.value.directionData ?: return
        val updatedData = currentData.removeStop(index)

        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        coroutineScope.launch {
            val result = if (updatedData.stops.isEmpty()) {
                repository.calculateRoute(
                    updatedData.origin,
                    updatedData.destination,
                    mode = "walk"
                )
            } else {
                repository.calculateRouteWithStops(
                    updatedData.origin,
                    updatedData.destination,
                    updatedData.stops,
                    mode = "walk"
                )
            }

            result.onSuccess { newRoutes ->
                val finalData = updatedData.copy(routes = newRoutes, selectedRouteIndex = 0)
                val newPolylines = newRoutes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update { it.copy(directionData = finalData, polylines = newPolylines) }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after removing stop: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(
                            updatedData
                        )
                    )
                }
            }
        }
    }

    /** Swap origin and destination and recalc routes */
    fun onSwapOriginDestination() {
        val currentData = state.value.directionData ?: return
        val swapped = currentData.swapOriginDestination()

        state.update {
            it.copy(
                directionData = swapped.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = when (it.bottomSheetState) {
                    is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(
                        swapped
                    )

                    is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(
                        swapped
                    )

                    else -> BottomSheetState.DirectionInitial(swapped)
                }
            )
        }

        coroutineScope.launch {
            val result = if (swapped.stops.isEmpty()) {
                repository.calculateRoute(swapped.origin, swapped.destination, mode = "walk")
            } else {
                repository.calculateRouteWithStops(
                    swapped.origin,
                    swapped.destination,
                    swapped.stops,
                    mode = "walk"
                )
            }

            result.onSuccess { newRoutes ->
                val updatedData = swapped.copy(routes = newRoutes, selectedRouteIndex = 0)
                val newPolylines = newRoutes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update {
                    it.copy(
                        directionData = updatedData,
                        polylines = newPolylines,
                        bottomSheetState = when (val currentSheet = it.bottomSheetState) {
                            is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(
                                updatedData
                            )

                            is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(
                                updatedData
                            )

                            else -> currentSheet
                        }
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after swap: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = when (it.bottomSheetState) {
                            is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(
                                swapped
                            )

                            is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(
                                swapped
                            )

                            else -> BottomSheetState.DirectionInitial(swapped)
                        }
                    )
                }
            }
        }
    }

    /** Swap a stop with the destination */
    fun onSwapStopWithDestination(stopIndex: Int) {
        val currentData = state.value.directionData ?: return
        if (stopIndex !in currentData.stops.indices) return

        val newStops = currentData.stops.toMutableList()
        val selectedStop = newStops.removeAt(stopIndex)
        val oldDestination = currentData.destination
        newStops.add(stopIndex, oldDestination)

        val updatedData = currentData.copy(
            origin = currentData.origin,
            destination = selectedStop,
            stops = newStops
        )

        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        coroutineScope.launch {
            val result = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after swap stop/destination: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(
                            updatedData
                        ),
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed."
                        )
                    )
                }
            }
        }
    }

    /** Move a stop up (swap with previous) */
    fun onMoveStopUp(index: Int) {
        val currentData = state.value.directionData ?: return
        if (index <= 0 || index >= currentData.stops.size) return

        val newStops = currentData.stops.toMutableList()
        val tmp = newStops[index - 1]
        newStops[index - 1] = newStops[index]
        newStops[index] = tmp

        val updatedData = currentData.copy(stops = newStops)

        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        coroutineScope.launch {
            val result = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after moving stop up: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(
                            updatedData
                        ),
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed."
                        )
                    )
                }
            }
        }
    }

    /** Move a stop down (swap with next) */
    fun onMoveStopDown(index: Int) {
        val currentData = state.value.directionData ?: return
        if (index < 0 || index >= currentData.stops.size - 1) return

        val newStops = currentData.stops.toMutableList()
        val tmp = newStops[index + 1]
        newStops[index + 1] = newStops[index]
        newStops[index] = tmp

        val updatedData = currentData.copy(stops = newStops)

        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        coroutineScope.launch {
            val result = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after moving stop down: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(
                            updatedData
                        ),
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed."
                        )
                    )
                }
            }
        }
    }

    /** Move destination up to become last stop (swap destination with last stop) */
    fun onMoveDestinationUp() {
        val currentData = state.value.directionData ?: return
        val stops = currentData.stops.toMutableList()
        if (stops.isEmpty()) return

        val oldDestination = currentData.destination
        val lastStop = stops.removeAt(stops.lastIndex)

        stops.add(oldDestination)

        val updatedData = currentData.copy(destination = lastStop, stops = stops)

        state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        coroutineScope.launch {
            val result = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }
                state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e(
                    "MapStopManager",
                    "Failed to calculate route after moving destination up: ${error.message}",
                    error
                )
                state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(
                            updatedData
                        ),
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed."
                        )
                    )
                }
            }
        }
    }
}

