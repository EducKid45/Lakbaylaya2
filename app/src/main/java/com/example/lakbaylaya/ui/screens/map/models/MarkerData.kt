package com.example.lakbaylaya.ui.screens.map.models

/**
 * Represents a voice note for a location.
 */
data class VoiceNoteItem(
    val id: String = "",
    val label: String = "",
    val text: String = "", // store transcription or note text
    val audioFilePath: String? = null // optional recorded audio file path
)

/**
 * Marker metadata containing voice notes, landmark details, associated information, and
 * optional geographic coordinates for the marker.
 *
 * Note: latitude/longitude are nullable to indicate they may not be set yet.
 */
data class MarkerMetadata(
    val voiceNotes: List<VoiceNoteItem> = listOf(),
    // note ids attached to the 'Landmark' section (selected from saved voice notes)
    val landmarkNoteIds: List<String> = listOf(),
    val landmarkDescription: String = "",
    val landmarkName: String = "",
    val routeDifficulty: String = "None",
    val latitude: Double? = null,
    val longitude: Double? = null
)
