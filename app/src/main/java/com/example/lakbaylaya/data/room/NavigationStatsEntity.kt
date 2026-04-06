package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single row recording walking/navigation statistics for one calendar day.
 * [dateEpochDay] is the result of LocalDate.toEpochDay() — one unique row per day.
 * Rows older than 30 days are auto-deleted by [NavigationStatsDao.deleteOlderThan].
 */
@Entity(tableName = "navigation_stats")
data class NavigationStatsEntity(
    @PrimaryKey val dateEpochDay: Long,   // days since 1970-01-01
    val distanceMeters: Double = 0.0,
    val steps: Int = 0,
    val routesCompleted: Int = 0
)

