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
import java.util.UUID

/**
 * MarkerManager
 * - Uses a provided drawable `R.drawable.maplibre_marker_icon_green` as the default preview icon.
 * - Keeps three in-memory lists: persistent, temporary, preview.
 * - Provides atomic promotion of preview/temporary -> persistent to avoid flicker.
 */
class MarkerManager(private val context: Context, private val markerDp: Float = 48f) {
    private val markerSourceId = "marker-source"
    private val markerLayerId = "marker-layer"
    private val markerLabelSourceId = "marker-label-source"
    private val markerLabelLayerId = "marker-label-layer"

    private val defaultMarkerImageId = "marker-default-icon"
    private val persistentMarkerImageId = "marker-default-icon-green"

    private val persistentMarkerFeatures = mutableListOf<Feature>()
    private val temporaryMarkerFeatures = mutableListOf<Feature>()
    private val previewMarkerFeatures = mutableListOf<Feature>()

    /** Ensure the persistent (green) image is loaded into the style. */
    private fun ensurePersistentImageLoaded(style: Style) {
        if (style.getImage(persistentMarkerImageId) == null) {
            Log.d("MarkerManager", "Loading persistent image into style: $persistentMarkerImageId")
            try {
                val bmp = loadMarkerBitmap(context, markerDp, R.drawable.maplibre_marker_icon_green)
                style.addImage(persistentMarkerImageId, bmp)
                Log.d("MarkerManager", "Persistent image added: $persistentMarkerImageId")
            } catch (e: Exception) {
                Log.w("MarkerManager", "Failed to load persistent image: ${e.message}")
                try {
                    val bmp = createDefaultMarkerBitmap(
                        (markerDp * context.resources.displayMetrics.density).toInt(),
                        "#4CAF50"
                    )
                    style.addImage(persistentMarkerImageId, bmp)
                    Log.d(
                        "MarkerManager",
                        "Persistent fallback image added: $persistentMarkerImageId"
                    )
                } catch (inner: Exception) {
                    Log.w(
                        "MarkerManager",
                        "Fallback failed to add persistent image: ${inner.message}"
                    )
                }
            }
        }
    }

    /** Ensure the default (red) image is loaded into the style. */
    private fun ensureDefaultImageLoaded(style: Style) {
        if (style.getImage(defaultMarkerImageId) == null) {
            try {
                val bmp =
                    loadMarkerBitmap(context, markerDp, R.drawable.maplibre_marker_icon_default)
                style.addImage(defaultMarkerImageId, bmp)
            } catch (e: Exception) {
                Log.w("MarkerManager", "Failed to load default image: ${e.message}")
                try {
                    val bmp =
                        createDefaultMarkerBitmap((markerDp * context.resources.displayMetrics.density).toInt())
                    style.addImage(defaultMarkerImageId, bmp)
                } catch (inner: Exception) {
                    Log.w("MarkerManager", "Fallback failed to add default image: ${inner.message}")
                }
            }
        }
    }

    /** Add a marker. If persistent=true it stays in persistent list. */
    fun addMarker(
        style: Style,
        latitude: Double,
        longitude: Double,
        title: String = "",
        persistent: Boolean = false
    ) {
        try {
            if (style.getSource(markerSourceId) == null) style.addSource(
                GeoJsonSource(
                    markerSourceId
                )
            )

            // If this is a persistent marker, ensure the green image is loaded first to avoid races
            if (persistent) {
                ensurePersistentImageLoaded(style)
            } else {
                // ensure default red is loaded so temporary markers get explicit icon
                ensureDefaultImageLoaded(style)
            }

            // helper to check proximity
            fun isNearExisting(f: Feature, lat: Double, lng: Double, tol: Double = 1e-5): Boolean {
                val g = f.geometry()
                if (g is Point) {
                    return kotlin.math.abs(g.latitude() - lat) <= tol && kotlin.math.abs(g.longitude() - lng) <= tol
                }
                return false
            }

            // If adding a temporary marker but there is already a persistent marker nearby, skip to avoid duplicate red->green flicker
            if (!persistent) {
                val exists =
                    persistentMarkerFeatures.any { isNearExisting(it, latitude, longitude, 1e-4) }
                if (exists) {
                    Log.d(
                        "MarkerManager",
                        "Skipping temporary marker at $latitude,$longitude because persistent marker exists nearby"
                    )
                    return
                }
            }

            // Remove any preview marker at the same location (we'll re-add below or keep preview separate)
            previewMarkerFeatures.removeAll { isNearExisting(it, latitude, longitude, 1e-4) }

            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                if (title.isNotBlank()) addStringProperty("title", title)
                // Now that images are ensured, set the icon property directly
                if (persistent) {
                    addStringProperty("icon", persistentMarkerImageId)
                    Log.d(
                        "MarkerManager",
                        "Adding persistent feature at $latitude,$longitude with icon $persistentMarkerImageId"
                    )
                } else {
                    addStringProperty("icon", defaultMarkerImageId)
                    Log.d(
                        "MarkerManager",
                        "Adding temporary feature at $latitude,$longitude with icon $defaultMarkerImageId"
                    )
                }
            }

            if (persistent) persistentMarkerFeatures.add(feature) else temporaryMarkerFeatures.add(
                feature
            )

            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
        } catch (e: Exception) {
            Log.e("MarkerManager", "addMarker failed: ${e.message}", e)
        }
    }

    /** Add a preview marker (used during editing). Default preview icon is the green drawable per user's request. */
    fun addPreviewMarker(style: Style, latitude: Double, longitude: Double, title: String = "") {
        try {
            if (style.getSource(markerSourceId) == null) style.addSource(
                GeoJsonSource(
                    markerSourceId
                )
            )
            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                if (title.isNotBlank()) addStringProperty("title", title)
                // only attach the default (red) icon if the style already contains it; otherwise leave unset
                if (style.getImage(defaultMarkerImageId) != null) addStringProperty(
                    "icon",
                    defaultMarkerImageId
                )
            }
            previewMarkerFeatures.add(feature)
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
        } catch (e: Exception) {
            Log.e("MarkerManager", "addPreviewMarker failed: ${e.message}", e)
        }
    }

    /** Add a preview marker (used during editing). Returns a previewId for exact promotion. */
    fun addPreviewMarkerWithId(
        style: Style,
        latitude: Double,
        longitude: Double,
        title: String = ""
    ): String {
        try {
            if (style.getSource(markerSourceId) == null) style.addSource(
                GeoJsonSource(
                    markerSourceId
                )
            )

            // ensure default icon exists for preview (best-effort)
            if (style.getImage(defaultMarkerImageId) == null) ensureDefaultImageLoaded(style)

            val previewId = UUID.randomUUID().toString()
            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                addStringProperty("previewId", previewId)
                if (title.isNotBlank()) addStringProperty("title", title)
                if (style.getImage(defaultMarkerImageId) != null) addStringProperty(
                    "icon",
                    defaultMarkerImageId
                )
            }
            previewMarkerFeatures.add(feature)
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
            Log.d("MarkerManager", "Added preview marker id=$previewId at $latitude,$longitude")
            return previewId
        } catch (e: Exception) {
            Log.e("MarkerManager", "addPreviewMarkerWithId failed: ${e.message}", e)
            return ""
        }
    }

    /** Clear preview markers only. */
    fun clearPreviewMarkers(style: Style) {
        try {
            previewMarkerFeatures.clear()
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
        } catch (e: Exception) {
            Log.e("MarkerManager", "clearPreviewMarkers failed: ${e.message}", e)
        }
    }

    /** Clear temporary (non-persistent) markers. */
    fun clearMarkers(style: Style) {
        try {
            temporaryMarkerFeatures.clear()
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
            val labelSrc = style.getSourceAs<GeoJsonSource>(markerLabelSourceId)
            labelSrc?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } catch (e: Exception) {
            Log.e("MarkerManager", "clearMarkers failed: ${e.message}", e)
        }
    }

    /** Clear everything. */
    fun clearAllMarkers(style: Style) {
        try {
            persistentMarkerFeatures.clear()
            temporaryMarkerFeatures.clear()
            previewMarkerFeatures.clear()
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            val labelSrc = style.getSourceAs<GeoJsonSource>(markerLabelSourceId)
            labelSrc?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } catch (e: Exception) {
            Log.e("MarkerManager", "clearAllMarkers failed: ${e.message}", e)
        }
    }

    /** Atomically promote a nearby preview/temp marker to persistent (changes icon to green) to avoid flicker. */
    fun promoteToPersistent(style: Style, latitude: Double, longitude: Double, title: String = "") {
        try {
            val tol = 1e-5
            fun isNear(f: Feature, t: Double = tol): Boolean {
                val g = f.geometry()
                if (g is Point) {
                    val lat = g.latitude()
                    val lng = g.longitude()
                    return kotlin.math.abs(lat - latitude) <= t && kotlin.math.abs(lng - longitude) <= t
                }
                return false
            }

            Log.d("MarkerManager", "Promoting marker to persistent at $latitude,$longitude")

            // Remove any nearby preview/temporary features first (use slightly larger tolerance to avoid duplicates)
            val removalTol = 1e-4
            val removedPreview = previewMarkerFeatures.removeAll { isNear(it, removalTol) }
            val removedTemp = temporaryMarkerFeatures.removeAll { isNear(it, removalTol) }
            val removedPersistent = persistentMarkerFeatures.removeAll { isNear(it, removalTol) }
            if (removedPreview || removedTemp || removedPersistent) {
                Log.d(
                    "MarkerManager",
                    "Removed nearby features (preview=$removedPreview,temp=$removedTemp,persistent=$removedPersistent)"
                )
            }

            // Ensure the persistent image exists before creating the persistent feature
            ensurePersistentImageLoaded(style)

            // Create and add the persistent feature (single atomic update)
            val point = Point.fromLngLat(longitude, latitude)
            val feature = Feature.fromGeometry(point).apply {
                if (title.isNotBlank()) addStringProperty("title", title)
                addStringProperty("icon", persistentMarkerImageId)
                Log.d(
                    "MarkerManager",
                    "Created persistent feature with icon $persistentMarkerImageId"
                )
            }
            persistentMarkerFeatures.add(feature)

            // Update the source once so the change is atomic (no intermediate state where red is present)
            val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
            src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
        } catch (e: Exception) {
            Log.e("MarkerManager", "promoteToPersistent failed: ${e.message}", e)
        }
    }

    /** Promote a preview marker by its previewId to persistent (green). Returns true if promoted. */
    fun promotePreviewToPersistentById(
        style: Style,
        previewId: String,
        title: String = ""
    ): Boolean {
        try {
            val idx = previewMarkerFeatures.indexOfFirst {
                try {
                    it.getStringProperty("previewId") == previewId
                } catch (_: Exception) {
                    false
                }
            }
            if (idx >= 0) {
                val feat = previewMarkerFeatures.removeAt(idx)

                // Extract coordinates from preview feature
                val geom = feat.geometry()
                var lat = Double.NaN
                var lng = Double.NaN
                if (geom is Point) {
                    lat = geom.latitude()
                    lng = geom.longitude()
                }

                if (title.isNotBlank()) feat.addStringProperty("title", title)
                // ensure persistent image present
                ensurePersistentImageLoaded(style)
                feat.addStringProperty("icon", persistentMarkerImageId)
                // remove previewId prop
                try {
                    feat.removeProperty("previewId")
                } catch (_: Exception) {
                }

                // Remove any temporary/persistent features nearby to avoid duplicates
                if (!lat.isNaN() && !lng.isNaN()) {
                    val tol = 1e-4
                    fun isNearCoord(f: Feature): Boolean {
                        val g = f.geometry()
                        if (g is Point) {
                            return kotlin.math.abs(g.latitude() - lat) <= tol && kotlin.math.abs(g.longitude() - lng) <= tol
                        }
                        return false
                    }

                    val removedTemp = temporaryMarkerFeatures.removeAll { isNearCoord(it) }
                    val removedPersistent = persistentMarkerFeatures.removeAll { isNearCoord(it) }
                    if (removedTemp || removedPersistent) {
                        Log.d(
                            "MarkerManager",
                            "Removed nearby temp/persistent features during id-promotion (temp=$removedTemp,persistent=$removedPersistent)"
                        )
                    }
                }

                persistentMarkerFeatures.add(feat)
                val src = style.getSourceAs<GeoJsonSource>(markerSourceId)
                src?.setGeoJson(FeatureCollection.fromFeatures(persistentMarkerFeatures + temporaryMarkerFeatures + previewMarkerFeatures))
                Log.d("MarkerManager", "Promoted preview id=$previewId to persistent")
                return true
            }
        } catch (e: Exception) {
            Log.e("MarkerManager", "promotePreviewToPersistentById failed: ${e.message}", e)
        }
        return false
    }

    /** Initialize sources, layers and images on the style. Uses user-provided green drawable as default. */
    fun initialize(style: Style) {
        try {
            if (style.getSource(markerSourceId) == null) {
                style.addSource(
                    GeoJsonSource(
                        markerSourceId,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                )
            }

            // Load default (red) preview icon from resources. Fallback to programmatic if missing.
            if (style.getImage(defaultMarkerImageId) == null) {
                val bmp = try {
                    loadMarkerBitmap(context, markerDp, R.drawable.maplibre_marker_icon_default)
                } catch (e: Exception) {
                    Log.w("MarkerManager", "loadMarkerBitmap(default) failed: ${e.message}")
                    createDefaultMarkerBitmap((markerDp * context.resources.displayMetrics.density).toInt())
                }
                try {
                    style.addImage(defaultMarkerImageId, bmp)
                } catch (_: Exception) {
                }
            }

            // Load persistent (green) icon from resources. Fallback to programmatic green if missing.
            if (style.getImage(persistentMarkerImageId) == null) {
                val bmp = try {
                    loadMarkerBitmap(context, markerDp, R.drawable.maplibre_marker_icon_green)
                } catch (e: Exception) {
                    Log.w("MarkerManager", "loadMarkerBitmap(persistent) failed: ${e.message}")
                    createDefaultMarkerBitmap(
                        (markerDp * context.resources.displayMetrics.density).toInt(),
                        "#4CAF50"
                    )
                }
                try {
                    style.addImage(persistentMarkerImageId, bmp)
                } catch (_: Exception) {
                }
            }

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

            if (style.getSource(markerLabelSourceId) == null) style.addSource(
                GeoJsonSource(
                    markerLabelSourceId,
                    FeatureCollection.fromFeatures(emptyList())
                )
            )
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
            Log.e("MarkerManager", "initialize failed: ${e.message}", e)
        }
    }

    /** Public API: preload marker images into the provided style (ensures red and green images are registered). */
    fun preloadImages(style: Style) {
        try {
            ensureDefaultImageLoaded(style)
            ensurePersistentImageLoaded(style)
        } catch (e: Exception) {
            Log.w("MarkerManager", "preloadImages failed: ${e.message}")
        }
    }

    /** Load user-supplied green drawable as bitmap (fallback to programmatic). */
    private fun loadMarkerBitmap(
        ctx: Context,
        dpSize: Float = 48f,
        drawableResId: Int? = null
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val targetSize = round(dpSize * density).toInt()
        val drawable = drawableResId?.let {
            androidx.core.content.res.ResourcesCompat.getDrawable(
                ctx.resources,
                it,
                ctx.theme
            )
        }
        return if (drawable != null) drawableToBitmapScaled(
            drawable,
            targetSize
        ) else createDefaultMarkerBitmap(targetSize)
    }

    private fun drawableToBitmapScaled(drawable: Drawable, size: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val intrinsicW = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.width
            else -> drawable.intrinsicWidth
        }.takeIf { it > 0 } ?: size
        val intrinsicH = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.height
            else -> drawable.intrinsicHeight
        }.takeIf { it > 0 } ?: size
        val scale = kotlin.math.min(
            size.toFloat() / intrinsicW.toFloat(),
            size.toFloat() / intrinsicH.toFloat()
        )
        val dstW = (intrinsicW * scale).toInt()
        val dstH = (intrinsicH * scale).toInt()
        val left = (size - dstW) / 2
        val top = (size - dstH) / 2
        if (drawable is BitmapDrawable) {
            val scaled = drawable.bitmap.scale(dstW, dstH)
            canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
            return bitmap
        }
        drawable.setBounds(left, top, left + dstW, top + dstH)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createDefaultMarkerBitmap(size: Int = 96, colorHex: String = "#E53935"): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = colorHex.toColorInt()
        canvas.drawCircle(size * 0.5f, size * 0.30f, size * 0.22f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(size * 0.5f, size * 0.30f, size * 0.12f, paint)
        paint.color = colorHex.toColorInt()
        val path = Path().apply {
            moveTo(size * 0.36f, size * 0.46f)
            lineTo(size * 0.64f, size * 0.46f)
            lineTo(size * 0.5f, size * 0.96f)
            close()
        }
        canvas.drawPath(path, paint)
        return bitmap
    }

    /** Clean up images/layers/sources added by manager. */
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
            try {
                s.getImage(defaultMarkerImageId)?.let { s.removeImage(defaultMarkerImageId) }
            } catch (_: Exception) {
            }
            try {
                s.getImage(persistentMarkerImageId)?.let { s.removeImage(persistentMarkerImageId) }
            } catch (_: Exception) {
            }
        }
    }
}
