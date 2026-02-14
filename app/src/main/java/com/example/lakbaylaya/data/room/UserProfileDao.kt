package com.example.lakbaylaya.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile WHERE id = 0")
    suspend fun clearProfile()
}
