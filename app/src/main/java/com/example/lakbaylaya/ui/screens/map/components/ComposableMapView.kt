package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import com.example.lakbaylaya.maplibre.config.MapStyleConfig
import org.maplibre.android.maps.Style

/**
 * Compose wrapper for MapLibre native map view
 *
 * This component integrates MapLibre's Android SDK with Jetpack Compose.
 * It handles the lifecycle of the MapView and provides a clean interface
 * for map operations.
 *
 * Features:
 * - Native MapLibre rendering
 * - Lifecycle-aware (handles pause/resume/destroy)
 * - Customizable style
 * - Camera control
 * - Marker support
 * - Location component (blue puck)
 *
 * @param modifier Modifier for customization
 * @param stylePreset Map style to use
 * @param hasLocationPermission Whether location permission is granted
 * @param onMapReady Callback when map is ready
 * @param mapManager Optional external MapLibreManager instance
 */
@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    stylePreset: MapStyleConfig.StylePreset = MapStyleConfig.StylePreset.STREETS,
    hasLocationPermission: Boolean = false,
    onMapReady: ((MapLibreManager) -> Unit)? = null,
    mapManager: MapLibreManager? = null
) {
    val context = LocalContext.current
    val manager = remember { mapManager ?: MapLibreManager(context) }
    var currentStyle by remember { mutableStateOf<Style?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            manager.onDestroy()
        }
    }

    // Enable/disable location when permission changes and map is ready
    LaunchedEffect(hasLocationPermission, currentStyle) {
        currentStyle?.let { style ->
            if (hasLocationPermission) {
                manager.enableLocationComponent(style)
            } else {
                manager.disableLocationComponent()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                getMapAsync { map ->
                    manager.setMap(map)
                    manager.loadStyle(stylePreset) { style ->
                        currentStyle = style
                        // Enable location component if permission granted
                        if (hasLocationPermission) {
                            manager.enableLocationComponent(style)
                        }
                        onMapReady?.invoke(manager)
                    }
                }
            }
        },
        update = { _ ->
            // Update when composable recomposes
            // Can add dynamic updates here if needed
        }
    )
}