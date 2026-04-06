package com.example.lakbaylaya.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for Geoapify Routing API
 */
@Serializable
data class RoutingResponse(
    @SerialName("type")
    val type: String,
    @SerialName("features")
    val features: List<RouteFeature>
)

@Serializable
data class RouteFeature(
    @SerialName("type")
    val type: String,
    @SerialName("properties")
    val properties: RouteProperties,
    @SerialName("geometry")
    val geometry: RouteGeometry
)

@Serializable
data class RouteProperties(
    @SerialName("distance")
    val distance: Double,
    @SerialName("time")
    val time: Double,
    @SerialName("legs")
    val legs: List<RouteLeg> = emptyList(),
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("waypoints")
    val waypoints: List<Waypoint> = emptyList()
)

@Serializable
data class RouteLeg(
    @SerialName("distance")
    val distance: Double,
    @SerialName("time")
    val time: Double,
    @SerialName("steps")
    val steps: List<RouteStep> = emptyList()
)

@Serializable
data class RouteStep(
    @SerialName("distance")
    val distance: Double,
    @SerialName("time")
    val time: Double,
    @SerialName("instruction")
    val instruction: StepInstruction,
    @SerialName("name")
    val name: String? = null,
    @SerialName("type")
    val type: Int? = null,
    @SerialName("location")
    val location: List<Double> = emptyList(),
    /** Compass bearing at the start of this step (degrees, 0–359). */
    @SerialName("bearing_before")
    val bearingBefore: Double = 0.0,
    /** Compass bearing at the end of this step (degrees, 0–359). */
    @SerialName("bearing_after")
    val bearingAfter: Double = 0.0,
    /** OSM tags carried by the Geoapify step (optional). */
    @SerialName("properties")
    val properties: RouteStepProperties? = null
)

@Serializable
data class RouteStepProperties(
    @SerialName("osm_tags")
    val osmTags: RouteStepOsmTags? = null
)

@Serializable
data class RouteStepOsmTags(
    @SerialName("highway")
    val highway: String? = null,
    @SerialName("junction")
    val junction: String? = null,
    @SerialName("footway")
    val footway: String? = null,
    @SerialName("surface")
    val surface: String? = null
)

@Serializable
data class StepInstruction(
    @SerialName("text")
    val text: String,
    @SerialName("type")
    val type: Int? = null
)

@Serializable
data class Waypoint(
    @SerialName("location")
    val location: List<Double>,
    @SerialName("original_index")
    val originalIndex: Int? = null
)

@Serializable
data class RouteGeometry(
    @SerialName("type")
    val type: String,
    @SerialName("coordinates")
    val coordinates: List<List<Double>>
)
