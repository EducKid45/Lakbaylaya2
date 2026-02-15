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
    @ColumnInfo(name = "start_latitude") val startLatitude: Double = 0.0,
    @ColumnInfo(name = "start_longitude") val startLongitude: Double = 0.0,
    @ColumnInfo(name = "end_latitude") val endLatitude: Double = 0.0,
    @ColumnInfo(name = "end_longitude") val endLongitude: Double = 0.0,
    @ColumnInfo(name = "distance_km") val distanceKm: Double,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int,
    @ColumnInfo(name = "has_voice_notes") val hasVoiceNotes: Boolean = false,
    @ColumnInfo(name = "has_difficult_segments") val hasDifficultSegments: Boolean = false,
    @ColumnInfo(name = "landmarks_json") val landmarksJson: String = "[]",
    @ColumnInfo(name = "voice_note_count") val voiceNoteCount: Int = 0,
    @ColumnInfo(name = "difficult_segment_count") val difficultSegmentCount: Int = 0,
    @ColumnInfo(name = "polyline") val polyline: String? = null,
    @ColumnInfo(name = "route_steps_json") val routeStepsJson: String? = null, // JSON array of DirectionStep
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// Room-based entity moved to legacy package because Room dependencies are not added in Gradle.
// If you want Room support later, re-enable Room dependencies and move this back to `data.room`.
