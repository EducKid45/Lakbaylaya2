package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing voice note recordings with location and metadata.
 */
@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String = "",
    val audioFilePath: String,
    val transcription: String = "",
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

