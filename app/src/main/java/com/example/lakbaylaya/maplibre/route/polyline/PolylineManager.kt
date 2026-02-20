package com.example.lakbaylaya.maplibre.route.polyline

import android.util.Log
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import androidx.core.graphics.toColorInt

/**
 * Manages drawing and clearing of route polylines on a map Style.
 *
 * Usage: call drawPolyline(style, ...) after the Style is loaded. Use clearPolylines(style)
 * to remove previously added route layers/sources.
 */
class PolylineManager {

    private val routeSourcePrefix = "route-source-"
    private val routeLayerPrefix = "route-layer-"

    /** Create minimal placeholders if desired; currently no global source/layer required. */
    fun initialize() {
        try {
            // no-op: polylines are created per-route id
        } catch (e: Exception) {
            Log.w("PolylineManager", "Initialize noop failed: ${e.message}")
        }
    }

    /**
     * Draw a polyline for the given routeId using GeoJSON LineString + LineLayer.
     * @param style active map Style
     * @param routeId unique id for this route (used to create source/layer ids)
     * @param coordinates list of (lat, lon) coordinate pairs
     * @param isPrimary if true use primary styling, otherwise alternative styling
     * @param color optional color (Int). If null, default colors are applied.
     */
    fun drawPolyline(
        style: Style,
        routeId: String,
        coordinates: List<Pair<Double, Double>>,
        isPrimary: Boolean,
        color: Int? = null
    ) {
        try {
            val sourceId = "$routeSourcePrefix$routeId"
            val layerId = "$routeLayerPrefix$routeId"

            // Convert coordinates to GeoJSON Points (MapLibre uses lon, lat ordering)
            val points = coordinates.map { (lat, lon) ->
                Point.fromLngLat(lon, lat)
            }

            val lineString = LineString.fromLngLats(points)
            val feature = Feature.fromGeometry(lineString)

            // Remove existing source/layer if present
            style.getLayer(layerId)?.let { style.removeLayer(it) }
            style.getSource(sourceId)?.let { style.removeSource(it) }

            // Add new source
            style.addSource(GeoJsonSource(sourceId, FeatureCollection.fromFeature(feature)))

            // Determine styling
            val lineColor = color ?: if (isPrimary) {
                "#2196F3".toColorInt()
            } else {
                "#3d94d7".toColorInt()
            }
            val lineWidth = if (isPrimary) 5f else 4f
            val dashArray = if (isPrimary) arrayOf(2f, 2f) else arrayOf(1f, 2f)

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
            Log.e("PolylineManager", "Failed to draw polyline: ${e.message}", e)
        }
    }

    /**
     * Remove all polylines and sources whose IDs start with the route prefixes.
     */
    fun clearPolylines(style: Style) {
        try {
            val layersToRemove = mutableListOf<String>()
            val sourcesToRemove = mutableListOf<String>()

            style.layers.forEach { layer ->
                if (layer.id.startsWith(routeLayerPrefix)) {
                    layersToRemove.add(layer.id)
                }
            }

            style.sources.forEach { source ->
                if (source.id.startsWith(routeSourcePrefix)) {
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
            Log.e("PolylineManager", "Failed to clear polylines: ${e.message}", e)
        }
    }

    /**
     * Clean up any resources added by the PolylineManager (layers & sources).
     */
    fun onDestroy(style: Style?) {
        style?.let { s ->
            try {
                clearPolylines(s)
            } catch (_: Exception) {
            }
        }
    }
}
