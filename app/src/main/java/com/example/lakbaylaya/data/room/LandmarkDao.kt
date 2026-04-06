package com.example.lakbaylaya.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for landmark operations.
 * Voice note relationships have been removed.
 */
@Dao
interface LandmarkDao {
    @Query("SELECT * FROM landmarks ORDER BY timestamp DESC")
    fun getAllLandmarks(): Flow<List<LandmarkEntity>>

    /** One-shot query for use in coroutines (e.g. proximity check). */
    @Query("SELECT * FROM landmarks ORDER BY timestamp DESC")
    suspend fun getAllLandmarksOnce(): List<LandmarkEntity>

    @Query("SELECT * FROM landmarks WHERE id = :id")
    suspend fun getLandmarkById(id: String): LandmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandmark(landmark: LandmarkEntity)

    @Update
    suspend fun updateLandmark(landmark: LandmarkEntity)

    @Delete
    suspend fun deleteLandmark(landmark: LandmarkEntity)
}
