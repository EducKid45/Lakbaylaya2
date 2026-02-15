package com.example.lakbaylaya.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for custom markers operations
 */
@Dao
interface CustomMarkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(marker: CustomMarkerEntity)

    @Query("SELECT * FROM custom_markers ORDER BY created_at DESC")
    suspend fun getAll(): List<CustomMarkerEntity>

    @Query("SELECT * FROM custom_markers WHERE id = :id")
    suspend fun getById(id: String): CustomMarkerEntity?

    @Query("DELETE FROM custom_markers WHERE id = :id")
    suspend fun delete(id: String)
}

