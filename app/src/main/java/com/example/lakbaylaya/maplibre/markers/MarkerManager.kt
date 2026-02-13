package com.example.lakbaylaya.maplibre.markers

import android.util.Log
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import android.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Responsible for managing simple marker points rendered via a GeoJson source + CircleLayer.
 * Keeps marker-specific logic out of the main MapLibreManager.
 */
class MarkerManager {
    private val markerSourceId = "marker-source"
    private val markerLayerId = "marker-layer"

    /**
     * Add or replace the marker(s) source with a single-point feature.
     * If the source/layer do not exist, this will create them.
     */
    fun addMarker(style: Style, latitude: Double, longitude: Double, title: String = "") {
        try {
            // Ensure source exists
            if (style.getSource(markerSourceId) == null) {
                style.addSource(GeoJsonSource(markerLayerId))
            }

            // Create point feature (title stored as property if needed by SymbolLayer)
            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                if (title.isNotBlank()) addStringProperty("title", title)
            }

            // Update the source with the new feature
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeature(feature))

            // Ensure a simple CircleLayer exists for the source so it will render if not already added.
            if (style.getLayer(markerLayerId) == null) {
                val layer = CircleLayer(markerLayerId, markerSourceId)
                    .withProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleColor("#FF0000".toColorInt()),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(Color.WHITE)
                    )
                style.addLayer(layer)
            }
        } catch (e: Exception) {
            Log.e("MarkerManager", "Failed to add marker: ${e.message}", e)
        }
    }

    /** Clear all markers (set empty feature collection on the source). */
    fun clearMarkers(style: Style) {
        try {
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } catch (e: Exception) {
            Log.e("MarkerManager", "Failed to clear markers: ${e.message}", e)
        }
    }
}

