package com.example.lakbaylaya.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationStatsDao {

    @Query("SELECT * FROM navigation_stats WHERE dateEpochDay = :day LIMIT 1")
    suspend fun getForDay(day: Long): NavigationStatsEntity?

    /** Observe today + last 7 days for the profile screen live stats. */
    @Query("SELECT * FROM navigation_stats WHERE dateEpochDay >= :sinceDay ORDER BY dateEpochDay DESC")
    fun observeSince(sinceDay: Long): Flow<List<NavigationStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: NavigationStatsEntity)

    /** Auto-prune rows older than 30 days. */
    @Query("DELETE FROM navigation_stats WHERE dateEpochDay < :cutoffDay")
    suspend fun deleteOlderThan(cutoffDay: Long)

    /** Atomically increment steps for today without touching distance or routes. */
    @Query("UPDATE navigation_stats SET steps = steps + :deltaSteps WHERE dateEpochDay = :day")
    suspend fun incrementSteps(day: Long, deltaSteps: Int)
}

