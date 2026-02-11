package com.example.lakbaylaya.ui.screens.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.ui.screens.map.data.repository.MapRepository
import com.example.lakbaylaya.ui.screens.map.data.repository.MapRepositoryImpl
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.ui.screens.map.utils.CategoryIconMapper
import com.example.lakbaylaya.ui.screens.map.utils.Debouncer
import com.example.lakbaylaya.ui.screens.map.utils.DistanceUtils
import com.example.lakbaylaya.ui.screens.map.utils.LocationProvider
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
        private const val MIN_SEARCH_QUERY_LENGTH = 3
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val MAX_RECENT_SEARCHES = 10
        private const val MAX_SEARCH_RESULTS = 10
        private const val MIN_DISTANCE_FOR_UPDATE_METERS = 50.0 // Don't update if moved less than 50m
    }

    // Mutable state
    private val _state = MutableStateFlow(MapState())

    // Public immutable state
    val state: StateFlow<MapState> = _state.asStateFlow()

    // Debouncer for search input (300ms delay)
    private val searchDebouncer = Debouncer(
        delayMillis = DEBOUNCE_DELAY_MS,
        coroutineScope = viewModelScope
    )

    // Location provider for GPS updates
    private val locationProvider by lazy { LocationProvider(getApplication()) }

    // Track last reverse geocoded location to prevent unnecessary API calls
    private var lastReverseGeocodedLatLng: Pair<Double, Double>? = null

    // Navigation management
    private var navigationProgressManager: com.example.lakbaylaya.ui.screens.map.navigation.progress.NavigationProgressManager? = null

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
        _state.update { it.copy(searchQuery = query) }

        // Cancel any pending search
        searchDebouncer.cancel()

        when {
            query.isEmpty() -> {
                // Return to idle state
                _state.update { it.copy(searchUiState = SearchUiState.Idle) }
            }

            query.length < MIN_SEARCH_QUERY_LENGTH -> {
                // Typing but not enough characters to search
                _state.update { it.copy(searchUiState = SearchUiState.Typing(query)) }
            }

            else -> {
                // Enough characters - show typing state and debounce search
                _state.update { it.copy(searchUiState = SearchUiState.Typing(query)) }

                searchDebouncer.debounce {
                    performSearch(query)
                }
            }
        }
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
        searchDebouncer.cancel()
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
        searchDebouncer.cancel()
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
        val updatedRecent = addToRecentSearches(result)

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
            PlaceAction.VOICE_NOTES -> openVoiceNotesForPlace(place)
            PlaceAction.EXPLORATION_MODE -> startExplorationMode(place)
            PlaceAction.MARK_LOCATION -> markLocation(place)
            PlaceAction.SHARE -> sharePlace(place)
            PlaceAction.CALL -> callPlace(place)
            PlaceAction.WEBSITE -> openWebsite(place)
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

    /**
     * Performs search using repository and updates UI state
     *
     * State transitions:
     * Searching -> Results (if results found)
     * Searching -> Empty (if no results)
     * Searching -> Error (if API fails)
     */
    private fun performSearch(query: String) {
        if (query.isBlank() || query.length < MIN_SEARCH_QUERY_LENGTH) {
            return
        }

        // Transition to searching state
        _state.update { it.copy(searchUiState = SearchUiState.Searching(query)) }

        viewModelScope.launch {
            val currentLocation = _state.value.currentLocation
            val apiResult = repository.searchPlaces(
                query = query,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                limit = MAX_SEARCH_RESULTS
            )

            apiResult.onSuccess { rawResults ->
                // Transform results: calculate distance, sort, map icons
                val transformedResults = transformSearchResults(rawResults, currentLocation)

                // Update state based on results
                val newState = if (transformedResults.isEmpty()) {
                    SearchUiState.Empty(query)
                } else {
                    SearchUiState.Results(query, transformedResults)
                }

                _state.update { it.copy(searchUiState = newState) }
            }.onFailure { error ->
                // Transition to error state
                _state.update {
                    it.copy(
                        searchUiState = SearchUiState.Error(
                            query = query,
                            message = error.message ?: "Failed to search places"
                        )
                    )
                }
            }
        }
    }

    /**
     * Transforms raw search results:
     * - Calculates accurate distance from user location
     * - Sorts by distance (nearest first)
     * - Maps categories to icon types
     * - Limits to MAX_SEARCH_RESULTS
     */
    private fun transformSearchResults(
        results: List<SearchResult>,
        userLocation: Location?
    ): List<SearchResult> {
        if (userLocation == null) return results.take(MAX_SEARCH_RESULTS)

        return results
            .map { result ->
                // Recalculate distance using accurate Haversine formula
                val accurateDistance = DistanceUtils.calculateDistance(
                    lat1 = userLocation.latitude,
                    lon1 = userLocation.longitude,
                    lat2 = result.latitude,
                    lon2 = result.longitude
                )

                // Map category to icon type
                val iconType = CategoryIconMapper.getCategoryIcon(
                    category = result.category,
                    isRecent = result.isRecent
                )

                result.copy(
                    distanceMeters = accurateDistance,
                    iconType = iconType
                )
            }
            .sortedBy { it.distanceMeters } // Sort by nearest distance
            .take(MAX_SEARCH_RESULTS)
    }

    /**
     * Adds a result to recent searches list
     * Maintains max of MAX_RECENT_SEARCHES items
     * Avoids duplicates
     */
    private fun addToRecentSearches(result: SearchResult): List<SearchResult> {
        val currentRecent = _state.value.recentSearches

        // Remove if already exists (to move to top)
        val filtered = currentRecent.filterNot { it.id == result.id }

        // Add to top with recent flag and icon
        val recentResult = result.copy(
            isRecent = true,
            iconType = PlaceIconType.RECENT
        )

        return listOf(recentResult) + filtered.take(MAX_RECENT_SEARCHES - 1)
    }

    // Private action handlers

    private fun savePlaceToFavorites(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement saving to local database or shared preferences
    }

    private fun openVoiceNotesForPlace(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement voice notes feature
    }

    private fun startExplorationMode(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement exploration mode
    }

    private fun markLocation(@Suppress("UNUSED_PARAMETER") place: SearchResult) {
        // TODO: Implement location marking
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
        val currentData = _state.value.directionData ?: return

        // Check if this coordinate already exists in origin, destination, or stops
        val epsilon = 0.0001 // ~11 meters tolerance
        val isDuplicate = listOf(currentData.origin, currentData.destination)
            .plus(currentData.stops)
            .any { point ->
                kotlin.math.abs(point.latitude - result.latitude) < epsilon &&
                kotlin.math.abs(point.longitude - result.longitude) < epsilon
            }

        if (isDuplicate) {
            // Show error - location already added
            _state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "This location is already in your route"
                    )
                )
            }
            return
        }

        // Validate distance from current location (last stop or origin)
        val lastPoint = currentData.stops.lastOrNull() ?: currentData.origin
        val distanceFromLast = DistanceUtils.calculateDistance(
            lastPoint.latitude,
            lastPoint.longitude,
            result.latitude,
            result.longitude
        )

        // Also validate distance to destination
        val distanceToDestination = DistanceUtils.calculateDistance(
            result.latitude,
            result.longitude,
            currentData.destination.latitude,
            currentData.destination.longitude
        )

        // Check if any segment would exceed 50km limit
        if (distanceFromLast > 50000) {
            _state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "Stop too far from previous location (${DistanceUtils.formatDistance(distanceFromLast)}). Maximum walking distance is 50 km per segment."
                    )
                )
            }
            return
        }

        if (distanceToDestination > 50000) {
            _state.update {
                it.copy(
                    searchUiState = SearchUiState.Error(
                        query = result.placeName,
                        message = "Stop too far from destination (${DistanceUtils.formatDistance(distanceToDestination)}). Maximum walking distance is 50 km per segment."
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

        // Add the selected result to recent searches so it appears in history
        val updatedRecent = addToRecentSearches(result)

        // Close search and show loading state
        _state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()), // Clear routes while loading
                polylines = emptyList(),
                isSearchOverlayActive = false,
                searchQuery = "",
                recentSearches = updatedRecent,
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        // Fetch new routes with the added stop
        viewModelScope.launch {
            val result = repository.calculateRouteWithStops(
                updatedData.origin,
                updatedData.destination,
                updatedData.stops,
                mode = "walk"
            )

            result.onSuccess { newRoutes ->
                val finalData = updatedData.copy(routes = newRoutes, selectedRouteIndex = 0)

                val newPolylines = newRoutes.map { route ->
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
                android.util.Log.e("MapViewModel", "Failed to calculate route with stops: ${error.message}", error)
                // Keep sheet visible in loading/error state without mock
                _state.update {
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

    /**
     * Remove a stop at index
     */
    fun onRemoveStop(index: Int) {
        val currentData = _state.value.directionData ?: return
        val updatedData = currentData.removeStop(index)

        // Show loading state immediately and keep sheet visible
        _state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        // Regenerate routes without the stop
        viewModelScope.launch {
            val result = if (updatedData.stops.isEmpty()) {
                repository.calculateRoute(updatedData.origin, updatedData.destination, mode = "walk")
            } else {
                repository.calculateRouteWithStops(updatedData.origin, updatedData.destination, updatedData.stops, mode = "walk")
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

                _state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after removing stop: ${error.message}", error)
                // Keep sheet visible, no mock
                _state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
                    )
                }
            }
        }
    }

    /**
     * Swap origin and destination
     */
    fun onSwapOriginDestination() {
        val currentData = _state.value.directionData ?: return
        val swapped = currentData.swapOriginDestination()

        // Show loading state and keep sheet visible
        _state.update {
            it.copy(
                directionData = swapped.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = when (it.bottomSheetState) {
                    is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(swapped)
                    is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(swapped)
                    else -> BottomSheetState.DirectionInitial(swapped)
                }
            )
        }

        // Regenerate routes with swapped origin/destination
        viewModelScope.launch {
            val result = if (swapped.stops.isEmpty()) {
                repository.calculateRoute(swapped.origin, swapped.destination, mode = "walk")
            } else {
                repository.calculateRouteWithStops(swapped.origin, swapped.destination, swapped.stops, mode = "walk")
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

                _state.update {
                    it.copy(
                        directionData = updatedData,
                        polylines = newPolylines,
                        bottomSheetState = when (val currentSheet = it.bottomSheetState) {
                            is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(updatedData)
                            is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(updatedData)
                            else -> currentSheet
                        }
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after swap: ${error.message}", error)
                // Keep sheet visible without mock
                _state.update {
                    it.copy(
                        bottomSheetState = when (it.bottomSheetState) {
                            is BottomSheetState.DirectionInitial -> BottomSheetState.DirectionInitial(swapped)
                            is BottomSheetState.DirectionFullExpand -> BottomSheetState.DirectionFullExpand(swapped)
                            else -> BottomSheetState.DirectionInitial(swapped)
                        }
                    )
                }
            }
        }
    }

    /**
     * Swap a stop with the destination (making that stop the new destination)
     */
    fun onSwapStopWithDestination(stopIndex: Int) {
        val currentData = _state.value.directionData ?: return
        if (stopIndex !in currentData.stops.indices) return

        val newStops = currentData.stops.toMutableList()
        val selectedStop = newStops.removeAt(stopIndex)

        // Swap: selectedStop becomes the new destination, old destination becomes a stop at the same index
        val oldDestination = currentData.destination
        newStops.add(stopIndex, oldDestination)

        val updatedData = currentData.copy(
            origin = currentData.origin,
            destination = selectedStop,
            stops = newStops
        )

        // Show loading state and keep sheet visible
        _state.update {
            it.copy(
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        // Regenerate real routes
        viewModelScope.launch {
            val result = repository.calculateRouteWithStops(updatedData.origin, updatedData.destination, updatedData.stops, mode = "walk")
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route -> PolylineData(routeId = route.id, coordinates = route.polylineCoordinates, isPrimary = route.isPrimary) }

                _state.update {
                    it.copy(
                        directionData = finalData,
                        polylines = newPolylines,
                        bottomSheetState = BottomSheetState.DirectionInitial(finalData)
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after swap stop/destination: ${error.message}", error)
                _state.update { it.copy(bottomSheetState = BottomSheetState.DirectionInitial(updatedData), searchUiState = SearchUiState.Error(query = "", message = "Getting route failed.")) }
            }
        }
    }

    /**
     * Move a stop up in the stops list (swap with previous stop)
     */
    fun onMoveStopUp(index: Int) {
        val currentData = _state.value.directionData ?: return
        if (index <= 0 || index >= currentData.stops.size) return

        val newStops = currentData.stops.toMutableList()
        val tmp = newStops[index - 1]
        newStops[index - 1] = newStops[index]
        newStops[index] = tmp

        val updatedData = currentData.copy(stops = newStops)

        // Show loading state
        _state.update { it.copy(directionData = updatedData.copy(routes = emptyList()), polylines = emptyList(), bottomSheetState = BottomSheetState.DirectionInitial(updatedData)) }

        viewModelScope.launch {
            val result = repository.calculateRouteWithStops(updatedData.origin, updatedData.destination, updatedData.stops, mode = "walk")
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route -> PolylineData(routeId = route.id, coordinates = route.polylineCoordinates, isPrimary = route.isPrimary) }
                _state.update { it.copy(directionData = finalData, polylines = newPolylines, bottomSheetState = BottomSheetState.DirectionInitial(finalData)) }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after moving stop up: ${error.message}", error)
                _state.update { it.copy(bottomSheetState = BottomSheetState.DirectionInitial(updatedData), searchUiState = SearchUiState.Error(query = "", message = "Getting route failed.")) }
            }
        }
    }

    /**
     * Move a stop down in the stops list (swap with next stop)
     */
    fun onMoveStopDown(index: Int) {
        val currentData = _state.value.directionData ?: return
        if (index < 0 || index >= currentData.stops.size - 1) return

        val newStops = currentData.stops.toMutableList()
        val tmp = newStops[index + 1]
        newStops[index + 1] = newStops[index]
        newStops[index] = tmp

        val updatedData = currentData.copy(stops = newStops)

        // Show loading state
        _state.update { it.copy(directionData = updatedData.copy(routes = emptyList()), polylines = emptyList(), bottomSheetState = BottomSheetState.DirectionInitial(updatedData)) }

        viewModelScope.launch {
            val result = repository.calculateRouteWithStops(updatedData.origin, updatedData.destination, updatedData.stops, mode = "walk")
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route -> PolylineData(routeId = route.id, coordinates = route.polylineCoordinates, isPrimary = route.isPrimary) }
                _state.update { it.copy(directionData = finalData, polylines = newPolylines, bottomSheetState = BottomSheetState.DirectionInitial(finalData)) }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after moving stop down: ${error.message}", error)
                _state.update { it.copy(bottomSheetState = BottomSheetState.DirectionInitial(updatedData), searchUiState = SearchUiState.Error(query = "", message = "Getting route failed.")) }
            }
        }
    }

    /**
     * Move destination up to become the last stop (swap destination with last stop)
     */
    fun onMoveDestinationUp() {
        val currentData = _state.value.directionData ?: return
        val stops = currentData.stops.toMutableList()
        if (stops.isEmpty()) return

        val oldDestination = currentData.destination
        val lastStop = stops.removeAt(stops.lastIndex)

        // New destination becomes lastStop, old destination appended as a stop at end
        stops.add(oldDestination)

        val updatedData = currentData.copy(destination = lastStop, stops = stops)

        // Show loading state
        _state.update { it.copy(directionData = updatedData.copy(routes = emptyList()), polylines = emptyList(), bottomSheetState = BottomSheetState.DirectionInitial(updatedData)) }

        viewModelScope.launch {
            val result = repository.calculateRouteWithStops(updatedData.origin, updatedData.destination, updatedData.stops, mode = "walk")
            result.onSuccess { routes ->
                val finalData = updatedData.copy(routes = routes, selectedRouteIndex = 0)
                val newPolylines = routes.map { route -> PolylineData(routeId = route.id, coordinates = route.polylineCoordinates, isPrimary = route.isPrimary) }
                _state.update { it.copy(directionData = finalData, polylines = newPolylines, bottomSheetState = BottomSheetState.DirectionInitial(finalData)) }
            }.onFailure { error ->
                android.util.Log.e("MapViewModel", "Failed to calculate route after moving destination up: ${error.message}", error)
                _state.update { it.copy(bottomSheetState = BottomSheetState.DirectionInitial(updatedData), searchUiState = SearchUiState.Error(query = "", message = "Getting route failed.")) }
            }
        }
    }

    /**
     * Handle overlay panel expansion state change
     * When expanded, hides the bottom sheet
     */
    fun onOverlayPanelExpandedChange(isExpanded: Boolean) {
        _state.update { it.copy(isOverlayPanelExpanded = isExpanded) }
    }

    private fun startLocationUpdates() {
        // Start GPS location updates using FusedLocationProviderClient
        locationProvider.startLocationUpdates { location ->
            // Update state with real GPS location
            _state.update {
                it.copy(
                    currentLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }

            // Fetch reverse geocoded location for display
            fetchReverseGeocodedLocation(location.latitude, location.longitude)
        }
    }

    /**
     * Fetches reverse geocoded location to display readable location name
     * Only updates if user has moved significant distance (50+ meters)
     */
    private fun fetchReverseGeocodedLocation(latitude: Double, longitude: Double) {
        // Check if we need to update based on distance moved
        lastReverseGeocodedLatLng?.let { (lastLat, lastLon) ->
            val distanceMoved = DistanceUtils.calculateDistance(
                lat1 = lastLat,
                lon1 = lastLon,
                lat2 = latitude,
                lon2 = longitude
            )

            // If moved less than threshold, don't update
            if (distanceMoved < MIN_DISTANCE_FOR_UPDATE_METERS) {
                return
            }
        }

        viewModelScope.launch {
            // Set loading state
            _state.update { it.copy(reverseGeocodedLocation = ReverseGeocodedLocation.loading()) }

            val result = repository.reverseGeocodeLocation(latitude, longitude, radius = 100)
            result.onSuccess { geocodedLocation ->
                _state.update { it.copy(reverseGeocodedLocation = geocodedLocation) }
                // Update last geocoded position
                lastReverseGeocodedLatLng = Pair(latitude, longitude)
            }.onFailure {
                _state.update { it.copy(reverseGeocodedLocation = ReverseGeocodedLocation.unknown()) }
            }
        }
    }

    // ============================================
    // Navigation Methods
    // ============================================

    /**
     * Start active navigation with GPS tracking and step threshold detection.
     * Initializes navigation state with the first step and begins location tracking.
     */
    fun startNavigation() {
        val dirData = _state.value.directionData ?: return
        val selectedRoute = dirData.getSelectedRoute() ?: return

        if (selectedRoute.steps.isEmpty()) {
            android.util.Log.w("MapViewModel", "Cannot start navigation: no steps in route")
            return
        }

        // Debug: Log route steps for verification
        android.util.Log.d("MapViewModel", "Starting navigation with ${selectedRoute.steps.size} steps:")
        selectedRoute.steps.forEachIndexed { index, step ->
            android.util.Log.d("MapViewModel", "Step $index: ${step.instruction} at (${step.latitude}, ${step.longitude})")
        }

        // Initialize navigation progress manager
        navigationProgressManager = com.example.lakbaylaya.ui.screens.map.navigation.progress.NavigationProgressManager().apply {
            // Set thresholds for step advancement
            setStepThreshold(5.0) // 5 meters to advance step (more responsive for walking)
            setGpsAccuracyThreshold(15.0) // Only use GPS with accuracy better than 15m

            // Start navigation with callbacks
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
                // Wire destination arrival to mark navigation complete and update UI
                onDestinationArrived = {
                    // When arrival detector fires, treat as navigation complete
                    onNavigationComplete()
                }
            )
        }

        // Create active navigation state with the first step immediately available
        val initialStep = selectedRoute.steps.firstOrNull()
        val navigationState = NavigationState.Active(
            currentStepIndex = 0,
            routeOption = selectedRoute,
            currentLocation = _state.value.currentLocation?.let { loc ->
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

        _state.update {
            it.copy(
                navigationState = navigationState,
                // Hide direction bottom sheet when navigation starts
                bottomSheetState = BottomSheetState.Hidden
            )
        }

        android.util.Log.d("MapViewModel", "Navigation started with ${selectedRoute.steps.size} steps")

        // Log first step for immediate display
        initialStep?.let { step ->
            android.util.Log.d("MapViewModel", "Initial step ready: ${step.instruction}")
        }
    }

    /**
     * Update user location during navigation
     * This method should be called from MapScreen when GPS location updates
     */
    fun updateNavigationLocation(location: android.location.Location) {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        android.util.Log.d("MapViewModel", "GPS Update: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")

        // Update navigation state with new location
        _state.update {
            it.copy(navigationState = navState.updateLocation(location))
        }

        // Update progress manager with new location for step threshold detection
        navigationProgressManager?.updateLocation(location)

        // If the progress manager reports we're within the destination threshold,
        // treat that as arrival and mark navigation completed so UI can show Done.
        try {
            val withinThreshold = navigationProgressManager?.isWithinDestinationThreshold() == true
            if (withinThreshold) {
                android.util.Log.d("MapViewModel", "Detected within destination threshold - marking navigation complete")
                onNavigationComplete()
            } else {
                // Fallback: check raw distance to destination if available and within a small buffer (25m)
                val distanceToDest = navigationProgressManager?.getDistanceToDestination()
                if (distanceToDest != null && distanceToDest <= 25.0) {
                    android.util.Log.d("MapViewModel", "Distance to destination ${distanceToDest}m <= 25m - marking navigation complete (fallback)")
                    onNavigationComplete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapViewModel", "Error checking arrival threshold: ${e.message}", e)
        }

        android.util.Log.d("MapViewModel", "Navigation location updated and sent to progress manager")
    }

    /**
     * Update step count from pedometer (for UI display)
     */
    fun updateNavigationStepCount(stepCount: Int) {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        _state.update {
            it.copy(navigationState = navState.updateStepCount(stepCount))
        }
    }

    /**
     * Called when navigation step is automatically advanced due to GPS threshold
     */
    private fun onNavigationStepAdvanced(step: DirectionStep, stepIndex: Int) {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        val newNavState = navState.copy(
            currentStepIndex = stepIndex,
            distanceCovered = 0.0 // Reset step distance
        )

        _state.update {
            it.copy(navigationState = newNavState)
        }

        android.util.Log.d("MapViewModel", "Advanced to step $stepIndex: ${step.instruction}")
    }

    /**
     * Called when navigation progress is updated
     */
    private fun onNavigationProgressUpdate(stepDistance: Double, totalDistance: Double) {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        val newNavState = navState.updateStepDistance(stepDistance, totalDistance)

        _state.update {
            it.copy(navigationState = newNavState)
        }
    }

    /**
     * Called when navigation is complete
     */
    private fun onNavigationComplete() {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        // Mark navigation as completed so UI can show Done state
        val completedState = navState.copy(isCompleted = true)
        _state.update {
            it.copy(navigationState = completedState)
        }

        // Stop internal progress manager to free resources
        navigationProgressManager?.stopNavigation()

        android.util.Log.d("MapViewModel", "Navigation completed - marked as completed in state")
    }

    /**
     * Stop active navigation and return to direction preview mode
     */
    fun stopNavigation() {
        navigationProgressManager?.stopNavigation()
        _state.update {
            it.copy(
                navigationState = NavigationState.Inactive,
                // Restore direction bottom sheet
                bottomSheetState = it.directionData?.let { data ->
                    BottomSheetState.DirectionInitial(data)
                } ?: BottomSheetState.Hidden
            )
        }

        android.util.Log.d("MapViewModel", "Navigation stopped")
    }

    /**
     * Finish navigation explicitly (Done action): stop navigation,
     * close navigation UI, show user location bottom sheet and the search bar
     */
    fun finishNavigation() {
        navigationProgressManager?.stopNavigation()
        _state.update {
            it.copy(
                navigationState = NavigationState.Inactive,
                bottomSheetState = BottomSheetState.Hidden,
                isSearchOverlayActive = false,
                uiMode = UiMode.Normal
            )
        }

        android.util.Log.d("MapViewModel", "Navigation finished (Done) - returned to normal mode")
    }

    /**
     * Toggle mute/unmute for voice guidance
     */
    fun toggleNavigationMute() {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        _state.update {
            it.copy(navigationState = navState.toggleMute())
        }
    }

    /**
     * Toggle between 2D and 3D map modes
     */
    fun toggleNavigationMapMode() {
        val navState = _state.value.navigationState
        if (navState !is NavigationState.Active) return

        _state.update {
            it.copy(navigationState = navState.toggleMapMode())
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchDebouncer.cancel()
        locationProvider.stopLocationUpdates()
        navigationProgressManager?.stopNavigation()
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
}
