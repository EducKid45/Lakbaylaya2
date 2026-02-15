package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing landmark markers with location and route difficulty.
 */
@Entity(tableName = "landmarks")
data class LandmarkEntity(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val routeDifficulty: String = "None",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

