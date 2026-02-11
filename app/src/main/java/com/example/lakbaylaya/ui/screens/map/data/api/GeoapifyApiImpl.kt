package com.example.lakbaylaya.ui.screens.map.data.api

import android.util.Log
import com.example.lakbaylaya.BuildConfig
import com.example.lakbaylaya.ui.screens.map.data.api.client.GeoapifyHttpClient
import com.example.lakbaylaya.ui.screens.map.data.api.models.GeocodeResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.PlaceDetailsResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.PlacesResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.ReverseGeocodeFeature
import com.example.lakbaylaya.ui.screens.map.data.api.models.ReverseGeocodeProperties
import com.example.lakbaylaya.ui.screens.map.data.api.models.ReverseGeocodeResponse
import com.example.lakbaylaya.ui.screens.map.data.api.models.RoutingResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Implementation of GeoapifyApi using HTTP client
 *
 * This class handles all communication with the Geoapify REST API.
 * It uses dependency injection for the HTTP client to enable testing.
 */
class GeoapifyApiImpl(
    private val httpClient: GeoapifyHttpClient = GeoapifyHttpClient()
) : GeoapifyApi {

    private val gson = Gson()

    private val apiKey = BuildConfig.GEOAPIFY_API_KEY
    private val baseUrl = "https://api.geoapify.com/v1"

    companion object {
        private const val TAG = "GeoapifyApiImpl"
    }

    override suspend fun searchPlaces(
        query: String,
        lat: Double?,
        lon: Double?,
        limit: Int,
        bias: String
    ): Result<PlacesResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(
                endpoint = "geocode/search",
                params = buildMap {
                    put("text", query)
                    put("limit", limit.toString())
                    put("apiKey", apiKey)
                    if (lat != null && lon != null) {
                        put("lat", lat.toString())
                        put("lon", lon.toString())
                        // Format bias correctly: proximity:lon,lat (note: lon comes first in Geoapify)
                        put("bias", "proximity:$lon,$lat")
                    }
                }
            )

            val response = httpClient.get(url)
            val placesResponse = parsePlacesResponse(response)
            Result.success(placesResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching places: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun reverseGeocode(
        lat: Double,
        lon: Double
    ): Result<GeocodeResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(
                endpoint = "geocode/reverse",
                params = mapOf(
                    "lat" to lat.toString(),
                    "lon" to lon.toString(),
                    "apiKey" to apiKey
                )
            )

            val response = httpClient.get(url)
            val geocodeResponse = parseGeocodeResponse(response)
            Result.success(geocodeResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error reverse geocoding: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun reverseGeocodeWithAmenity(
        lat: Double,
        lon: Double,
        radius: Int
    ): Result<ReverseGeocodeResponse> = withContext(Dispatchers.IO) {
        try {
            // Build reverse-geocoding request URL using latitude, longitude, limit=10, and API key
            val url = buildUrl(
                endpoint = "geocode/reverse",
                params = mapOf(
                    "lat" to lat.toString(),
                    "lon" to lon.toString(),
                    "type" to "amenity",
                    "limit" to "10", // Algorithm specifies limit=10
                    "apiKey" to apiKey
                )
            )

            // Perform network request and parse JSON response into place objects
            val response = httpClient.get(url)
            val reverseResponse = parseReverseGeocodeResponse(response)
            Result.success(reverseResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error reverse geocoding with amenity: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPlaceDetails(
        placeId: String
    ): Result<PlaceDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(
                endpoint = "geocode/details",
                params = mapOf(
                    "id" to placeId,
                    "apiKey" to apiKey
                )
            )

            val response = httpClient.get(url)
            val detailsResponse = parsePlaceDetailsResponse(response)
            Result.success(detailsResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting place details: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun autocomplete(
        query: String,
        lat: Double?,
        lon: Double?,
        limit: Int
    ): Result<PlacesResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(
                endpoint = "geocode/autocomplete",
                params = buildMap {
                    put("text", query)
                    put("limit", limit.toString())
                    put("apiKey", apiKey)
                    if (lat != null && lon != null) {
                        put("lat", lat.toString())
                        put("lon", lon.toString())
                    }
                }
            )

            val response = httpClient.get(url)
            val placesResponse = parsePlacesResponse(response)
            Result.success(placesResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error autocompleting: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Attempt routing with multiple parameter variants to improve success rate
    private suspend fun tryRoutingWithVariants(waypointsString: String, preferredMode: String, alternatives: Int): Result<RoutingResponse> = withContext(Dispatchers.IO) {
        // Variants to try: (mode, details)
        val variants = listOf(
            Pair(preferredMode, true),
            Pair(preferredMode, false),
            Pair("drive", false),
            Pair("walk", false)
        ).distinct()

        val separators = listOf("|", ";")

        var lastError: Exception? = null

        for (sep in separators) {
            val normalizedOriginal = if (waypointsString.contains("|") || waypointsString.contains(";")) {
                // normalize original to the separator we're testing
                waypointsString.replace("|", sep).replace(";", sep)
            } else waypointsString

            // Also prepare a swapped-order version (lat,lon) in case API expects lat,lon
            val swapped = try {
                normalizedOriginal.split(sep).joinToString(sep) { pair ->
                    val parts = pair.split(",")
                    if (parts.size >= 2) {
                        // if original is lon,lat -> swapped lat,lon ; if already lat,lon -> becomes lon,lat
                        "${parts[1]},${parts[0]}"
                    } else pair
                }
            } catch (e: Exception) {
                normalizedOriginal
            }

            val candidates = listOf(normalizedOriginal, swapped).distinct()

            for (adjustedWaypoints in candidates) {
                for ((mode, details) in variants) {
                    try {
                        val params = mutableMapOf(
                            "waypoints" to adjustedWaypoints,
                            "mode" to mode,
                            "units" to "metric",
                            "apiKey" to apiKey
                        )
                        if (details) params["details"] = "instruction_details"
                        if (alternatives > 0) params["alternatives"] = alternatives.toString()

                        val url = buildUrl(endpoint = "routing", params = params)
                        Log.d(TAG, "Trying routing variant mode=$mode details=$details sep=$sep swapped=${adjustedWaypoints==swapped} url=$url")
                        val response = httpClient.get(url)
                        Log.d(TAG, "Variant response length: ${response.length}")

                        // Parse; parseRoutingResponse will throw if no routes
                        val routingResponse = parseRoutingResponse(response)
                        return@withContext Result.success(routingResponse)
                    } catch (e: Exception) {
                        Log.w(TAG, "Routing variant failed (sep=$sep swapped=${adjustedWaypoints==swapped} mode=${mode}, details=${details}): ${e.message}")
                        lastError = e
                    }
                }
            }
        }

        Result.failure(lastError ?: Exception("Routing failed with all variants"))
    }

    override suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        mode: String,
        alternatives: Int
    ): Result<RoutingResponse> = withContext(Dispatchers.IO) {
        try {
            // Geoapify expects waypoints as lon,lat
            val waypointsString = "$startLon,$startLat|$endLon,$endLat"

            // First try requested parameters
            val primaryParams = mapOf(
                "waypoints" to waypointsString,
                "mode" to mode,
                "details" to "instruction_details",
                "units" to "metric",
                "alternatives" to alternatives.toString(),
                "apiKey" to apiKey
            )

            val primaryUrl = buildUrl(endpoint = "routing", params = primaryParams)
            Log.d(TAG, "Primary routing URL: $primaryUrl")
            try {
                val response = httpClient.get(primaryUrl)
                val routingResponse = parseRoutingResponse(response)
                return@withContext Result.success(routingResponse)
            } catch (e: Exception) {
                Log.w(TAG, "Primary routing request failed: ${e.message}")
                // Fall through to variant attempts
            }

            // Try variants
            return@withContext tryRoutingWithVariants(waypointsString, mode, alternatives)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting route: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getRouteWithWaypoints(
        waypoints: List<Pair<Double, Double>>,
        mode: String,
        alternatives: Int
    ): Result<RoutingResponse> = withContext(Dispatchers.IO) {
        try {
            if (waypoints.size < 2) {
                throw IllegalArgumentException("At least 2 waypoints required")
            }

            // Format waypoints as "lon1,lat1|lon2,lat2|..." (Geoapify expects lon,lat)
            val waypointsString = waypoints.joinToString("|") { "${it.second},${it.first}" }

            // Primary attempt
            val primaryParams = mutableMapOf<String, String>(
                "waypoints" to waypointsString,
                "mode" to mode,
                "details" to "instruction_details",
                "units" to "metric",
                "apiKey" to apiKey
            )
            if (alternatives > 0) primaryParams["alternatives"] = alternatives.toString()

            val primaryUrl = buildUrl(endpoint = "routing", params = primaryParams)
            Log.d(TAG, "Primary waypoints routing URL: $primaryUrl")

            try {
                val response = httpClient.get(primaryUrl)
                val routingResponse = parseRoutingResponse(response)
                return@withContext Result.success(routingResponse)
            } catch (e: Exception) {
                Log.w(TAG, "Primary waypoints routing failed: ${e.message}")
            }

            // Try variants
            return@withContext tryRoutingWithVariants(waypointsString, mode, alternatives)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting route with waypoints: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun buildUrl(endpoint: String, params: Map<String, String>): String {
        val queryString = params.entries.joinToString("&") { (key, value) ->
            val encoded = try {
                if (value.contains("|")) {
                    // Waypoints must be sent as raw lon,lat|lon,lat per Geoapify docs — do not encode commas or pipes
                    value
                } else {
                    java.net.URLEncoder.encode(value, "UTF-8")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to encode param value: $value", e)
                value
            }
            "$key=$encoded"
        }
        val full = "$baseUrl/$endpoint?$queryString"
        Log.d(TAG, "Built URL: $full")
        return full
    }

    private fun parsePlacesResponse(json: String): PlacesResponse {
        val jsonObject = JSONObject(json)
        val features = jsonObject.getJSONArray("features")
        val places = mutableListOf<PlacesResponse.Place>()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val properties = feature.getJSONObject("properties")
            val geometry = feature.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            places.add(PlacesResponse.Place(
                placeId = properties.optString("place_id", ""),
                name = properties.optString("name", properties.optString("address_line1", "")),
                address = properties.optString("formatted", ""),
                latitude = coordinates.getDouble(1),
                longitude = coordinates.getDouble(0),
                category = properties.optString("categories", "").split(",").firstOrNull() ?: "",
                distance = properties.optDouble("distance", 0.0)
            ))
        }

        return PlacesResponse(places = places)
    }

    private fun parseGeocodeResponse(json: String): GeocodeResponse {
        val jsonObject = JSONObject(json)
        val features = jsonObject.optJSONArray("features")

        if (features != null && features.length() > 0) {
            val feature = features.getJSONObject(0)
            val properties = feature.getJSONObject("properties")
            val geometry = feature.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            return GeocodeResponse(
                name = properties.optString("name", ""),
                address = properties.optString("formatted", ""),
                latitude = coordinates.getDouble(1),
                longitude = coordinates.getDouble(0),
                city = properties.optString("city", ""),
                country = properties.optString("country", "")
            )
        }

        throw Exception("No results found")
    }

    private fun parsePlaceDetailsResponse(json: String): PlaceDetailsResponse {
        val jsonObject = JSONObject(json)
        val features = jsonObject.optJSONArray("features")

        if (features != null && features.length() > 0) {
            val feature = features.getJSONObject(0)
            val properties = feature.getJSONObject("properties")
            val geometry = feature.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            return PlaceDetailsResponse(
                placeId = properties.optString("place_id", ""),
                name = properties.optString("name", ""),
                address = properties.optString("formatted", ""),
                latitude = coordinates.getDouble(1),
                longitude = coordinates.getDouble(0),
                category = properties.optString("categories", ""),
                phone = properties.optString("phone", ""),
                website = properties.optString("website", ""),
                openingHours = properties.optString("opening_hours", "")
            )
        }

        throw Exception("Place details not found")
    }

    private fun parseReverseGeocodeResponse(json: String): ReverseGeocodeResponse {
        val jsonObject = JSONObject(json)
        val features = jsonObject.optJSONArray("features") ?: return ReverseGeocodeResponse(emptyList())

        val featuresList = mutableListOf<ReverseGeocodeFeature>()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val properties = feature.getJSONObject("properties")

            // Extract coordinates from geometry for distance calculation
            val geometry = feature.optJSONObject("geometry")
            val coordinates = geometry?.optJSONArray("coordinates")
            val longitude = coordinates?.optDouble(0) ?: 0.0
            val latitude = coordinates?.optDouble(1) ?: 0.0

            val reverseProperties = ReverseGeocodeProperties(
                name = properties.optString("name"),
                amenity = properties.optString("amenity"),
                street = properties.optString("street"),
                houseNumber = properties.optString("housenumber"),
                suburb = properties.optString("suburb"),
                city = properties.optString("city"),
                state = properties.optString("state"),
                postcode = properties.optString("postcode"),
                country = properties.optString("country"),
                countryCode = properties.optString("country_code"),
                formatted = properties.optString("formatted"),
                addressLine1 = properties.optString("address_line1"),
                addressLine2 = properties.optString("address_line2"),
                distance = properties.optDouble("distance", 0.0)
            )

            featuresList.add(ReverseGeocodeFeature(reverseProperties, latitude, longitude))
        }

        return ReverseGeocodeResponse(featuresList)
    }

    private fun parseRoutingResponse(json: String): RoutingResponse {
        Log.d(TAG, "Parsing routing response JSON (Gson preferred): ${json.take(500)}...")
        // 1) Try Gson (OkHttp-friendly parsing)
        try {
            val parsed = gson.fromJson(json, RoutingResponse::class.java)
            try {
                if (parsed != null && parsed.features != null && parsed.features.isNotEmpty()) {
                    return parsed
                } else {
                    Log.w(TAG, "Gson parsed response but no features found, falling back. Parsed object: $parsed")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gson parsed object but features access failed: ${e.message}")
            }
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "Gson failed to parse routing response: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Gson parsing error: ${e.message}")
        }

        // 2) Fallback to manual parser (robust for different shapes)
        return parseRoutingResponseManually(json)
    }

    private fun parseRoutingResponseManually(json: String): RoutingResponse {
        val jsonObject = JSONObject(json)

        // If API returned an explicit error object or message, surface it
        val topLevelMessage = jsonObject.optString("message", null)
            ?: jsonObject.optString("error", null)
        if (!topLevelMessage.isNullOrBlank()) {
            Log.e(TAG, "Routing API returned error/message: $topLevelMessage")
            throw Exception("Routing API error: $topLevelMessage")
        }

        // 0) Some providers wrap payload in 'data' object
        if (jsonObject.has("data")) {
            try {
                val dataObj = jsonObject.get("data")
                if (dataObj is JSONObject) {
                    // check for features inside data
                    val df = dataObj.optJSONArray("features")
                    if (df != null && df.length() > 0) return parseFeaturesArray(dataObj as JSONObject, df)
                    val dr = dataObj.optJSONArray("routes")
                    if (dr != null && dr.length() > 0) {
                        // reuse routes-array parsing by building a minimal response
                        val fake = JSONObject()
                        fake.put("routes", dr)
                        return parseRoutingResponseManually(fake.toString())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse 'data' wrapper: ${e.message}")
            }
        }

        // 1) Try GeoJSON-style features at top-level
        val features = jsonObject.optJSONArray("features")
        if (features != null && features.length() > 0) {
            return parseFeaturesArray(jsonObject, features)
        }

        // 2) Fallback: some APIs return a 'routes' array (non-GeoJSON)
        val routesArray = jsonObject.optJSONArray("routes")
        if (routesArray != null && routesArray.length() > 0) {
            // Build RoutingResponse from routes array
            val featuresList = mutableListOf<com.example.lakbaylaya.ui.screens.map.data.api.models.RouteFeature>()
            for (i in 0 until routesArray.length()) {
                try {
                    val routeObj = routesArray.getJSONObject(i)
                    // distance/time keys may be 'distance'/'duration' or 'distance'/'time'
                    val distance = routeObj.optDouble("distance", routeObj.optDouble("distance", 0.0))
                    val time = routeObj.optDouble("duration", routeObj.optDouble("time", 0.0))

                    // Try to extract geometry.coordinates (could be GeoJSON-like) or legs/geometry
                    val geometryObj = routeObj.optJSONObject("geometry")
                    val coords = mutableListOf<List<Double>>()
                    if (geometryObj != null) {
                        val coordsArray = geometryObj.optJSONArray("coordinates")
                        if (coordsArray != null) {
                            // flatten possible nested arrays
                            fun flatten(array: org.json.JSONArray) {
                                for (j in 0 until array.length()) {
                                    val item = array.get(j)
                                    if (item is org.json.JSONArray) {
                                        if (item.length() >= 2 && item.opt(0) is Number && item.opt(1) is Number) {
                                            coords.add(listOf(item.getDouble(0), item.getDouble(1)))
                                        } else {
                                            flatten(item)
                                        }
                                    }
                                }
                            }
                            flatten(coordsArray)
                        }
                    }

                    // Create minimal RouteFeature with parsed values
                    val props = com.example.lakbaylaya.ui.screens.map.data.api.models.RouteProperties(
                        distance = distance,
                        time = time,
                        legs = emptyList()
                    )
                    val geom = com.example.lakbaylaya.ui.screens.map.data.api.models.RouteGeometry(
                        type = geometryObj?.optString("type", "LineString") ?: "LineString",
                        coordinates = coords
                    )

                    featuresList.add(
                        com.example.lakbaylaya.ui.screens.map.data.api.models.RouteFeature(
                            type = "Feature",
                            properties = props,
                            geometry = geom
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse route entry $i in 'routes' array: ${e.message}")
                }
            }

            if (featuresList.isNotEmpty()) {
                return RoutingResponse(type = jsonObject.optString("type", "FeatureCollection"), features = featuresList)
            }
        }

        // 3) No recognized routes found — log top-level keys and sample of response to aid debugging and fail
        val keys = jsonObject.keys().asSequence().toList()
        Log.e(TAG, "No routes found. Top-level keys: $keys")
        Log.e(TAG, "Raw response (truncated): ${json.take(2000)}")
        throw Exception("No routes found")
    }

    // Helper: parse GeoJSON features array into RoutingResponse
    private fun parseFeaturesArray(originObject: JSONObject, features: org.json.JSONArray): RoutingResponse {
        val featuresList = mutableListOf<com.example.lakbaylaya.ui.screens.map.data.api.models.RouteFeature>()

        for (i in 0 until features.length()) {
            try {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val geometry = feature.getJSONObject("geometry")
                val geomType = geometry.optString("type", "LineString")

                // Parse coordinates for LineString and MultiLineString
                val coordinatesArray = geometry.getJSONArray("coordinates")
                val coordinates = mutableListOf<List<Double>>()

                fun flattenCoords(array: org.json.JSONArray) {
                    for (idx in 0 until array.length()) {
                        val item = array.get(idx)
                        if (item is org.json.JSONArray) {
                            if (item.length() >= 2 && item.opt(0) is Number && item.opt(1) is Number) {
                                coordinates.add(listOf(item.getDouble(0), item.getDouble(1)))
                            } else {
                                flattenCoords(item)
                            }
                        }
                    }
                }

                flattenCoords(coordinatesArray)

                // Parse legs and steps
                val legsArray = properties.optJSONArray("legs")
                val legs = mutableListOf<com.example.lakbaylaya.ui.screens.map.data.api.models.RouteLeg>()

                if (legsArray != null) {
                    for (k in 0 until legsArray.length()) {
                        val legObject = legsArray.getJSONObject(k)
                        val stepsArray = legObject.optJSONArray("steps")
                        val steps = mutableListOf<com.example.lakbaylaya.ui.screens.map.data.api.models.RouteStep>()

                        if (stepsArray != null) {
                            for (l in 0 until stepsArray.length()) {
                                val stepObject = stepsArray.getJSONObject(l)
                                val instruction = stepObject.optJSONObject("instruction")
                                val locationArray = stepObject.optJSONArray("location")
                                val location = if (locationArray != null && locationArray.length() >= 2) {
                                    listOf(locationArray.getDouble(0), locationArray.getDouble(1))
                                } else {
                                    emptyList()
                                }

                                val stepInstruction = com.example.lakbaylaya.ui.screens.map.data.api.models.StepInstruction(
                                    text = instruction?.optString("text") ?: "",
                                    type = instruction?.optInt("type")
                                )

                                steps.add(
                                    com.example.lakbaylaya.ui.screens.map.data.api.models.RouteStep(
                                        distance = stepObject.optDouble("distance", 0.0),
                                        time = stepObject.optDouble("time", 0.0),
                                        instruction = stepInstruction,
                                        name = stepObject.optString("name"),
                                        type = stepObject.optInt("type"),
                                        location = location
                                    )
                                )
                            }
                        }

                        legs.add(
                            com.example.lakbaylaya.ui.screens.map.data.api.models.RouteLeg(
                                distance = legObject.optDouble("distance", 0.0),
                                time = legObject.optDouble("time", 0.0),
                                steps = steps
                            )
                        )
                    }
                }

                val routeProperties = com.example.lakbaylaya.ui.screens.map.data.api.models.RouteProperties(
                    distance = properties.optDouble("distance", 0.0),
                    time = properties.optDouble("time", 0.0),
                    legs = legs,
                    mode = properties.optString("mode")
                )

                val routeGeometry = com.example.lakbaylaya.ui.screens.map.data.api.models.RouteGeometry(
                    type = geomType,
                    coordinates = coordinates
                )

                featuresList.add(
                    com.example.lakbaylaya.ui.screens.map.data.api.models.RouteFeature(
                        type = feature.optString("type", "Feature"),
                        properties = routeProperties,
                        geometry = routeGeometry
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing route feature $i: ${e.message}", e)
                // Continue with next feature
            }
        }

        if (featuresList.isEmpty()) {
            throw Exception("No valid routes found in response")
        }

        return RoutingResponse(
            type = originObject.optString("type", "FeatureCollection"),
            features = featuresList
        )
    }
}
