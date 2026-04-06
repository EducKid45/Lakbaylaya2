package com.example.lakbaylaya.data.repository

import android.content.Context
import android.util.Log
import com.example.lakbaylaya.data.room.AppDatabase
import com.example.lakbaylaya.data.room.NavigationStatsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for reading and writing per-day navigation statistics.
 * Statistics are keyed by [LocalDate.toEpochDay()] — one row per calendar day.
 * Rows older than 30 days are pruned automatically on every write.
 */
class NavigationStatsRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).navigationStatsDao()

    companion object {
        private const val TAG = "NavStatsRepo"
        private const val PRUNE_DAYS = 30L
    }

    /** Observe stats for today and the last [days] days (for the profile screen). */
    fun observeSince(days: Long = 7): Flow<List<NavigationStatsEntity>> {
        val since = LocalDate.now().toEpochDay() - days
        return dao.observeSince(since)
    }

    /**
     * Record the completion of one navigation session.
     * Called from [com.example.lakbaylaya.ui.screens.map.viewmodel.MapNavigationManager]
     * when navigation completes.
     *
     * @param distanceMeters Total distance covered during session.
     * @param actualSteps Real pedometer step count for the session (0 = estimate from distance).
     */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun recordSession(distanceMeters: Double, actualSteps: Int = 0) {
        val today = LocalDate.now().toEpochDay()
        val existing = dao.getForDay(today) ?: NavigationStatsEntity(today)
        val stepsToAdd = if (actualSteps > 0) actualSteps else (distanceMeters / 0.762).toInt()
        val updated = existing.copy(
            distanceMeters = existing.distanceMeters + distanceMeters,
            steps = existing.steps + stepsToAdd,
            routesCompleted = existing.routesCompleted + 1
        )
        dao.upsert(updated)
        Log.d(TAG, "Session recorded: +${distanceMeters}m, +${stepsToAdd} steps (actual=$actualSteps), routes=${updated.routesCompleted}")
        pruneOldStats()
    }

    /** Increment only distance + estimated steps (e.g. partial GPS update during navigation). */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun addDistance(distanceMeters: Double) {
        val today = LocalDate.now().toEpochDay()
        val existing = dao.getForDay(today) ?: NavigationStatsEntity(today)
        // steps are now tracked via addSteps() from the real pedometer; don't double-count here
        dao.upsert(existing.copy(
            distanceMeters = existing.distanceMeters + distanceMeters
        ))
    }

    /**
     * Increment steps for today using the real pedometer delta.
     * Called from PedometerStatsTracker on every sensor flush.
     */
    suspend fun addSteps(deltaSteps: Int) {
        if (deltaSteps <= 0) return
        val today = LocalDate.now().toEpochDay()
        // Ensure the row exists before trying an UPDATE
        val existing = dao.getForDay(today)
        if (existing == null) dao.upsert(NavigationStatsEntity(today, steps = deltaSteps))
        else dao.incrementSteps(today, deltaSteps)
        Log.d(TAG, "Steps added: +$deltaSteps")
    }

    private suspend fun pruneOldStats() {
        val cutoff = LocalDate.now().toEpochDay() - PRUNE_DAYS
        dao.deleteOlderThan(cutoff)
        Log.d(TAG, "Pruned stats older than $PRUNE_DAYS days (cutoff epoch day: $cutoff)")
    }
}

