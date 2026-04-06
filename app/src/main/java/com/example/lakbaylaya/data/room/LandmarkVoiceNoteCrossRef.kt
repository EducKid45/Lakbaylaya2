package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between landmarks and voice notes.
 * Index on voiceNoteId avoids full table scan when resolving the relationship.
 */
@Entity(
    tableName = "landmark_voice_note",
    primaryKeys = ["landmarkId", "voiceNoteId"],
    indices = [Index("voiceNoteId")]
)
data class LandmarkVoiceNoteCrossRef(
    val landmarkId: String,
    val voiceNoteId: String
)

