package com.example.lakbaylaya.maplibre.manager

import android.content.Context
import android.util.Log
import com.example.lakbaylaya.BuildConfig
import com.example.lakbaylaya.maplibre.config.MapCameraConfig
import com.example.lakbaylaya.maplibre.config.MapStyleConfig
import com.example.lakbaylaya.maplibre.location.MapLocationManager
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import com.example.lakbaylaya.maplibre.markers.MarkerManager
import com.example.lakbaylaya.maplibre.markers.StopMarkerManager
import com.example.lakbaylaya.maplibre.route.polyline.PolylineManager

/**
 * This class encapsulates all MapLibre-specific operations and provides
 * a interface for the UI layer to interact with the map.
 *
 * Uses modern MapLibre style-layer rendering with GeoJsonSource + Layers.
 *
 */
class MapLibreManager(context: Context) {
    private var mapLibreMap: MapLibreMap? = null
    private var currentStyle: Style? = null
    private var currentStylePreset: MapStyleConfig.StylePreset = MapStyleConfig.StylePreset.STREETS
    private val locationManager: MapLocationManager = MapLocationManager(context)
    private val markerManager: MarkerManager = MarkerManager(context)
    private val stopMarkerManager: StopMarkerManager = StopMarkerManager()
    private val polylineManager: PolylineManager = PolylineManager()

    companion object {
        private var isInitialized = false

        /**
         * Initialize MapLibre instance (call once in Application class)
         */
        fun initialize(context: Context) {
            if (!isInitialized) {
                MapLibre.getInstance(context)
                isInitialized = true
            }
        }
    }

    /**
     * Set the MapLibre map instance
     */
    fun setMap(map: MapLibreMap) {
        this.mapLibreMap = map
        configureMap()
    }

    /**
     * Configure map settings
     */
    private fun configureMap() {
        mapLibreMap?.apply {
            // Enable user location
            uiSettings.apply {
                isCompassEnabled = true
                isAttributionEnabled = true
                isLogoEnabled = true
            }

            // Set initial camera position
            moveCamera(CameraUpdateFactory.newCameraPosition(MapCameraConfig.DEFAULT_POSITION))
        }
    }

    /**
     * Load map style and store reference for layer manipulation
     */
    fun loadStyle(
        stylePreset: MapStyleConfig.StylePreset = MapStyleConfig.StylePreset.STREETS,
        onStyleLoaded: ((Style) -> Unit)? = null
    ) {
        currentStylePreset = stylePreset
        val styleJson = MapStyleConfig.getStyleJson(stylePreset, BuildConfig.GEOAPIFY_API_KEY)

        mapLibreMap?.let { map ->
            try {
                val styleBuilder = Style.Builder().fromJson(styleJson)
                map.setStyle(styleBuilder) { style ->
                    currentStyle = style
                    initializeMapLayers(style)
                    onStyleLoaded?.invoke(style)
                }
            } catch (_: Exception) {
                try {
                    map.setStyle(styleJson) { style ->
                        currentStyle = style
                        initializeMapLayers(style)
                        onStyleLoaded?.invoke(style)
                    }
                } catch (inner: Exception) {
                    Log.e("MapLibreManager", "Failed to load style: ${inner.message}", inner)
                }
            }
        }
    }

    /**
     * Initialize GeoJSON sources and layers for markers and routes
     */
    private fun initializeMapLayers(style: Style) {
        try {
            // Delegate initialization to specialized managers
            markerManager.initialize(style)
            stopMarkerManager.initialize(style)
            polylineManager.initialize()
        } catch (e: Exception) {
            Log.e("MapLibreManager", "Failed to initialize layers: ${e.message}", e)
        }
    }

    /**
     * Enable location layer to show blue puck for user position.
     * Checks permission via the MapLocationManager and logs if not enabled.
     */
    fun enableLocationComponent(style: Style) {
        mapLibreMap?.let { map ->
            val enabled = locationManager.enable(map, style)
            if (!enabled) {
                Log.w(
                    "MapLibreManager",
                    "Location component not enabled (missing permission or error)"
                )
            }
        } ?: run {
            Log.w("MapLibreManager", "Map not initialized; cannot enable location component")
        }
    }

    /**
     * Disable location tracking (e.g., when permission is revoked)
     */
    fun disableLocationComponent() {
        mapLibreMap?.let { map ->
            val disabled = locationManager.disable(map)
            if (!disabled) {
                Log.w("MapLibreManager", "Failed to disable location component (security or error)")
            }
        } ?: run {
            Log.w("MapLibreManager", "Map not initialized; cannot disable location component")
        }
    }

    /**
     * Animate camera to a specific location
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param zoom Zoom level (default: ZOOM_STREET)
     * @param duration Animation duration in milliseconds
     */
    fun animateTo(
        latitude: Double,
        longitude: Double,
        zoom: Double = MapCameraConfig.ZOOM_STREET,
        duration: Int = MapCameraConfig.ANIMATION_DURATION_MEDIUM
    ) {
        val position = MapCameraConfig.createPosition(latitude, longitude, zoom)
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(position),
            duration
        )
    }

    /**
     * Add a marker at a specific location using GeoJSON source and layer
     *
     * @param latitude Marker latitude
     * @param longitude Marker longitude
     * @param title Marker title (stored as property)
     * @param persistent Whether the marker should persist across clearMarkers() calls
     */
    fun addMarker(
        latitude: Double,
        longitude: Double,
        title: String = "",
        persistent: Boolean = false
    ) {
        currentStyle?.let { style ->
            markerManager.addMarker(style, latitude, longitude, title, persistent)
        }
    }

    /**
     * Clear only temporary markers (preserves persistent markers)
     */
    fun clearTemporaryMarkers() {
        currentStyle?.let { style ->
            markerManager.clearMarkers(style)
        }
    }

    /**
     * Clear all markers including persistent ones
     */
    fun clearAllMarkers() {
        currentStyle?.let { style ->
            markerManager.clearAllMarkers(style)
        }
    }

    /**
     * Draw stop markers with labels A, B, C... using the StopMarkerManager
     */
    fun drawStopMarkers(points: List<Triple<Double, Double, String>>) {
        currentStyle?.let { style ->
            stopMarkerManager.drawStopMarkers(style, points)
        } ?: run {
            Log.w("MapLibreManager", "Style not initialized; cannot draw stop markers")
        }
    }

    /**
     * Clear stop markers previously added by this manager
     */
    fun clearStopMarkers() {
        currentStyle?.let { style ->
            stopMarkerManager.clearStopMarkers(style)
        } ?: run {
            Log.w("MapLibreManager", "Style not initialized; cannot clear stop markers")
        }
    }

    /**
     * Draw a polyline on the map for route visualization using GeoJSON + LineLayer
     *
     * @param routeId Unique identifier for this route
     * @param coordinates List of lat/lon pairs
     * @param isPrimary Whether this is the primary route (affects styling)
     * @param color Optional color (null = default based on isPrimary)
     */
    fun drawPolyline(
        routeId: String,
        coordinates: List<Pair<Double, Double>>,
        isPrimary: Boolean,
        color: Int? = null
    ) {
        currentStyle?.let { style ->
            polylineManager.drawPolyline(style, routeId, coordinates, isPrimary, color)
        } ?: run {
            Log.w("MapLibreManager", "Style not initialized; cannot draw polyline")
        }
    }

    /**
     * Clear all polylines from the map (sources & layers that start with ROUTE_ prefixes)
     */
    fun clearPolylines() {
        currentStyle?.let { style ->
            polylineManager.clearPolylines(style)
        } ?: run {
            Log.w("MapLibreManager", "Style not initialized; cannot clear polylines")
        }
    }

    /**
     * Pan the map by pixel offsets (screen-space).
     * This implementation converts the current map center to screen coordinates,
     * offsets by dx/dy, converts back to geographic coordinates, and moves/animates the camera.
     * This avoids reliance on CameraUpdateFactory.scrollBy which may not be available in some SDKs.
     */
    fun panBy(dx: Float, dy: Float, durationMs: Int = 0) {
        mapLibreMap?.let { map ->
            try {
                // Use projection to translate screen deltas to geographic coordinates
                val projection = map.projection
                // current center lat/lng (guard against null)
                val centerLatLng = map.cameraPosition.target ?: return
                // convert to screen point
                val centerPoint = projection.toScreenLocation(centerLatLng)
                // calculate target screen point
                val targetPoint = android.graphics.PointF(centerPoint.x + dx, centerPoint.y + dy)
                // convert back to lat/lng
                val targetLatLng = projection.fromScreenLocation(targetPoint)

                val position = MapCameraConfig.createPosition(
                    targetLatLng.latitude,
                    targetLatLng.longitude,
                    map.cameraPosition.zoom
                )
                if (durationMs > 0) {
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(position), durationMs)
                } else {
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(position))
                }
            } catch (e: Exception) {
                Log.w("MapLibreManager", "panBy failed (projection approach): ${e.message}")
            }
        }
    }

    /**
     * Return current map center coordinates (latitude, longitude) or null if not available
     */
    fun getCenter(): Pair<Double, Double>? {
        return mapLibreMap?.cameraPosition?.target?.let { target ->
            Pair(target.latitude, target.longitude)
        }
    }

    /**
     * Clean up resources
     */
    fun onDestroy() {
        clearAllMarkers()
        clearPolylines()
        clearStopMarkers()
        markerManager.onDestroy(currentStyle)
        stopMarkerManager.onDestroy(currentStyle)
        polylineManager.onDestroy(currentStyle)
        locationManager.onDestroy()
        currentStyle = null
        mapLibreMap = null
    }

    /** Add a preview marker (temporary but kept separately so it can be cleared without removing other temporaries) */
    fun addPreviewMarker(latitude: Double, longitude: Double, title: String = "") {
        currentStyle?.let { style ->
            markerManager.addPreviewMarker(style, latitude, longitude, title)
        }
    }

    /** Add a preview marker (returns a previewId for later exact promotion) */
    fun addPreviewMarkerWithId(latitude: Double, longitude: Double, title: String = ""): String? {
        currentStyle?.let { style ->
            return try {
                markerManager.addPreviewMarkerWithId(style, latitude, longitude, title)
                    .takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                Log.e("MapLibreManager", "addPreviewMarkerWithId failed: ${e.message}", e)
                null
            }
        }
        return null
    }

    /** Promote a preview marker by its previewId to persistent */
    fun promotePreviewToPersistentById(previewId: String, title: String = ""): Boolean {
        currentStyle?.let { style ->
            return try {
                markerManager.promotePreviewToPersistentById(style, previewId, title)
            } catch (e: Exception) {
                Log.e("MapLibreManager", "promotePreviewToPersistentById failed: ${e.message}", e)
                false
            }
        }
        return false
    }

    /**
     * Clear only preview markers
     */
    fun clearPreviewMarkers() {
        currentStyle?.let { style ->
            markerManager.clearPreviewMarkers(style)
        }
    }

    /**
     * Promote an existing preview/temporary marker at the coordinates to a persistent marker without flicker.
     */
    fun promoteMarkerToPersistent(latitude: Double, longitude: Double, title: String = "") {
        currentStyle?.let { style ->
            markerManager.promoteToPersistent(style, latitude, longitude, title)
        }
    }

}