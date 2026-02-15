package com.example.lakbaylaya.ui.screens.map.models

/**
 * Sealed class representing bottom sheet states for map interactions
 *
 * Bottom sheet displays different content based on current mode:
 * - Place mode: Shows selected place details with action buttons
 * - Direction mode: Shows route information with turn-by-turn steps
 *
 * Each state defines the visible height and content of the bottom sheet.
 */
sealed class BottomSheetState {

    /**
     * Hidden state - only drag handle visible or completely removed
     * Used when no place is selected or search is active
     * Direction sheet supports Hidden state as well (collapsed when overlay panel is expanded)
     */
    data object Hidden : BottomSheetState()

    // ========== Place Sheet States ==========

    /**
     * Initial state - minimal preview of selected place
     * Shows: place name, distance, quick action buttons (horizontally scrollable)
     */
    data class Initial(val place: SearchResult) : BottomSheetState()

    /**
     * Half-expanded state - detailed place information
     * Shows: name, address, distance, coordinates, estimated time, action buttons
     */
    data class Half(val place: SearchResult) : BottomSheetState()

    /**
     * Full state - complete place details
     * Shows: all information, all action buttons, scrollable content
     */
    data class Full(val place: SearchResult) : BottomSheetState()

    // ========== Direction Sheet States ==========

    /**
     * Direction Initial state (Collapsed) - route overview with stops
     * Shows: origin, destination, optional stops, route options (A/B), summary
     * Height: Similar to Half state (~40% screen)
     */
    data class DirectionInitial(
        val directionData: DirectionData
    ) : BottomSheetState()

    /**
     * Direction Full Expand state (Expanded) - complete turn-by-turn steps
     * Shows: all information from Initial + scrollable step list
     * Height: ~80% screen with scrollable steps
     */
    data class DirectionFullExpand(
        val directionData: DirectionData
    ) : BottomSheetState()
}

/**
 * Enum representing possible actions on a place
 */
enum class PlaceAction {
    START_NAVIGATION,
    DIRECTIONS,
    MARK_LOCATION,
    SAVE_PLACE,
    SHARE,
    CALL,
}
