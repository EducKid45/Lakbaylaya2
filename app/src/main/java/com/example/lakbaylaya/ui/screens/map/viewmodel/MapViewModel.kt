package com.example.lakbaylaya.ui.screens.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.repository.MapRepository
import com.example.lakbaylaya.data.repository.MapRepositoryImpl
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.utils.LocationProvider
import com.example.lakbaylaya.ui.screens.map.location.MapLocationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Factory for creating MapViewModel with Application context
 */
class MapViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(application) as T
        }
        return super.create(modelClass)
    }
}

/**
 * ViewModel for Map Screen with enhanced state management
 *
 * Responsibilities:
 * - Manages map state using SearchUiState pattern
 * - Handles user interactions with clear separation of concerns
 * - Implements debounced search with 3-character minimum
 * - Fetches and transforms search results from repository
 * - Manages recent searches (max 10 items)
 * - Sorts results by distance from user location
 * - Maps categories to appropriate icons
 * - Tracks real GPS location updates
 *
 * Architecture:
 * - MVVM pattern with clean architecture principles
 * - Repository pattern for data access
 * - Unidirectional data flow
 * - StateFlow for reactive state management
 * - Sealed classes for type-safe state
 * - Separation of concerns (UI logic vs business logic)
 */
class MapViewModel(
    application: Application,
    private val repository: MapRepository = MapRepositoryImpl()
) : AndroidViewModel(application) {

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val MIN_DISTANCE_FOR_UPDATE_METERS = 50.0 // Don't update if moved less than 50m
    }

    // Mutable state
    private val _state = MutableStateFlow(MapState())

    // Public immutable state
    val state: StateFlow<MapState> = _state.asStateFlow()

    // Debouncer for search input (300ms delay)
    // Search manager handles debounced searching and result transforms
    private val searchManager = MapSearchManager(
        repository = repository,
        state = _state,
        coroutineScope = viewModelScope,
        debounceDelayMs = DEBOUNCE_DELAY_MS
    )

    // Stop/route manager handles adding/removing/swapping stops and recalculating routes
    private val stopManager = MapStopManager(
        repository = repository,
        state = _state,
        coroutineScope = viewModelScope,
        addToRecentCallback = { result -> searchManager.addToRecentSearches(result) }
    )

    // Navigation manager handles active navigation lifecycle and GPS-driven updates
    private val navigationManager = MapNavigationManager(
        _state
    )

    // Location provider and controller for GPS updates + reverse geocoding
    private val locationProvider by lazy { LocationProvider(getApplication()) }
    private val locationController = MapLocationController(
        repository = repository,
        state = _state,
        coroutineScope = viewModelScope,
        locationProvider = locationProvider,
        distanceThresholdMeters = MIN_DISTANCE_FOR_UPDATE_METERS
    )

    init {
        // Initialize with default location (Manila City Hall) until GPS updates
        _state.update {
            it.copy(
                currentLocation = Location(
                    latitude = 14.5995,
                    longitude = 120.9842
                )
            )
        }
    }

    /**
     * Updates search query and manages UI state transitions
     *
     * State transitions:
     * - Empty query -> Idle
     * - 1-2 characters -> Typing (no search triggered)
     * - 3+ characters -> Typing -> (debounced) -> Searching -> Results/Empty/Error
     */
    fun onSearchQueryChange(query: String) {
        searchManager.onSearchQueryChange(query)
    }

    /**
     * Activates the search overlay and requests focus
     */
    fun onSearchBarClick() {
        _state.update { it.copy(isSearchOverlayActive = true) }
    }

    /**
     * Deactivates search overlay (back button pressed)
     */
    fun onBackClick() {
        searchManager.cancelPending()
        _state.update {
            it.copy(
                isSearchOverlayActive = false,
                searchUiState = SearchUiState.Idle
            )
        }
    }

    /**
     * Clears the search query and resets to idle state
     * Also removes marker if present
     */
    fun onClearSearch() {
        searchManager.cancelPending()
        _state.update {
            it.copy(
                searchQuery = "",
                searchUiState = SearchUiState.Idle,
                selectedMarker = null, // Remove marker when clearing search
                selectedResult = null,
                bottomSheetState = BottomSheetState.Hidden
            )
        }
    }

    /**
     * Handles selection of a search result
     * Shows bottom sheet with place details and closes search overlay
     * Auto-fills search bar with selected place name
     */
    fun onResultSelected(result: SearchResult) {
        // Add to recent searches (max 10 items)
        val updatedRecent = searchManager.addToRecentSearches(result)

        _state.update {
            it.copy(
                selectedResult = result,
                selectedMarker = result,
                recentSearches = updatedRecent,
                isSearchOverlayActive = false,
                searchQuery = result.address, // Auto-fill search bar with address
                searchUiState = SearchUiState.Idle,
                bottomSheetState = BottomSheetState.Initial(result)
            )
        }
    }

    /**
     * Updates bottom sheet state
     */
    fun onBottomSheetStateChange(newState: BottomSheetState) {
        _state.update { it.copy(bottomSheetState = newState) }
    }

    /**
     * Toggles user location bottom sheet expansion state
     */
    fun toggleUserLocationSheet() {
        _state.update { it.copy(isUserLocationSheetExpanded = !it.isUserLocationSheetExpanded) }
    }

    /**
     * Dismisses current place selection
     * Hides bottom sheet, removes marker, and clears search query
     */
    fun dismissPlace() {
        _state.update {
            it.copy(
                selectedResult = null,
                selectedMarker = null,
                searchQuery = "",
                bottomSheetState = BottomSheetState.Hidden
            )
        }
    }

    /**
     * Handles place action button clicks
     */
    fun onPlaceAction(action: PlaceAction, place: SearchResult) {
        when (action) {
            PlaceAction.START_NAVIGATION -> navigateToLocation(place)
            PlaceAction.SAVE_PLACE -> savePlaceToFavorites(place)
            PlaceAction.MARK_LOCATION -> markLocation(place)
            PlaceAction.SHARE -> sharePlace(place)
            PlaceAction.CALL -> callPlace(place)
            PlaceAction.DIRECTIONS -> showDirections(place)
        }
    }

    /**
     * Updates location permission state
     */
    fun onLocationPermissionChanged(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }

        // If permission granted, start location updates
        if (granted) {
            startLocationUpdates()
        }
    }

    /**
     * Updates current user location
     */
    fun updateCurrentLocation(location: Location) {
        _state.update { it.copy(currentLocation = location) }
    }

    /**
     * Navigates to selected location on map
     * Moves camera and adds marker
     */
    fun navigateToLocation(result: SearchResult) {
        onResultSelected(result)
        // Camera movement will be handled by MapScreen when selectedResult changes
    }

    // performSearch is delegated to MapSearchManager

    // Private action handlers

    private fun savePlaceToFavorites(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement saving to local database or shared preferences
    }

    private fun openVoiceNotesForPlace(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // Navigate to Voice Notes screen with place location
        // This will be handled by the UI layer (MapScreen) by exposing a callback
        _state.update {
            it.copy(
                // Add a flag or state to trigger navigation to Voice Notes screen
                pendingVoiceNotesPlace = place
            )
        }
    }

    private fun startExplorationMode(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement exploration mode
    }

    private fun markLocation(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement location marking
    }

    private fun addDifficulty(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement adding difficulty metadata for a place (e.g., accessibility difficulty)
    }

    private fun sharePlace(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement sharing via Android ShareSheet
    }

    private fun callPlace(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement phone call
    }

    private fun openWebsite(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement opening browser
    }

    private fun showDirections(place: SearchResult) {
        // Create direction data from current location to selected place
        val currentLoc = _state.value.currentLocation ?: return

        val origin = RoutePoint(
            latitude = currentLoc.latitude,
            longitude = currentLoc.longitude,
            name = "Your Location",
            address = _state.value.reverseGeocodedLocation?.getDisplayLabel() ?: "Current Location"
        )

        val destination = RoutePoint(
            latitude = place.latitude,
            longitude = place.longitude,
            name = place.placeName,
            address = place.address
        )

        // Prepare directionData locally so it's available in both success and failure branches
        val initialDirectionData = DirectionData(
            origin = origin,
            destination = destination,
            stops = emptyList(),
            routes = emptyList(),
            selectedRouteIndex = 0,
            sourcePlace = place
        )

        // Show loading state
        _state.update {
            it.copy(
                uiMode = UiMode.Direction(isEditingStops = false),
                directionData = initialDirectionData,
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(initialDirectionData),
                isSearchOverlayActive = false // Close search if open
            )
        }

        // Fetch real routes from API
        viewModelScope.launch {
            val result = repository.calculateRoute(origin, destination, mode = "walk")
            result.onSuccess { routes ->
                val directionData = initialDirectionData.copy(routes = routes, selectedRouteIndex = 0)

                // Generate polylines from routes
                val polylines = routes.map { route ->
                    PolylineData(
                        routeId = route.id,
                        coordinates = route.polylineCoordinates,
                        isPrimary = route.isPrimary
                    )
                }

                _state.update {
                    it.copy(
                        directionData = directionData,
                        polylines = polylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(directionData)
                    )
                }
            }.onFailure { error ->
                // Handle error - keep sheet visible in loading/error state without mock
                android.util.Log.e("MapViewModel", "Failed to calculate route: ${error.message}", error)

                _state.update {
                    it.copy(
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Getting route failed. Please try again."
                        ),
                        bottomSheetState = BottomSheetState.DirectionInitial(initialDirectionData)
                    )
                }
            }
        }
    }

    /**
     * Handle back from Direction Mode
     * Restores Normal mode and Place sheet if there was a source place
     */
    fun onDirectionBack() {
        val sourcePlace = _state.value.directionData?.sourcePlace

        _state.update {
            it.copy(
                uiMode = UiMode.Normal,
                directionData = null,
                polylines = emptyList(),
                bottomSheetState = if (sourcePlace != null) {
                    BottomSheetState.Half(sourcePlace)
                } else {
                    BottomSheetState.Hidden
                }
            )
        }
    }

    /**
     * Handle route selection change (when user taps a polyline on the map)
     */
    fun onRouteChange(routeIndex: Int) {
        val currentData = _state.value.directionData ?: return
        val newData = currentData.withSelectedRoute(routeIndex)

        // Update polylines to reflect selection (highlight the selected route)
        val updatedPolylines = currentData.routes.mapIndexed { index, route ->
            PolylineData(
                routeId = route.id,
                coordinates = route.polylineCoordinates,
                isPrimary = index == routeIndex
            )
        }

        _state.update {
            it.copy(
                directionData = newData,
                polylines = updatedPolylines,
                bottomSheetState = when (val currentSheet = it.bottomSheetState) {
                    is BottomSheetState.DirectionInitial ->
                        BottomSheetState.DirectionInitial(newData)
                    is BottomSheetState.DirectionFullExpand ->
                        BottomSheetState.DirectionFullExpand(newData)
                    else -> currentSheet
                }
            )
        }
    }

    /**
     * Start editing stops (from Overlay Panel "Add stop" button or Direction sheet "Add stops" button)
     * Clears search text, opens search overlay for adding stops
     */
    fun onStartEditingStops() {
        _state.update {
            it.copy(
                uiMode = UiMode.Direction(isEditingStops = true),
                isSearchOverlayActive = true,
                searchQuery = "", // Clear search bar text
                searchUiState = SearchUiState.Idle, // Reset search state
                bottomSheetState = BottomSheetState.Hidden // Hide when search opens
            )
        }
    }

    /**
     * Finish editing stops (from Overlay Panel "Done" button)
     * Shows Direction sheet again
     */
    fun onFinishEditingStops() {
        val directionData = _state.value.directionData

        _state.update {
            it.copy(
                uiMode = UiMode.Direction(isEditingStops = false),
                isSearchOverlayActive = false,
                bottomSheetState = if (directionData != null) {
                    BottomSheetState.DirectionInitial(directionData)
                } else {
                    BottomSheetState.Hidden
                }
            )
        }
    }

    /**
     * Add a stop from search result
     */
    fun onAddStopFromSearch(result: SearchResult) {
        stopManager.onAddStopFromSearch(result)
    }

    /**
     * Remove a stop at index
     */
    fun onRemoveStop(index: Int) {
        stopManager.onRemoveStop(index)
    }

    /**
     * Swap origin and destination
     */
    fun onSwapOriginDestination() {
        stopManager.onSwapOriginDestination()
    }

    /**
     * Swap a stop with the destination (making that stop the new destination)
     */
    fun onSwapStopWithDestination(stopIndex: Int) {
        stopManager.onSwapStopWithDestination(stopIndex)
    }

    /**
     * Move a stop up in the stops list (swap with previous stop)
     */
    fun onMoveStopUp(index: Int) {
        stopManager.onMoveStopUp(index)
    }

    /**
     * Move a stop down in the stops list (swap with next stop)
     */
    fun onMoveStopDown(index: Int) {
        stopManager.onMoveStopDown(index)
    }

    /**
     * Move destination up to become the last stop (swap destination with last stop)
     */
    fun onMoveDestinationUp() {
        stopManager.onMoveDestinationUp()
    }

    /**
     * Handle overlay panel expansion state change
     * When expanded, hides the bottom sheet
     */
    fun onOverlayPanelExpandedChange(isExpanded: Boolean) {
        _state.update { it.copy(isOverlayPanelExpanded = isExpanded) }
    }

    fun startLocationUpdates() {
        locationController.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        locationController.stopLocationUpdates()
    }

    /**
     * Called when the ViewModel is cleared
     * Stops location updates and navigation
     */
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        navigationManager.stopNavigation()
    }

    /**
     * Swap two stops by indices (used by OverlayStopPanel)
     */
    fun onSwapStops(indexA: Int, indexB: Int) {
        val currentData = _state.value.directionData ?: return
        if (indexA !in currentData.stops.indices) return
        if (indexB !in currentData.stops.indices) return
        if (indexA == indexB) return

        val newStops = currentData.stops.toMutableList()
        val tmp = newStops[indexA]
        newStops[indexA] = newStops[indexB]
        newStops[indexB] = tmp

        val updatedData = currentData.copy(stops = newStops)

        // Show loading state
        _state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        viewModelScope.launch {
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
                _state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after swapping stops: ${error.message}", error)
                _state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(updatedData),
                        searchUiState = SearchUiState.Error(query = "", message = "Getting route failed.")
                    )
                }
            }
        }
    }

    // ============================================
    // Navigation Methods

    fun startNavigation() {
        navigationManager.startNavigation()
    }

    /**
     * Called from MapScreen when GPS location updates arrive during navigation
     */
    fun updateNavigationLocation(location: android.location.Location) {
        navigationManager.updateNavigationLocation(location)
    }

    /** Update step count from pedometer (UI stats) */
    fun updateNavigationStepCount(stepCount: Int) {
        navigationManager.updateNavigationStepCount(stepCount)
    }

    /** Stop active navigation and return to direction preview mode */
    fun stopNavigation() {
        navigationManager.stopNavigation()
    }

    /** Finish navigation explicitly (Done action) */
    fun finishNavigation() {
        navigationManager.finishNavigation()
    }

    /**
     * Mute/unmute navigation voice guidance
     */
    fun toggleNavigationMute() {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        _state.update {
            it.copy(navigationState = navState.toggleMute())
        }
    }

    /**
     * Clears pending voice notes place after navigation is handled.
     */
    fun clearPendingVoiceNotesPlace() {
        _state.update {
            it.copy(pendingVoiceNotesPlace = null)
        }
    }
}
