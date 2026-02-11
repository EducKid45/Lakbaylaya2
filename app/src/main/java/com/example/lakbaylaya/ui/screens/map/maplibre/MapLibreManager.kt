package com.example.lakbaylaya.ui.screens.map.maplibre

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString
import com.example.lakbaylaya.BuildConfig
import com.example.lakbaylaya.ui.screens.map.maplibre.config.MapCameraConfig
import com.example.lakbaylaya.ui.screens.map.maplibre.config.MapStyleConfig

/**
 * Manager class for MapLibre map operations (Modern Style-Layer Based)
 *
 * This class encapsulates all MapLibre-specific operations and provides
 * a clean interface for the UI layer to interact with the map.
 *
 * Uses modern MapLibre style-layer rendering with GeoJsonSource + Layers.
 * Compatible with Jetpack Compose and MapLibre v10+.
 *
 * Follows Facade Pattern and Single Responsibility Principle.
 */
class MapLibreManager(private val context: Context) {

    private var mapLibreMap: MapLibreMap? = null
    private var currentStyle: Style? = null
    private var currentStylePreset: MapStyleConfig.StylePreset = MapStyleConfig.StylePreset.STREETS
    private var locationComponent: LocationComponent? = null
    private val stopMarkers = mutableListOf<Marker>() // Track stop markers for updates

    companion object {
        private var isInitialized = false

        // Source and layer IDs for markers and polylines
        private const val MARKER_SOURCE_ID = "marker-source"
        private const val MARKER_LAYER_ID = "marker-layer"
        private const val MARKER_LABEL_SOURCE_ID = "marker-label-source"
        private const val MARKER_LABEL_LAYER_ID = "marker-label-layer"
        private const val ROUTE_SOURCE_PREFIX = "route-source-"
        private const val ROUTE_LAYER_PREFIX = "route-layer-"
        private const val ROUTE_LABEL_SOURCE_PREFIX = "route-label-source-"
        private const val ROUTE_LABEL_LAYER_PREFIX = "route-label-layer-"
        private const val STOP_MARKERS_SOURCE_ID = "stop-markers-source"
        private const val STOP_MARKERS_LAYER_ID = "stop-markers-layer"
        private const val STOP_LABELS_SOURCE_ID = "stop-labels-source"
        private const val STOP_LABELS_LAYER_ID = "stop-labels-layer"

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
            } catch (e: Exception) {
                try {
                    map.setStyle(styleJson) { style ->
                        currentStyle = style
                        initializeMapLayers(style)
                        onStyleLoaded?.invoke(style)
                    }
                } catch (inner: Exception) {
                    android.util.Log.e("MapLibreManager", "Failed to load style: ${e.message}", inner)
                }
            }
        }
    }

    /**
     * Initialize GeoJSON sources and layers for markers and routes
     */
    private fun initializeMapLayers(style: Style) {
        try {
            // Add marker source and layer
            if (style.getSource(MARKER_SOURCE_ID) == null) {
                style.addSource(GeoJsonSource(MARKER_SOURCE_ID))
            }

            if (style.getLayer(MARKER_LAYER_ID) == null) {
                // Use CircleLayer for simple markers (can be replaced with SymbolLayer for custom icons)
                val markerLayer = CircleLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID)
                    .withProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleColor(Color.parseColor("#FF0000")),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(Color.WHITE)
                    )
                style.addLayer(markerLayer)
            }

            // Add marker label source & symbol layer for text labels (route labels)
            if (style.getSource(MARKER_LABEL_SOURCE_ID) == null) {
                style.addSource(GeoJsonSource(MARKER_LABEL_SOURCE_ID))
            }

            if (style.getLayer(MARKER_LABEL_LAYER_ID) == null) {
                val symbolLayer = SymbolLayer(MARKER_LABEL_LAYER_ID, MARKER_LABEL_SOURCE_ID)
                    .withProperties(
                        PropertyFactory.textField("{title}"),
                        PropertyFactory.textSize(14f),
                        PropertyFactory.textColor(Color.BLACK),
                        PropertyFactory.textHaloColor(Color.WHITE),
                        PropertyFactory.textHaloWidth(1.0f),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true)
                    )
                style.addLayer(symbolLayer)
            }

            // Note: Stop markers now use bitmap-based MarkerOptions instead of layers
            // See drawStopMarkers() method for implementation
        } catch (e: Exception) {
            android.util.Log.e("MapLibreManager", "Failed to initialize layers: ${e.message}", e)
        }
    }

    /**
     * Enable location layer to show blue puck for user position
     * Call this after location permission is granted
     */
    @SuppressLint("MissingPermission")
    fun enableLocationComponent(style: Style) {
        mapLibreMap?.let { map ->
            try {
                locationComponent = map.locationComponent.apply {
                    // Activate location component
                    activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style)
                            .useDefaultLocationEngine(true)
                            .build()
                    )

                    // Enable location component
                    isLocationComponentEnabled = true

                    // Set render mode to show blue puck
                    renderMode = RenderMode.COMPASS

                    // Set camera mode (can be TRACKING, NONE, etc.)
                    cameraMode = CameraMode.TRACKING
                }
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to enable location: ${e.message}", e)
            }
        }
    }

    /**
     * Disable location tracking (e.g., when permission is revoked)
     */
    @SuppressLint("MissingPermission")
    fun disableLocationComponent() {
        locationComponent?.isLocationComponentEnabled = false
        locationComponent = null
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
     * Animate camera to a location with custom tilt and bearing for 3D perspective
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param zoom Target zoom level
     * @param tilt Camera tilt angle (0 = overhead, 60 = perspective)
     * @param bearing Camera bearing angle (0 = north)
     * @param duration Animation duration in milliseconds
     */
    fun animateToWithTilt(
        latitude: Double,
        longitude: Double,
        zoom: Double = MapCameraConfig.ZOOM_STREET,
        tilt: Double = 0.0,
        bearing: Double = 0.0,
        duration: Int = MapCameraConfig.ANIMATION_DURATION_MEDIUM
    ) {
        val position = org.maplibre.android.camera.CameraPosition.Builder()
            .target(LatLng(latitude, longitude))
            .zoom(zoom)
            .tilt(tilt)
            .bearing(bearing)
            .build()

        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(position),
            duration
        )
    }

    /**
     * Move camera immediately to a location (no animation)
     */
    fun moveTo(
        latitude: Double,
        longitude: Double,
        zoom: Double = MapCameraConfig.ZOOM_STREET
    ) {
        val position = MapCameraConfig.createPosition(latitude, longitude, zoom)
        mapLibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    /**
     * Add a marker at a specific location using GeoJSON source and layer
     *
     * @param latitude Marker latitude
     * @param longitude Marker longitude
     * @param title Marker title (stored as property)
     */
    fun addMarker(
        latitude: Double,
        longitude: Double,
        title: String = ""
    ) {
        currentStyle?.let { style ->
            try {
                // Create GeoJSON point feature
                val point = Point.fromLngLat(longitude, latitude)
                val feature = Feature.fromGeometry(point).apply {
                    addStringProperty("title", title)
                }

                // Update source with new feature
                val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
                source?.setGeoJson(FeatureCollection.fromFeature(feature))
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to add marker: ${e.message}", e)
            }
        }
    }

    /**
     * Add multiple text labels (symbols) at specific locations. Used to label routes (A/B/C) on the map.
     */
    fun addRouteLabels(labels: List<Triple<String, Double, Double>>) {
        currentStyle?.let { style ->
            try {
                val features = labels.map { (title, lat, lon) ->
                    Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                        addStringProperty("title", title)
                    }
                }
                val source = style.getSourceAs<GeoJsonSource>(MARKER_LABEL_SOURCE_ID)
                source?.setGeoJson(FeatureCollection.fromFeatures(features))
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to add route labels: ${e.message}", e)
            }
        }
    }

    /**
     * Clear all markers from the map
     */
    fun clearMarkers() {
        currentStyle?.let { style ->
            try {
                val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
                source?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to clear markers: ${e.message}", e)
            }
        }
    }

    /**
     * Get current camera position
     */
    fun getCameraPosition(): LatLng? {
        return mapLibreMap?.cameraPosition?.target
    }

    /**
     * Set compass margins (pixels) to control the built-in compass position
     */
    fun setCompassMargins(left: Int, top: Int, right: Int, bottom: Int) {
        try {
            mapLibreMap?.uiSettings?.setCompassMargins(left, top, right, bottom)
        } catch (e: Exception) {
            android.util.Log.e("MapLibreManager", "Failed to set compass margins: ${e.message}", e)
        }
    }

    /**
     * Change map style
     */
    fun changeStyle(stylePreset: MapStyleConfig.StylePreset) {
        loadStyle(stylePreset)
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
            try {
                val sourceId = "$ROUTE_SOURCE_PREFIX$routeId"
                val layerId = "$ROUTE_LAYER_PREFIX$routeId"

                // Convert coordinates to GeoJSON Points
                val points = coordinates.map { (lat, lon) ->
                    Point.fromLngLat(lon, lat)
                }

                // Create LineString from points
                val lineString = LineString.fromLngLats(points)
                val feature = Feature.fromGeometry(lineString)

                // Remove existing source and layer if they exist
                style.getLayer(layerId)?.let { style.removeLayer(it) }
                style.getSource(sourceId)?.let { style.removeSource(it) }

                // Add new source
                style.addSource(GeoJsonSource(sourceId, FeatureCollection.fromFeature(feature)))

                // Determine styling (dotted)
                val lineColor = color ?: if (isPrimary) {
                    Color.parseColor("#2196F3") // Blue for primary
                } else {
                    Color.parseColor("#9E9E9E") // Gray for alternatives
                }
                val lineWidth = if (isPrimary) 5f else 4f
                val dashArray = if (isPrimary) arrayOf(2f, 2f) else arrayOf(1f, 2f)

                // Add line layer
                val lineLayer = LineLayer(layerId, sourceId)
                    .withProperties(
                        PropertyFactory.lineColor(lineColor),
                        PropertyFactory.lineWidth(lineWidth),
                        PropertyFactory.lineJoin("round"),
                        PropertyFactory.lineCap("round"),
                        PropertyFactory.lineDasharray(dashArray)
                    )
                style.addLayer(lineLayer)

            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to draw polyline: ${e.message}", e)
            }
        }
    }

    /**
     * Draw multiple route segments with distinct styling
     * First segment uses primary color, remaining segments use lighter/gray color
     * All segments are drawn as dotted lines
     *
     * @param routeId Base route identifier
     * @param segments List of coordinate segments
     */
    fun drawRouteSegments(
        routeId: String,
        segments: List<List<Pair<Double, Double>>>
    ) {
        currentStyle?.let { style ->
            try {
                segments.forEachIndexed { index, coordinates ->
                    val segmentId = "${routeId}_segment_${index}"
                    val sourceId = "$ROUTE_SOURCE_PREFIX$segmentId"
                    val layerId = "$ROUTE_LAYER_PREFIX$segmentId"

                    // Convert coordinates to GeoJSON Points
                    val points = coordinates.map { (lat, lon) ->
                        Point.fromLngLat(lon, lat)
                    }

                    // Create LineString from points
                    val lineString = LineString.fromLngLats(points)
                    val feature = Feature.fromGeometry(lineString)

                    // Remove existing source and layer if they exist
                    style.getLayer(layerId)?.let { style.removeLayer(it) }
                    style.getSource(sourceId)?.let { style.removeSource(it) }

                    // Add new source
                    style.addSource(GeoJsonSource(sourceId, FeatureCollection.fromFeature(feature)))

                    // First segment: distinct color, remaining: lighter/gray
                    val lineColor = if (index == 0) {
                        Color.parseColor("#2196F3") // Blue for first segment
                    } else {
                        Color.parseColor("#BDBDBD") // Light gray for remaining segments
                    }
                    val lineWidth = if (index == 0) 5f else 4f
                    val dashArray = arrayOf(2f, 2f) // Dotted for all segments

                    // Add line layer
                    val lineLayer = LineLayer(layerId, sourceId)
                        .withProperties(
                            PropertyFactory.lineColor(lineColor),
                            PropertyFactory.lineWidth(lineWidth),
                            PropertyFactory.lineJoin("round"),
                            PropertyFactory.lineCap("round"),
                            PropertyFactory.lineDasharray(dashArray)
                        )
                    style.addLayer(lineLayer)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to draw route segments: ${e.message}", e)
            }
        }
    }

    /**
     * Create a circular bitmap icon with a letter centered inside
     * @param label The letter to display (A, B, C, etc.)
     * @param size Size of the icon in pixels
     * @return Bitmap with circular background and centered letter
     */
    private fun createStopIconBitmap(label: String, size: Int = 96): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw white circle with blue border
        val circlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2196F3")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val radius = (size / 2f) - 3f
        val centerX = size / 2f
        val centerY = size / 2f

        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Draw letter centered
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = size * 0.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Calculate vertical center for text
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textY = centerY - textBounds.exactCenterY()

        canvas.drawText(label, centerX, textY, textPaint)

        return bitmap
    }

    /** Draw stop markers with labels A, B, C... using bitmap icons */
    fun drawStopMarkers(points: List<Triple<Double, Double, String>>) {
        mapLibreMap?.let { map ->
            try {
                android.util.Log.d("MapLibreManager", "drawStopMarkers called with ${points.size} points")

                // Clear existing stop markers
                stopMarkers.forEach { marker ->
                    map.removeMarker(marker)
                }
                stopMarkers.clear()

                val iconFactory = IconFactory.getInstance(context)

                // Create and add new markers
                points.forEachIndexed { idx, (lat, lon, label) ->
                    android.util.Log.d("MapLibreManager", "  Stop $idx: label='$label' at ($lat, $lon)")

                    // Create bitmap icon for this stop
                    val bitmap = createStopIconBitmap(label)
                    val icon = iconFactory.fromBitmap(bitmap)

                    // Create marker
                    val markerOptions = MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(lat, lon))
                        .icon(icon)
                        .title(label)

                    val marker = map.addMarker(markerOptions)
                    if (marker != null) {
                        stopMarkers.add(marker)
                        android.util.Log.d("MapLibreManager", "  Added marker for stop $label")
                    }
                }

                android.util.Log.d("MapLibreManager", "Total stop markers on map: ${stopMarkers.size}")
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to draw stop markers: ${e.message}", e)
            }
        }
    }

    fun clearStopMarkers() {
        mapLibreMap?.let { map ->
            try {
                android.util.Log.d("MapLibreManager", "Clearing ${stopMarkers.size} stop markers")
                stopMarkers.forEach { marker ->
                    map.removeMarker(marker)
                }
                stopMarkers.clear()
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to clear stop markers: ${e.message}", e)
            }
        }
    }

    /**
     * Clear all polylines from the map (sources & layers that start with ROUTE_ prefixes)
     */
    fun clearPolylines() {
        currentStyle?.let { style ->
            try {
                val layersToRemove = mutableListOf<String>()
                val sourcesToRemove = mutableListOf<String>()

                style.layers.forEach { layer ->
                    if (layer.id.startsWith(ROUTE_LAYER_PREFIX)) {
                        layersToRemove.add(layer.id)
                    }
                }

                style.sources.forEach { source ->
                    if (source.id.startsWith(ROUTE_SOURCE_PREFIX)) {
                        sourcesToRemove.add(source.id)
                    }
                }

                layersToRemove.forEach { layerId ->
                    style.getLayer(layerId)?.let { style.removeLayer(it) }
                }
                sourcesToRemove.forEach { sourceId ->
                    style.getSource(sourceId)?.let { style.removeSource(it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapLibreManager", "Failed to clear polylines: ${e.message}", e)
            }
        }
    }

    /**
     * Clean up resources
     */
    fun onDestroy() {
        clearMarkers()
        clearPolylines()
        clearStopMarkers()
        locationComponent = null
        currentStyle = null
        mapLibreMap = null
    }
}
