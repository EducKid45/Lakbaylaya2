package com.example.lakbaylaya.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "start_location") val startLocation: String,
    @ColumnInfo(name = "end_location") val endLocation: String,
    @ColumnInfo(name = "distance_km") val distanceKm: Double,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int,
    @ColumnInfo(name = "has_voice_notes") val hasVoiceNotes: Boolean,
    @ColumnInfo(name = "has_difficult_segments") val hasDifficultSegments: Boolean,
    @ColumnInfo(name = "landmarks_json") val landmarksJson: String,
    @ColumnInfo(name = "voice_note_count") val voiceNoteCount: Int,
    @ColumnInfo(name = "difficult_segment_count") val difficultSegmentCount: Int,
    @ColumnInfo(name = "polyline") val polyline: String? = null
)

// Room-based entity moved to legacy package because Room dependencies are not added in Gradle.
// If you want Room support later, re-enable Room dependencies and move this back to `data.room`.
