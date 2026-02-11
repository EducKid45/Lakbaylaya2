package com.example.lakbaylaya.ui.screens.map.data.api.models

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
    val location: List<Double> = emptyList()
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
