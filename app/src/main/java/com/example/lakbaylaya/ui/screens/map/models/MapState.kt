package com.example.lakbaylaya.ui.screens.map.models

/**
 * Data class representing the state of the map screen
 *
 * @property uiMode Current UI mode (Normal or Direction)
 * @property searchQuery Current search query text
 * @property searchUiState Current search UI state (Idle, Typing, Searching, Results, Empty, Error)
 * @property isSearchOverlayActive Whether the search overlay is visible
 * @property recentSearches List of recent search results
 * @property currentLocation User's current location
 * @property reverseGeocodedLocation Reverse geocoded location name for current position
 * @property selectedResult Currently selected search result
 * @property selectedMarker Currently selected marker on the map
 * @property bottomSheetState Current bottom sheet state
 * @property hasLocationPermission Whether location permission is granted
 * @property isUserLocationSheetExpanded Whether user location bottom sheet is expanded
 * @property directionData Current direction data (null when not in direction mode)
 * @property polylines List of route polylines to draw on map
 * @property isOverlayPanelExpanded Whether the overlay stop panel is expanded (hides bottom sheet when true)
 * @property navigationState Current navigation state (Inactive or Active with step-by-step guidance)
 * @property isMarkerEditing Whether marker editor overlay is active for repositioning pin
 * @property markerEditingResult The search result being edited (for repositioning pin)
 * @property editingStopIndex Index of stop being edited in direction mode (null if editing regular search result)
 */
data class MapState(
    val uiMode: UiMode = UiMode.Normal,
    val searchQuery: String = "",
    val searchUiState: SearchUiState = SearchUiState.Idle,
    val isSearchOverlayActive: Boolean = false,
    val recentSearches: List<SearchResult> = emptyList(),
    val currentLocation: Location? = null,
    val reverseGeocodedLocation: ReverseGeocodedLocation? = null,
    val selectedResult: SearchResult? = null,
    val selectedMarker: SearchResult? = null,
    val bottomSheetState: BottomSheetState = BottomSheetState.Hidden,
    val hasLocationPermission: Boolean = false,
    val isUserLocationSheetExpanded: Boolean = true,
    val directionData: DirectionData? = null,
    val polylines: List<PolylineData> = emptyList(),
    val isOverlayPanelExpanded: Boolean = false,
    val navigationState: NavigationState = NavigationState.Inactive,
    val pendingVoiceNotesPlace: SearchResult? = null,
    val isVoiceListening: Boolean = false,
    val isMarkerEditing: Boolean = false,
    val markerEditingResult: SearchResult? = null,
    val editingStopIndex: Int? = null,
    /** Non-null when Save Place succeeded — UI shows confirmation popup then clears this. */
    val pendingSavePlaceResult: SavePlaceResult? = null,
    /**
     * True while voice-dialog search is active — suppresses FloatingSearchBar keyboard focus
     * so the keyboard doesn't open and trigger the debounce race with searchNow().
     */
    val isVoiceSearchActive: Boolean = false,
    /** Non-null when an arrival SMS was sent (or failed). MapScreen shows a Toast then clears this. */
    val pendingArrivalToast: String? = null
)

/**
 * Carries the result of a successful Save Place operation so the UI can
 * show a confirmation dialog/snackbar and surface the data in Route screen.
 */
data class SavePlaceResult(
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val label: String
)

/**
 * Data class representing a polyline to draw on the map
 */
data class PolylineData(
    val routeId: String,
    val coordinates: List<Pair<Double, Double>>,
    val isPrimary: Boolean,
    val color: Int? = null
)
