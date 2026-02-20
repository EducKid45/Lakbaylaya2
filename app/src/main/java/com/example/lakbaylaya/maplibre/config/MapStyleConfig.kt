package com.example.lakbaylaya.maplibre.config

import org.json.JSONArray
import org.json.JSONObject

/**
 * Configuration for MapLibre map styles and tile sources
 *
 * This class provides JSON-based style configurations for MapLibre GL.
 * It follows the MapLibre Style Specification:
 * https://maplibre.org/maplibre-style-spec/
 */
object MapStyleConfig {

    /** Map style presets */
    enum class StylePreset(val displayName: String) {
        STREETS("Streets")
    }

    /**
     * Get MapLibre style JSON for the specified preset
     * @param preset The style preset to use
     * @param apiKey Geoapify API key for tile sources
     * @return JSON string conforming to MapLibre Style Specification
     */
    fun getStyleJson(preset: StylePreset, apiKey: String): String {
        val tileUrl = when (preset) {
            StylePreset.STREETS -> "https://maps.geoapify.com/v1/tile/osm-carto/{z}/{x}/{y}.png?apiKey=$apiKey"
        }
        return buildMapStyle(preset, tileUrl)
    }

    /**
     * Build a complete MapLibre style JSON using the preset's display name.
     * @param preset Style preset (provides display name)
     * @param tileUrl Tile source URL template
     * @return Style JSON string
     */
    private fun buildMapStyle(preset: StylePreset, tileUrl: String): String {
        val name = preset.displayName
        val styleJson = JSONObject().apply {
            put("version", 8)
            put("name", name)

            // Sources configuration
            put("sources", JSONObject().apply {
                put("geoapify", JSONObject().apply {
                    put("type", "raster")
                    put("tiles", JSONArray().apply {
                        put(tileUrl)
                    })
                    put("tileSize", 256)
                    put(
                        "attribution",
                        "© <a href='https://www.geoapify.com/'>Geoapify</a> | © <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors"
                    )
                    put("minzoom", 0)
                    put("maxzoom", 18)
                })
            })

            // Layers configuration
            put("layers", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "geoapify-layer")
                    put("type", "raster")
                    put("source", "geoapify")
                    put("minzoom", 0)
                    put("maxzoom", 22)
                })
            })
        }
        return styleJson.toString()
    }
}