package com.example.lakbaylaya.ui.screens.map.components.markerEdit

import androidx.compose.runtime.*
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import com.example.lakbaylaya.ui.screens.map.models.SearchResult

/**
 * Full-screen map editor overlay for selecting a new marker location.
 * Allows user to pan the map with a centered green marker and confirm the new location.
 * @param isVisible Controls overlay visibility
 * @param mapManager MapLibreManager instance to control map panning and camera
 * @param onLocationSelected Callback with latitude, longitude, and address when user confirms
 * @param onCancel Callback when user cancels the location selection
 */
@Composable
fun MarkerLocationEditorDialog(
    isVisible: Boolean,
    mapManager: MapLibreManager?,
    // Optional initial SearchResult when editor was opened from a search result
    initialResult: SearchResult? = null,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit,
    onCancel: () -> Unit
) {
    if (!isVisible) return

    MarkerMapEditorOverlay(
        mapManager = mapManager,
        initialLabel = initialResult?.placeName,
        initialLatitude = initialResult?.latitude,
        initialLongitude = initialResult?.longitude,
        mapContent = {
            // The actual MapLibre map will be rendered behind this overlay
            // by the parent MapScreen composable
        },
        onLocationSelected = onLocationSelected,
        onCancel = onCancel
    )
}
