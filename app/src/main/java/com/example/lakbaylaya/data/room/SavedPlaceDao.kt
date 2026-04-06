package com.example.lakbaylaya.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved places operations
 */
@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlaceEntity)

    @Update
    suspend fun update(place: SavedPlaceEntity)

    @Query("SELECT * FROM saved_places ORDER BY created_at DESC")
    suspend fun getAll(): List<SavedPlaceEntity>

    /** Live stream — Route screen observes this so the list refreshes automatically. */
    @Query("SELECT * FROM saved_places ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun getById(id: String): SavedPlaceEntity?

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM saved_places WHERE label = :label ORDER BY created_at DESC")
    suspend fun getByLabel(label: String): List<SavedPlaceEntity>

    /**
     * Find any existing place whose coords are within ~1 metre of the given values.
     * Used for duplicate-coordinate detection before saving.
     */
    @Query("""
        SELECT * FROM saved_places
        WHERE ABS(latitude  - :lat) < 0.00001
          AND ABS(longitude - :lon) < 0.00001
        LIMIT 1
    """)
    suspend fun findByCoordinates(lat: Double, lon: Double): SavedPlaceEntity?
}
