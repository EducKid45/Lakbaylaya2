package com.example.lakbaylaya.maplibre.markers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.example.lakbaylaya.R
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import kotlin.math.round
import androidx.core.graphics.scale

/**
 * Responsible for managing simple marker points rendered via a GeoJson source + SymbolLayer.
 * Keeps marker-specific logic out of the main MapLibreManager.
 *
 * Now accepts an optional Context so we can load a "default" marker drawable from resources
 * (falling back to the previously used programmatic bitmap if no drawable is available).
 */
class MarkerManager(private val context: Context, private val markerDp: Float = 72f) {
    private val markerSourceId = "marker-source"
    private val markerLayerId = "marker-layer"
    private val markerLabelSourceId = "marker-label-source"
    private val markerLabelLayerId = "marker-label-layer"

    // Image id used for the default marker icon added to the style
    private val defaultMarkerImageId = "marker-default-icon"

    /**
     * Add or replace the marker(s) source with a single-point feature.
     * If the source/layer do not exist, this will create them.
     */
    fun addMarker(style: Style, latitude: Double, longitude: Double, title: String = "") {
        try {
            // Ensure source exists (use correct markerSourceId)
            if (style.getSource(markerSourceId) == null) {
                style.addSource(GeoJsonSource(markerSourceId))
            }

            // Create point feature (title stored as property if needed by SymbolLayer)
            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                if (title.isNotBlank()) addStringProperty("title", title)
                addStringProperty("icon", defaultMarkerImageId)
            }

            // Update the source with the new feature
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeature(feature))

            // Ensure a SymbolLayer exists that shows our image for each feature
            if (style.getLayer(markerLayerId) == null) {
                val symbolLayer = SymbolLayer(markerLayerId, markerSourceId)
                    .withProperties(
                        PropertyFactory.iconImage("{icon}"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor("bottom"),
                        PropertyFactory.iconSize(1.0f)
                    )
                style.addLayer(symbolLayer)
            }
        } catch (e: Exception) {
            Log.e("MarkerManager", "Failed to add marker: ${e.message}", e)
        }
    }

    /**
     * Initialize marker-related sources, layers and default images on the provided Style.
     * Ensures the marker GeoJson source and SymbolLayer exist, and creates
     * a label SymbolLayer/source for titles if missing.
     */
    fun initialize(style: Style) {
        try {
            // Ensure source exists
            if (style.getSource(markerSourceId) == null) {
                style.addSource(
                    GeoJsonSource(
                        markerSourceId,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                )
            }

            // Add default marker image to the style if missing
            if (style.getImage(defaultMarkerImageId) == null) {
                // Try to load an existing drawable/mipmap marker from resources when a Context is available.
                // Load marker bitmap sized to `markerDp` (preserves aspect ratio)
                val bmp = try {
                    loadDefaultMarkerBitmap(context, markerDp)
                } catch (e: Exception) {
                    Log.w(
                        "MarkerManager",
                        "Failed to load default marker bitmap; using fallback",
                        e
                    )
                    // Fallback in the unlikely event loading fails; compute size from context density
                    createDefaultMarkerBitmap(size = (markerDp * context.resources.displayMetrics.density).toInt())
                }
                try {
                    style.addImage(defaultMarkerImageId, bmp)
                } catch (e: Exception) {
                    // some versions may throw if image exists concurrently; ignore
                    Log.w("MarkerManager", "Failed to add default marker image: ${e.message}")
                }
            }

            // Ensure symbol layer for markers exists
            if (style.getLayer(markerLayerId) == null) {
                val symbolLayer = SymbolLayer(markerLayerId, markerSourceId)
                    .withProperties(
                        PropertyFactory.iconImage("{icon}"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor("bottom"),
                        PropertyFactory.iconSize(1.0f)
                    )
                style.addLayer(symbolLayer)
            }

            // Create a text label source/layer for marker titles when needed
            if (style.getSource(markerLabelSourceId) == null) {
                style.addSource(
                    GeoJsonSource(
                        markerLabelSourceId,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                )
            }

            if (style.getLayer(markerLabelLayerId) == null) {
                val labelLayer = SymbolLayer(markerLabelLayerId, markerLabelSourceId)
                    .withProperties(
                        PropertyFactory.textField("{title}"),
                        PropertyFactory.textSize(14f),
                        PropertyFactory.textColor(Color.BLACK),
                        PropertyFactory.textHaloColor(Color.WHITE),
                        PropertyFactory.textHaloWidth(1.0f),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true)
                    )
                style.addLayer(labelLayer)
            }
        } catch (e: Exception) {
            Log.e("MarkerManager", "Failed to initialize markers: ${e.message}", e)
        }
    }

    /** Clear all markers (set empty feature collection on the source). */
    fun clearMarkers(style: Style) {
        try {
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            // Also clear labels
            val labelSrc = style.getSourceAs<GeoJsonSource>(markerLabelSourceId)
            labelSrc?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } catch (e: Exception) {
            Log.e("MarkerManager", "Failed to clear markers: ${e.message}", e)
        }
    }

    /**
     * Load marker bitmap strictly from the app's compile-time drawable resource
     * `R.drawable.maplibre_marker_icon_default`. This avoids resource reflection.
     */
    private fun loadDefaultMarkerBitmap(ctx: Context, dpSize: Float = 48f): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val targetSize = round(dpSize * density).toInt()

        try {
            // Use direct compile-time reference to avoid reflection and lint warnings
            val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(
                ctx.resources,
                R.drawable.maplibre_marker_icon_default,
                ctx.theme
            )
            if (drawable != null) return drawableToBitmapScaled(drawable, targetSize)
        } catch (e: Exception) {
            Log.w("MarkerManager", "Failed to load app marker drawable via R: ${e.message}")
        }

        // Fallback to the programmatic bitmap if resource not available
        return createDefaultMarkerBitmap(size = targetSize)
    }

    /**
     * Convert a Drawable to a Bitmap sized to `size` x `size` pixels while preserving aspect ratio
     * and centering the drawable within the canvas.
     */
    private fun drawableToBitmapScaled(drawable: Drawable, size: Int): Bitmap {
        // Create a square bitmap `size x size` and draw the drawable centered with preserved aspect ratio
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Get intrinsic dimensions from Drawable or Bitmap
        val intrinsicW = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.width
            else -> drawable.intrinsicWidth
        }.takeIf { it > 0 } ?: size

        val intrinsicH = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.height
            else -> drawable.intrinsicHeight
        }.takeIf { it > 0 } ?: size

        // Compute scale preserving aspect ratio
        val scale = kotlin.math.min(
            size.toFloat() / intrinsicW.toFloat(),
            size.toFloat() / intrinsicH.toFloat()
        )
        val dstW = (intrinsicW * scale).toInt()
        val dstH = (intrinsicH * scale).toInt()
        val left = (size - dstW) / 2
        val top = (size - dstH) / 2

        // If drawable is a BitmapDrawable we can draw its bitmap directly into the scaled rect
        if (drawable is BitmapDrawable) {
            val srcBmp = drawable.bitmap
            // use KTX extension Bitmap.scale to avoid deprecation/lint warning
            val scaled = srcBmp.scale(dstW, dstH)
            canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
            return bitmap
        }

        // Otherwise let the drawable draw itself into the bounds we set
        drawable.setBounds(left, top, left + dstW, top + dstH)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Create a simple default marker bitmap (pin-like) programmatically.
     */
    private fun createDefaultMarkerBitmap(size: Int = 96): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // draw outer circle (pin head)
        paint.color = "#E53935".toColorInt() // red-ish
        canvas.drawCircle(size * 0.5f, size * 0.30f, size * 0.22f, paint)

        // draw white inner circle
        paint.color = Color.WHITE
        canvas.drawCircle(size * 0.5f, size * 0.30f, size * 0.12f, paint)

        // draw pin tail as triangle
        paint.color = "#E53935".toColorInt()
        val path = Path().apply {
            moveTo(size * 0.36f, size * 0.46f)
            lineTo(size * 0.64f, size * 0.46f)
            lineTo(size * 0.5f, size * 0.96f)
            close()
        }
        canvas.drawPath(path, paint)

        return bitmap
    }

    /**
     * Clean up any marker-related resources (layers, sources, images) from the given style.
     * Called when the map/style is being destroyed or replaced.
     */
    fun onDestroy(style: Style?) {
        style?.let { s ->
            try {
                s.getLayer(markerLabelLayerId)?.let { s.removeLayer(it) }
            } catch (_: Exception) {
            }
            try {
                s.getSource(markerLabelSourceId)?.let { s.removeSource(it) }
            } catch (_: Exception) {
            }
            try {
                s.getLayer(markerLayerId)?.let { s.removeLayer(it) }
            } catch (_: Exception) {
            }
            try {
                s.getSource(markerSourceId)?.let { s.removeSource(it) }
            } catch (_: Exception) {
            }
            // remove image if we added it
            try {
                s.getImage(defaultMarkerImageId)?.let { s.removeImage(defaultMarkerImageId) }
            } catch (_: Exception) {
            }
        }
    }
}
