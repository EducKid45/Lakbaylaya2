package com.example.lakbaylaya.ui.screens.map.navigation.tts

import org.json.JSONObject

/**
 * OSM tag snapshot extracted from a Geoapify routing step's properties.osm_tags node.
 */
data class StepOsmTags(
    val highway: String? = null,
    val junction: String? = null,
    val footway: String? = null,
    val surface: String? = null,
    val name: String? = null
)

/**
 * Parsed representation of a single Geoapify routing step, ready for TTS use.
 * Includes optional rawInstruction text so we can fallback when bearings are missing.
 */
data class GeoapifyStep(
    val distanceMeters: Double,
    val bearingBefore: Double,
    val bearingAfter: Double,
    val street: String?,
    val lat: Double,
    val lon: Double,
    val osmTags: StepOsmTags = StepOsmTags(),
    val rawInstruction: String? = null
)

/**
 * Parse a raw Geoapify routing step represented as JSONObject into [GeoapifyStep].
 */
fun parseGeoapifyStep(json: JSONObject): GeoapifyStep {
    val distance = json.optDouble("distance", 0.0)
    val bearingBefore = json.optDouble("bearing_before", 0.0)
    val bearingAfter = json.optDouble("bearing_after", 0.0)
    val street = json.optString("name").takeIf { it.isNotBlank() }

    // Location may be [lon, lat] or [lat, lon] depending on API; try common shapes.
    val locationArray = json.optJSONArray("location")
    var lat = 0.0
    var lon = 0.0
    if (locationArray != null) {
        // If length >= 2, guess ordering by value ranges (-90..90 for lat)
        if (locationArray.length() >= 2) {
            val a0 = locationArray.optDouble(0)
            val a1 = locationArray.optDouble(1)
            // Heuristic: latitude is between -90 and 90
            if (a0 >= -90.0 && a0 <= 90.0 && (a1 < -90.0 || a1 > 90.0)) {
                lat = a0
                lon = a1
            } else if (a1 >= -90.0 && a1 <= 90.0 && (a0 < -90.0 || a0 > 90.0)) {
                lat = a1
                lon = a0
            } else {
                // Ambiguous: assume [lon, lat] as GeoJSON is common
                lon = a0
                lat = a1
            }
        }
    }

    val props = json.optJSONObject("properties")
    val osmJson = props?.optJSONObject("osm_tags")
    val osmTags = osmJson?.let {
        StepOsmTags(
            highway = it.optString("highway").takeIf { s -> s.isNotBlank() },
            junction = it.optString("junction").takeIf { s -> s.isNotBlank() },
            footway = it.optString("footway").takeIf { s -> s.isNotBlank() },
            surface = it.optString("surface").takeIf { s -> s.isNotBlank() },
            name = it.optString("name").takeIf { s -> s.isNotBlank() }
        )
    } ?: StepOsmTags()

    // Extract raw instruction text if present
    val instructionText = json.optJSONObject("instruction")?.optString("text")
        ?: json.optString("instruction_text").takeIf { it.isNotBlank() }

    return GeoapifyStep(
        distanceMeters = distance,
        bearingBefore = bearingBefore,
        bearingAfter = bearingAfter,
        street = street,
        lat = lat,
        lon = lon,
        osmTags = osmTags,
        rawInstruction = instructionText?.takeIf { it.isNotBlank() }
    )
}

/**
 * Simplify a turn using the difference between bearingAfter and bearingBefore.
 * Falls back to parsing raw instruction text if bearings are missing or equal.
 */
fun simplifyTurn(bearingBefore: Double, bearingAfter: Double, rawInstruction: String? = null): String {
    // Prioritize raw instruction keywords to align chip with the route text
    if (!rawInstruction.isNullOrBlank()) {
        // Normalize: lowercase, replace punctuation with space, collapse multiple spaces
        val normalized = rawInstruction
            .lowercase()
            .replace(Regex("[\u2018\u2019\u201C\u201D\"'.,;:()-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Helpful regex patterns
        val reUTurn = Regex("\\b(u-?turn|turn around|make a u)\\b")
        val reRoundabout = Regex("\\b(roundabout|take the [0-9]+(st|nd|rd|th)? exit|exit to)\\b")
        val reSlightLeft = Regex("\\b(slight(?:ly)? left|bear left|veer left)\\b")
        val reSlightRight = Regex("\\b(slight(?:ly)? right|bear right|veer right)\\b")
        val reLeft = Regex("\\b(turn left|left onto|left)\\b")
        val reRight = Regex("\\b(turn right|right onto|right)\\b")
        val reKeepLeft = Regex("\\b(keep left|fork left)\\b")
        val reKeepRight = Regex("\\b(keep right|fork right)\\b")
        val reStraight = Regex("\\b(continue|straight|keep going|proceed)\\b")

        // Use explicit if checks to ensure function returns a String in all branches
        if (reUTurn.containsMatchIn(normalized)) return "turn around"
        if (reRoundabout.containsMatchIn(normalized)) return "turn right"
        if (reSlightLeft.containsMatchIn(normalized)) return "turn slightly left"
        if (reSlightRight.containsMatchIn(normalized)) return "turn slightly right"
        if (reKeepLeft.containsMatchIn(normalized)) return "turn left"
        if (reKeepRight.containsMatchIn(normalized)) return "turn right"
        if (reLeft.containsMatchIn(normalized)) return "turn left"
        if (reRight.containsMatchIn(normalized)) return "turn right"
        if (reStraight.containsMatchIn(normalized)) return "go straight"
        // No clear keyword — fall through to bearing-based logic
    }

    // Robust normalization to (-180, 180]
    val raw = bearingAfter - bearingBefore
    val delta = ((raw + 540.0) % 360.0) - 180.0

    return when {
        delta > 140.0 || delta < -140.0 -> "turn around"
        delta > 60.0 -> "turn right"
        delta > 20.0 -> "turn slightly right"
        delta >= -20.0 && delta <= 20.0 -> "go straight"
        delta < -60.0 -> "turn left"
        delta < -20.0 -> "turn slightly left"
        else -> "go straight"
    }
}

/**
 * Convert distance in meters to a human-friendly spoken phrase.
 */
fun humanDistance(distanceMeters: Double): String {
    return when {
        distanceMeters < 20.0 -> "a few steps"
        distanceMeters < 50.0 -> "about 50 meters"
        distanceMeters < 100.0 -> "about 100 meters"
        else -> "about ${distanceMeters.toInt()} meters"
    }
}

/**
 * Build safety warning sentences based on OSM tags attached to the step.
 */
fun buildSafetyWarning(step: GeoapifyStep): String {
    val warnings = mutableListOf<String>()
    val tags = step.osmTags

    when (tags.highway) {
        "crossing" -> warnings += "Crosswalk ahead."
        "traffic_signals" -> warnings += "Traffic light crossing ahead."
        "primary", "secondary", "trunk" -> warnings += "Busy road ahead. Walk carefully."
    }

    when (tags.junction) {
        "roundabout" -> warnings += "Roundabout ahead. Use caution."
    }

    when (tags.footway) {
        "sidewalk" -> warnings += "Sidewalk available."
        null -> warnings += "Road without sidewalk. Use caution."
        else -> { /* other footway values - ignore by default */ }
    }

    return warnings.joinToString(" ")
}

/**
 * Build a single spoken instruction sentence for the step. Optionally include a nearby landmark.
 */
fun buildSpokenInstruction(step: GeoapifyStep, landmark: String? = null): String {
    val parts = mutableListOf<String>()

    // Movement + distance
    val distancePhrase = humanDistance(step.distanceMeters)

    // Turn phrase derived from bearings (use rawInstruction as fallback)
    val turnPhrase = simplifyTurn(step.bearingBefore, step.bearingAfter, step.rawInstruction)

    // Form first sentence: action + distance
    val actionSentence = when (turnPhrase) {
        "go straight" -> "Walk straight for $distancePhrase."
        else -> {
            // For turns, say distance first then the turn as separate sentence to be clearer
            "Walk for $distancePhrase. ${turnPhrase.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}."
        }
    }
    parts += actionSentence

    // Street information
    step.street?.takeIf { it.isNotBlank() }?.let { streetName ->
        parts += "You are now on $streetName."
    }

    // Landmark
    landmark?.takeIf { it.isNotBlank() }?.let { lm ->
        parts += "You are near a $lm."
    }

    // Safety warnings
    val warnings = buildSafetyWarning(step)
    if (warnings.isNotBlank()) parts += warnings

    return parts.joinToString(" ")
}

// Small helper (unused externally) to capitalize sentences if needed
private fun String.capitalizeSentence(): String =
    if (this.isEmpty()) this else this[0].uppercaseChar() + this.substring(1)
