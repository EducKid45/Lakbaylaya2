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
    val pendingVoiceNotesPlace: SearchResult? = null
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
