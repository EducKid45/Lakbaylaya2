package com.example.lakbaylaya.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: SavedRouteEntity)

    @Query("SELECT * FROM saved_routes ORDER BY name ASC")
    suspend fun getAll(): List<SavedRouteEntity>

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun delete(id: String)
}
