package com.example.lakbaylaya.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for saved places
 */
@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "place_name") val placeName: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "label") val label: String, // e.g., "Home", "Work", "Favorite"
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

