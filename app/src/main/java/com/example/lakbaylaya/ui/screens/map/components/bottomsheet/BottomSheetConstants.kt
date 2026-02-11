package com.example.lakbaylaya.ui.screens.map.components.bottomsheet

import androidx.compose.ui.unit.dp

/**
 * Configuration constants for bottom sheet behavior and appearance
 */
object BottomSheetConstants {

    // Height configurations
    val HANDLE_ONLY_HEIGHT = 28.dp
    val INITIAL_STATE_HEIGHT = 180.dp
    const val HALF_STATE_SCREEN_RATIO = 0.5f
    const val FULL_STATE_SCREEN_RATIO = 0.85f

    // Direction sheet height configurations
    const val DIRECTION_INITIAL_SCREEN_RATIO = 0.42f  // ~40% for route overview
    const val DIRECTION_FULL_SCREEN_RATIO = 0.1f     // full screen when expanded

    // Gesture thresholds
    const val DRAG_THRESHOLD_PX = 100f
    const val FLING_THRESHOLD_PX = 50f

    // Animation configuration (Google Maps-like smooth animation)
    const val ANIMATION_DURATION_MS = 300
    const val ANIMATION_STIFFNESS = 400f
    const val ANIMATION_DAMPING_RATIO = 0.75f

    // Visual styling
    val CORNER_RADIUS = 20.dp
    val TONAL_ELEVATION = 8.dp
    val SHADOW_ELEVATION = 8.dp

    // Drag handle styling
    val DRAG_HANDLE_WIDTH = 32.dp
    val DRAG_HANDLE_HEIGHT = 4.dp
    val DRAG_HANDLE_CORNER_RADIUS = 2.dp
    val DRAG_HANDLE_VERTICAL_PADDING = 12.dp
    const val DRAG_HANDLE_ALPHA = 0.4f
}
