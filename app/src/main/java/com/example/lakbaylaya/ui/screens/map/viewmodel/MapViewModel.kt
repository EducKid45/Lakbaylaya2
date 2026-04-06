package com.example.lakbaylaya.ui.screens.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.repository.MapRepository
import com.example.lakbaylaya.data.repository.MapRepositoryImpl
import com.example.lakbaylaya.data.repository.SavedPlaceRepositoryImpl
import com.example.lakbaylaya.data.repository.UserProfileRepository
import com.example.lakbaylaya.ui.screens.map.location.MapLocationController
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.ui.screens.route.SavedPlace
import com.example.lakbaylaya.utils.LocationProvider
import kotlinx.coroutines.delay
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
        private const val MIN_DISTANCE_FOR_UPDATE_METERS = 50.0
    }

    // Lazy repository for saving places to the Room database
    private val savedPlaceRepository by lazy {
        SavedPlaceRepositoryImpl(application.applicationContext)
    }

    // Profile repository for reading emergency contact + arrival SMS toggle
    private val profileRepository by lazy {
        UserProfileRepository.create(application.applicationContext)
    }

    // TTS manager used to speak the final recognized search phrase ("Searching for ...")
    private val voiceManager by lazy { com.example.lakbaylaya.voice.VoiceManager(getApplication()) }

    // Mutable state
    private val _state = MutableStateFlow(MapState())

    // Public immutable state
    val state: StateFlow<MapState> = _state.asStateFlow()

    // MapLibre manager reference (set by MapScreen after initialization)
    var mapManager: com.example.lakbaylaya.maplibre.manager.MapLibreManager? = null

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
        _state,
        application
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

        // Register arrival callback: when navigation completes, show toast and optionally send arrival SMS
        navigationManager.onNavigationCompleted = { destName, destLat, destLon ->
            viewModelScope.launch {
                // Always show an arrival toast regardless of SMS setting
                val arrivalMsg = "You have arrived at $destName!"
                _state.update { it.copy(pendingArrivalToast = arrivalMsg) }

                // Send SMS only if toggle is enabled and contact is set
                val profile = profileRepository.getProfile() ?: return@launch
                if (!profile.autoSendArrivalNotification) return@launch
                val phone = profile.emergencyContactNumber.trim()
                if (phone.isBlank()) return@launch

                val mapsLink = "https://maps.google.com/?q=$destLat,$destLon"
                val contactName = profile.emergencyContactName.trim()
                val smsMessage = buildString {
                    append("I have arrived safely at $destName.")
                    append(" Location: $mapsLink")
                }

                try {
                    val smsManager: android.telephony.SmsManager =
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                            application.getSystemService(android.telephony.SmsManager::class.java)
                        else
                            @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()

                    val parts = smsManager.divideMessage(smsMessage)
                    if (parts.size == 1) {
                        smsManager.sendTextMessage(phone, null, smsMessage, null, null)
                    } else {
                        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                    }
                    // Update toast to include SMS confirmation
                    val displayName = contactName.ifBlank { phone }
                    _state.update { it.copy(pendingArrivalToast = "Arrived at $destName! Arrival SMS sent to $displayName") }
                    android.util.Log.i("MapViewModel", "Arrival SMS sent to $phone for $destName")
                } catch (e: Exception) {
                    android.util.Log.e("MapViewModel", "Arrival SMS failed: ${e.message}", e)
                    // Keep the base toast even if SMS fails
                }
            }
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
     * Called by the voice-dialog bridge.
     * Opens the search overlay immediately (user sees Searching spinner → live results),
     * blocks keyboard via isVoiceSearchActive, then fires searchNow() — the same
     * performSearch() pipeline the search bar uses, bypassing debounce and 3-char minimum.
     * flow.first{} in MapScreen.LaunchedEffect catches the Results state from this single call.
     */
    /**
     * Called by the voice-dialog bridge in MapScreen.
     * Opens the overlay immediately (user sees Searching spinner → live results),
     * blocks keyboard via isVoiceSearchActive, fires API via searchNow, and
     * delivers results directly via [onResults] callback — no StateFlow polling race.
     */
    fun triggerVoiceSearch(
        query: String,
        onResults: (List<com.example.lakbaylaya.ui.screens.map.models.SearchResult>) -> Unit
    ) {
        android.util.Log.d("MapViewModel", "triggerVoiceSearch: '$query'")
        _state.update {
            it.copy(
                searchQuery = query,
                searchUiState = com.example.lakbaylaya.ui.screens.map.models.SearchUiState.Idle,
                isSearchOverlayActive = true,
                isVoiceSearchActive = true,
                isVoiceListening = false
            )
        }
        searchManager.searchNow(query, onResults)
    }

    /** No-op — kept for call-site compatibility. */
    fun openSearchOverlayAfterVoice() = Unit

    /** Voice recognition integration - update listening flag and accept voice transcripts */
    fun startVoiceListening() {
        _state.update { it.copy(isVoiceListening = true, isSearchOverlayActive = true) }
        // TTS prompt moved to SpeechRecognitionManager.onReadyForSpeech() when using SpeechRecognizer,
        // but MapScreen uses the system activity recognizer, so provide an explicit cue method below.
    }

    fun playListeningCue() {
        try {
            voiceManager.speak("I'm listening")
        } catch (e: Exception) {
            android.util.Log.w("MapViewModel", "TTS listening cue failed: ${e.message}")
        }
    }

    fun stopVoiceListening() {
        _state.update { it.copy(isVoiceListening = false) }
        // SpeechRecognitionManager will stop TTS when recognition ends or is cancelled if used.
    }

    /** Called when voice recognizer returns text. If final, we set the search query and trigger search flow. */
    fun onVoiceResult(text: String, isFinal: Boolean) {
        if (isFinal) {
            // Update UI immediately
            _state.update { it.copy(searchQuery = text, isVoiceListening = false) }

            // Speak the phrase we will search for, then trigger the actual search after a short delay
            viewModelScope.launch {
                try {
                    // Use TTS to confirm/search phrase vocally
                    voiceManager.speak("Searching for $text")
                } catch (e: Exception) {
                    android.util.Log.w("MapViewModel", "TTS speak failed: ${e.message}")
                }

                // Small delay to let the spoken phrase start before triggering network search
                delay(450)

                // Trigger search manager after speaking
                searchManager.onSearchQueryChange(text)
            }
        } else {
            // Update query shown in UI but don't trigger debounced search for partials
            _state.update {
                it.copy(
                    searchQuery = text,
                    searchUiState = SearchUiState.Typing(text)
                )
            }
        }
    }

    /**
     * Activates the search overlay and requests focus
     */
    fun onSearchBarClick() {
        // Clear isVoiceSearchActive so keyboard and normal search work on manual interaction
        _state.update { it.copy(isSearchOverlayActive = true, isVoiceSearchActive = false) }
    }

    /**
     * Deactivates search overlay (back button pressed)
     */
    fun onBackClick() {
        searchManager.cancelPending()
        _state.update {
            it.copy(
                isSearchOverlayActive = false,
                isVoiceSearchActive = false,
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
     * Opens marker editor overlay to reposition pin for a search result
     * Closes search overlay and shows marker editor with centered pin
     * Clears the selected marker so search icon disappears during editing
     */
    fun onSearchResultMarkerEdit(result: SearchResult) {
        android.util.Log.d(
            "MapViewModel",
            "onSearchResultMarkerEdit: opening editor for ${result.placeName} at ${result.latitude},${result.longitude}"
        )
        _state.update {
            it.copy(
                isSearchOverlayActive = false, // Close search overlay
                isMarkerEditing = true, // Open marker editor
                markerEditingResult = result, // Store result being edited
                searchUiState = SearchUiState.Idle,
                selectedMarker = result, // Show marker icon during editing so the pin is visible
                bottomSheetState = BottomSheetState.Hidden // Hide bottom sheet during editing
            )
        }

        // Move map camera to the search result location
        viewModelScope.launch {
            mapManager?.animateTo(
                latitude = result.latitude,
                longitude = result.longitude,
                zoom = 17.0
            )
            android.util.Log.d(
                "MapViewModel",
                "onSearchResultMarkerEdit: animated to ${result.latitude},${result.longitude}"
            )
        }
    }

    /**
     * Opens marker editor for a stop in direction mode
     * Allows repositioning stop locations from search results
     */
    fun onEditStopLocation(stopIndex: Int) {
        val directionData = _state.value.directionData ?: return
        if (stopIndex !in directionData.stops.indices) return

        val stop = directionData.stops[stopIndex]

        // Create a SearchResult from the stop for editing
        val stopAsResult = SearchResult(
            id = "stop_$stopIndex",
            placeName = stop.name,
            address = stop.address,
            latitude = stop.latitude,
            longitude = stop.longitude,
            category = "",
            iconType = com.example.lakbaylaya.ui.screens.map.models.PlaceIconType.LOCATION,
            distanceMeters = 0.0
        )

        _state.update {
            it.copy(
                isSearchOverlayActive = false,
                isMarkerEditing = true,
                markerEditingResult = stopAsResult,
                selectedMarker = null,
                bottomSheetState = BottomSheetState.Hidden,
                // Store metadata about which stop is being edited
                editingStopIndex = stopIndex
            )
        }

        // Move camera to stop location
        viewModelScope.launch {
            mapManager?.animateTo(
                latitude = stop.latitude,
                longitude = stop.longitude,
                zoom = 17.0
            )
        }
    }

    /**
     * Handles when user confirms new marker position after editing
     * Updates the result with new coordinates and refreshes Place Bottom Sheet
     * Also handles updating stops in Direction mode
     */
    fun onMarkerEditConfirmed(latitude: Double, longitude: Double, address: String) {
        android.util.Log.d(
            "MapViewModel",
            "onMarkerEditConfirmed: confirmed at $latitude,$longitude ($address)"
        )
        val editingResult = _state.value.markerEditingResult ?: return
        val editingStopIndex = _state.value.editingStopIndex

        // Check if we're editing a stop in direction mode
        if (editingStopIndex != null) {
            onStopEditConfirmed(editingStopIndex, latitude, longitude, address)
            return
        }

        // Create updated search result with new coordinates
        val updatedResult = editingResult.copy(
            latitude = latitude,
            longitude = longitude,
            address = address
        )

        // Add updated result to recent searches
        val updatedRecent = searchManager.addToRecentSearches(updatedResult)

        // Close marker editor. Show the place bottom sheet with the updated location so the user
        // can see place details and confirm/cancel actions after re-marking.
        _state.update {
            it.copy(
                isMarkerEditing = false,
                markerEditingResult = null,
                selectedResult = updatedResult,
                selectedMarker = updatedResult, // Show marker at new position
                // Show the place sheet reflecting the new coordinates and details
                bottomSheetState = BottomSheetState.Initial(updatedResult),
                // Ensure search overlay and typing state are cleared
                isSearchOverlayActive = false,
                searchQuery = "",
                searchUiState = SearchUiState.Idle,
                recentSearches = updatedRecent // Update recent searches
            )
        }
        android.util.Log.d(
            "MapViewModel",
            "onMarkerEditConfirmed: state updated; bottomSheetState=Initial with ${updatedResult.latitude},${updatedResult.longitude}"
        )
    }

    /**
     * Handles confirming edited stop location
     * Updates the stop coordinates and recalculates the route
     */
    private fun onStopEditConfirmed(
        stopIndex: Int,
        latitude: Double,
        longitude: Double,
        address: String
    ) {
        val currentData = _state.value.directionData ?: return
        if (stopIndex !in currentData.stops.indices) return

        val oldStop = currentData.stops[stopIndex]
        val updatedStop = oldStop.copy(
            latitude = latitude,
            longitude = longitude,
            address = address
        )

        val newStops = currentData.stops.toMutableList()
        newStops[stopIndex] = updatedStop

        val updatedData = currentData.copy(stops = newStops)

        // Close editor and show loading state
        _state.update {
            it.copy(
                isMarkerEditing = false,
                markerEditingResult = null,
                editingStopIndex = null,
                directionData = updatedData.copy(routes = emptyList()),
                polylines = emptyList(),
                bottomSheetState = BottomSheetState.DirectionInitial(updatedData)
            )
        }

        // Recalculate route with updated stop
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
                android.util.Log.e(
                    "MapViewModel",
                    "Failed to recalculate route after editing stop: ${error.message}",
                    error
                )
                _state.update {
                    it.copy(
                        bottomSheetState = BottomSheetState.DirectionInitial(updatedData),
                        searchUiState = SearchUiState.Error(
                            query = "",
                            message = "Failed to recalculate route."
                        )
                    )
                }
            }
        }
    }

    /**
     * Handles when user cancels marker editing
     */
    fun onMarkerEditCancelled() {
        _state.update {
            it.copy(
                isMarkerEditing = false,
                markerEditingResult = null,
                editingStopIndex = null // Clear editing stop index on cancel
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
            PlaceAction.START_NAVIGATION -> showDirections(place, autoStart = true)
            PlaceAction.SAVE_PLACE -> savePlaceToFavorites(place)
            PlaceAction.MARK_LOCATION -> markLocation(place)
            PlaceAction.SHARE -> sharePlace(place)
            PlaceAction.CALL -> callPlace(place)
            PlaceAction.DIRECTIONS -> showDirections(place, autoStart = false)
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

    private fun savePlaceToFavorites(place: SearchResult) {
        // Guard: require a valid place name and coordinates
        if (place.placeName.isBlank()) {
            android.util.Log.w("MapViewModel", "savePlaceToFavorites: place name is blank, skipping")
            return
        }
        viewModelScope.launch {
            try {
                val savedPlace = SavedPlace(
                    id        = java.util.UUID.randomUUID().toString(),
                    placeName = place.placeName,
                    address   = place.address.ifBlank { place.placeName },
                    latitude  = place.latitude,
                    longitude = place.longitude,
                    label     = place.category.ifBlank { "Saved" },
                    category  = place.category,
                    createdAt = System.currentTimeMillis()
                )
                val result = savedPlaceRepository.savePlace(savedPlace)
                if (result.isSuccess) {
                    // Signal to UI to show confirmation popup
                    _state.update {
                        it.copy(
                            pendingSavePlaceResult = com.example.lakbaylaya.ui.screens.map.models.SavePlaceResult(
                                placeName = savedPlace.placeName,
                                address   = savedPlace.address,
                                latitude  = savedPlace.latitude,
                                longitude = savedPlace.longitude,
                                label     = savedPlace.label
                            )
                        )
                    }
                    android.util.Log.d("MapViewModel", "Place saved: ${savedPlace.placeName}")
                } else {
                    android.util.Log.e("MapViewModel", "Failed to save place: ${result.exceptionOrNull()?.message}")
                }
            } catch (t: Throwable) {
                android.util.Log.e("MapViewModel", "Exception saving place: ${t.message}", t)
            }
        }
    }

    /** Called by UI after showing the Save Place confirmation dialog. */
    fun clearPendingSavePlaceResult() {
        _state.update { it.copy(pendingSavePlaceResult = null) }
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

    /**
     * Modified showDirections to accept autoStart parameter
     */
    private fun showDirections(place: SearchResult, autoStart: Boolean = false) {
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

                // If requested, start navigation immediately (uses same action as DirectionBottomSheet Start)
                if (autoStart) {
                    startNavigation()
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
        // Shutdown voice manager used by this ViewModel
        try {
            voiceManager.shutdown()
        } catch (e: Exception) {
            android.util.Log.w("MapViewModel", "TTS shutdown failed: ${e.message}")
        }
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

    /**
     * Test method to manually trigger arrival (for debugging arrival detection issues)
     * This should immediately show "You have arrived!" and the Done button
     */
    fun testArrival() {
        val navState = state.value.navigationState
        if (navState is NavigationState.Active) {
            android.util.Log.i("MapViewModel", "🧪 TEST: Manually triggering arrival for debugging")
            navigationManager.testArrival()
        } else {
            android.util.Log.w("MapViewModel", "Cannot test arrival - navigation not active")
        }
    }

    /** Public API to start directions to a specific lat/lon and optional name. Used by NavGraph navigation. */
    fun startNavigationTo(destLat: Double, destLon: Double, name: String = "", autoStart: Boolean = false) {
        // Build a lightweight SearchResult to reuse existing showDirections code
        val place = com.example.lakbaylaya.ui.screens.map.models.SearchResult(
            id = java.util.UUID.randomUUID().toString(),
            placeName = name.ifBlank { "Destination" },
            address = name.ifBlank { "" },
            latitude = destLat,
            longitude = destLon,
            distanceMeters = 0.0,
            category = "",
            isRecent = false,
            iconType = com.example.lakbaylaya.ui.screens.map.models.PlaceIconType.LOCATION
        )
        // Use coroutine to avoid calling showDirections synchronously during composition/navigation race
        viewModelScope.launch {
            try {
                // Small delay to allow ViewModel and MapScreen to settle
                kotlinx.coroutines.delay(200)
                showDirections(place, autoStart = autoStart)
            } catch (t: Throwable) {
                android.util.Log.w("MapViewModel", "startNavigationTo failed: ${t.message}")
            }
        }
    }

    /**
     * Select a saved place by coordinates/name and show it on the map with the bottom sheet —
     * WITHOUT entering Direction mode, without touching the search bar, without a search overlay.
     * Used when the user taps a saved place in the Routes → Places tab.
     */
    fun selectPlaceAt(destLat: Double, destLon: Double, name: String = "") {
        val place = SearchResult(
            id = java.util.UUID.randomUUID().toString(),
            placeName = name.ifBlank { "Saved Place" },
            address = name.ifBlank { "" },
            latitude = destLat,
            longitude = destLon,
            distanceMeters = 0.0,
            category = "",
            isRecent = false,
            iconType = PlaceIconType.LOCATION
        )
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(200)
                // Update state: select the place and show bottom sheet, but do NOT change
                // searchQuery or searchOverlayActive — the user came from Saved Places and
                // does not need the search bar.
                _state.update {
                    it.copy(
                        selectedResult = place,
                        selectedMarker = place,
                        uiMode = UiMode.Normal,          // ensure we are in Normal mode
                        isSearchOverlayActive = false,   // keep search overlay closed
                        bottomSheetState = BottomSheetState.Initial(place)
                    )
                }
                // Animate camera to the saved place coordinates
                mapManager?.animateTo(
                    latitude = destLat,
                    longitude = destLon,
                    zoom = 15.0
                )
                android.util.Log.d("MapViewModel", "selectPlaceAt: pinned $name at $destLat,$destLon")
            } catch (t: Throwable) {
                android.util.Log.w("MapViewModel", "selectPlaceAt failed: ${t.message}")
            }
        }
    }

    /** Clear the pending arrival toast after it has been shown. */
    fun clearPendingArrivalToast() {
        _state.update { it.copy(pendingArrivalToast = null) }
    }
}
