package com.example.lakbaylaya.maplibre.markers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

/**
 * WHAT it does: Manages stop markers (A, B, C, ...) by creating bitmap icons,
 * adding them as images to the map Style, and rendering points with a SymbolLayer.
 * WHY it exists: Avoids deprecated annotation-based Marker APIs and uses the
 * style/layer system which is more efficient for lots of markers and aligns
 * with modern MapLibre/Mapbox usage.
 * WHEN it is used: Called after the map Style is loaded to show/hide stop icons.
 */
class StopMarkerManager {

    private val sourceId = "stop-marker-source"
    private val layerId = "stop-marker-layer"

    // Track image ids we added so we can remove them on cleanup
    private val addedImageIds = mutableListOf<String>()

    /**
     * Initialize minimal source/layer placeholders for stops so other components can rely on them.
     */
    fun initialize(style: Style) {
        try {
            if (style.getSource(sourceId) == null) {
                style.addSource(
                    GeoJsonSource(
                        sourceId,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                )
            }
            if (style.getLayer(layerId) == null) {
                val symbolLayer = SymbolLayer(layerId, sourceId)
                    .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                    )
                style.addLayer(symbolLayer)
            }
        } catch (e: Exception) {
            Log.w(
                "StopMarkerManager",
                "Failed to initialize stop marker layer/source: ${e.message}"
            )
        }
    }

    /**
     * Draw stop markers using the provided Style. Each point becomes a Feature with
     * an "icon" property referencing an image added to the Style.
     * @param style map Style (must be non-null and loaded)
     * @param points list of Triple(lat, lon, label)
     */
    fun drawStopMarkers(style: Style, points: List<Triple<Double, Double, String>>) {
        try {
            Log.d("StopMarkerManager", "drawStopMarkers called with ${points.size} points")

            // Remove previous layer/source/images if present
            try {
                style.getLayer(layerId)?.let { style.removeLayer(it) }
            } catch (_: Exception) {
            }
            try {
                style.getSource(sourceId)?.let { style.removeSource(it) }
            } catch (_: Exception) {
            }
            // Remove previously added images
            addedImageIds.forEach { id ->
                try {
                    style.getImage(id)?.let { style.removeImage(id) }
                } catch (_: Exception) {
                }
            }
            addedImageIds.clear()

            if (points.isEmpty()) return

            // Create a bitmap image per point and a corresponding GeoJSON Feature
            val features = points.mapIndexed { idx, triple ->
                val (lat, lon, label) = triple
                val imageId = "stop-icon-$idx-${label.replace("\\s+".toRegex(), "-").lowercase()}"

                // create bitmap and add it as a style image
                val bitmap = createStopIconBitmap(label)
                try {
                    style.addImage(imageId, bitmap)
                    addedImageIds.add(imageId)
                } catch (e: Exception) {
                    Log.w(
                        "StopMarkerManager",
                        "Failed to add image '$imageId' to style: ${e.message}"
                    )
                }

                Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                    addStringProperty("icon", imageId)
                    addStringProperty("title", label)
                }
            }

            // Add GeoJSON source
            val collection = FeatureCollection.fromFeatures(features)
            style.addSource(GeoJsonSource(sourceId, collection))

            // Add SymbolLayer that uses the icon property from each feature
            val symbolLayer = SymbolLayer(layerId, sourceId)
                .withProperties(
                    PropertyFactory.iconImage("{icon}"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )

            style.addLayer(symbolLayer)

            Log.d(
                "StopMarkerManager",
                "Added ${features.size} stop features and ${addedImageIds.size} images"
            )
        } catch (e: Exception) {
            Log.e("StopMarkerManager", "Failed to draw stop markers: ${e.message}", e)
        }
    }

    /**
     * Clear stop markers by removing the layer, source and any images we added.
     */
    fun clearStopMarkers(style: Style) {
        try {
            Log.d(
                "StopMarkerManager",
                "Clearing ${addedImageIds.size} stop images and source/layer"
            )
            try {
                style.getLayer(layerId)?.let { style.removeLayer(it) }
            } catch (_: Exception) {
            }
            try {
                style.getSource(sourceId)?.let { style.removeSource(it) }
            } catch (_: Exception) {
            }
            addedImageIds.forEach { id ->
                try {
                    style.getImage(id)?.let { style.removeImage(id) }
                } catch (_: Exception) {
                }
            }
            addedImageIds.clear()
        } catch (e: Exception) {
            Log.e("StopMarkerManager", "Failed to clear stop markers: ${e.message}", e)
        }
    }

    /**
     * Create a circular bitmap icon with a letter or short label centered inside.
     */
    private fun createStopIconBitmap(label: String, size: Int = 96): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Draw white circle with blue border
        val circlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = "#2196F3".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val radius = (size / 2f) - 3f
        val centerX = size / 2f
        val centerY = size / 2f

        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Draw letter centered (trim to 2 chars to avoid overflow)
        val display = if (label.length <= 2) label else label.take(2)
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = size * 0.45f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(display, 0, display.length, textBounds)
        val textY = centerY - textBounds.exactCenterY()

        canvas.drawText(display, centerX, textY, textPaint)

        return bitmap
    }

    /**
     * Clean up any resources held by the manager.
     */
    fun onDestroy(style: Style?) {
        style?.let { s ->
            try {
                s.getLayer(layerId)?.let { s.removeLayer(it) }
            } catch (_: Exception) {
            }
            try {
                s.getSource(sourceId)?.let { s.removeSource(it) }
            } catch (_: Exception) {
            }
            addedImageIds.forEach { id ->
                try {
                    s.getImage(id)?.let { s.removeImage(id) }
                } catch (_: Exception) {
                }
            }
        }
        addedImageIds.clear()
    }
}
