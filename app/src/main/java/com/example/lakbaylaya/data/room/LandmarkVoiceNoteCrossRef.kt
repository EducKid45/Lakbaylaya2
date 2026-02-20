package com.example.lakbaylaya.data.room

import androidx.room.Entity

/**
 * Junction table for many-to-many relationship between landmarks and voice notes.
 */
@Entity(tableName = "landmark_voice_note", primaryKeys = ["landmarkId", "voiceNoteId"])
data class LandmarkVoiceNoteCrossRef(
    val landmarkId: String,
    val voiceNoteId: String
)

