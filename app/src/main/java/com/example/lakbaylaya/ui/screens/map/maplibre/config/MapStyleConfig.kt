package com.example.lakbaylaya.ui.screens.map.maplibre.config

import org.json.JSONObject

/**
 * Configuration for MapLibre map styles and tile sources
 *
 * This class provides JSON-based style configurations for MapLibre GL.
 * It follows the MapLibre Style Specification:
 * https://maplibre.org/maplibre-style-spec/
 *
 * Follows Single Responsibility Principle and Strategy Pattern.
 */
object MapStyleConfig {

    /**
     * Available map style presets
     */
    enum class StylePreset {
        STREETS,
        SATELLITE,
        OUTDOOR,
        DARK,
        LIGHT
    }

    /**
     * Get MapLibre style JSON for the specified preset
     *
     * @param preset The style preset to use
     * @param apiKey Geoapify API key for tile sources
     * @return JSON string conforming to MapLibre Style Specification
     */
    fun getStyleJson(preset: StylePreset, apiKey: String): String {
        // Use a single, consistent tile style (OSM-Carto) across the app.
        // Dark/light/outdoor presets are intentionally ignored to keep visual consistency.
        return getStreetsStyle(apiKey)
    }

    /**
     * Streets style - default street map with labels
     */
    private fun getStreetsStyle(apiKey: String): String {
        return buildMapStyle(
            name = "Streets",
            tileUrl = "https://maps.geoapify.com/v1/tile/osm-carto/{z}/{x}/{y}.png?apiKey=$apiKey"
        )
    }

    /**
     * Satellite style - aerial imagery
     */
    private fun getSatelliteStyle(apiKey: String): String {
        return buildMapStyle(
            name = "Satellite",
            tileUrl = "https://maps.geoapify.com/v1/tile/satellite/{z}/{x}/{y}.jpg?apiKey=$apiKey"
        )
    }

    /**
     * Outdoor style - topographic map for hiking/outdoor activities
     */
    private fun getOutdoorStyle(apiKey: String): String {
        return buildMapStyle(
            name = "Outdoor",
            tileUrl = "https://maps.geoapify.com/v1/tile/osm-bright/{z}/{x}/{y}.png?apiKey=$apiKey"
        )
    }

    /**
     * Dark style - dark theme for night mode
     */
    private fun getDarkStyle(apiKey: String): String {
        return buildMapStyle(
            name = "Dark",
            tileUrl = "https://maps.geoapify.com/v1/tile/dark-matter-yellow-roads/{z}/{x}/{y}.png?apiKey=$apiKey"
        )
    }

    /**
     * Light style - minimal light theme
     */
    private fun getLightStyle(apiKey: String): String {
        return buildMapStyle(
            name = "Light",
            tileUrl = "https://maps.geoapify.com/v1/tile/positron/{z}/{x}/{y}.png?apiKey=$apiKey"
        )
    }

    /**
     * Build a complete MapLibre style JSON
     *
     * @param name Style name
     * @param tileUrl Tile source URL template
     * @return JSON string conforming to MapLibre Style Specification
     */
    private fun buildMapStyle(name: String, tileUrl: String): String {
        val styleJson = JSONObject().apply {
            put("version", 8)
            put("name", name)

            // Sources configuration
            put("sources", JSONObject().apply {
                put("geoapify", JSONObject().apply {
                    put("type", "raster")
                    put("tiles", org.json.JSONArray().apply {
                        put(tileUrl)
                    })
                    put("tileSize", 256)
                    put("attribution", "© <a href='https://www.geoapify.com/'>Geoapify</a> | © <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors")
                    put("minzoom", 0)
                    put("maxzoom", 18)
                })
            })

            // Layers configuration
            put("layers", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "geoapify-layer")
                    put("type", "raster")
                    put("source", "geoapify")
                    put("minzoom", 0)
                    put("maxzoom", 22)
                })
            })

            // Note: glyphs and sprite entries (demo URLs) were removed to avoid MapLibre attempting
            // to fetch demo resources which can cause "Unable to parse resourceUrl" errors.
        }

        return styleJson.toString()
    }
}
