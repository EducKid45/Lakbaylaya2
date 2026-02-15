package com.example.lakbaylaya.data.model

import kotlinx.serialization.Serializable

/**
 * Data model for voice notes with GPS location and file metadata.
 */
@Serializable
data class VoiceNote(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val filePath: String,
    val iconPath: String = "marker_icon.png",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L
)

/**
 * Container for voice notes list serialization.
 */
@Serializable
data class VoiceNotesData(
    val notes: List<VoiceNote> = emptyList()
)
