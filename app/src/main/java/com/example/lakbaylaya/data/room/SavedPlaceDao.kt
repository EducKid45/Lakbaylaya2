package com.example.lakbaylaya.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for saved places operations
 */
@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlaceEntity)

    @Query("SELECT * FROM saved_places ORDER BY created_at DESC")
    suspend fun getAll(): List<SavedPlaceEntity>

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun getById(id: String): SavedPlaceEntity?

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM saved_places WHERE label = :label ORDER BY created_at DESC")
    suspend fun getByLabel(label: String): List<SavedPlaceEntity>
}

