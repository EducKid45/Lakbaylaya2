package com.example.lakbaylaya.ui.screens.map.models

/**
 * Sealed class representing the app's UI mode
 *
 * Determines which UI elements are visible and how the app behaves
 */
sealed class UiMode {
    /**
     * Normal mode - default map browsing with search
     * Shows: Search bar, Map, Location/Place sheets
     */
    data object Normal : UiMode()

    /**
     * Direction mode - navigation planning with routes
     * Shows: Overlay Stop Panel, Map with polylines, Direction sheet
     * Hides: Search bar
     */
    data class Direction(
        val isEditingStops: Boolean = false
    ) : UiMode()
}

