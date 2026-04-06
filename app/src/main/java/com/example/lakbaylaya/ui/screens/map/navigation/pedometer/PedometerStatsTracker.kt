@file:Suppress("unused", "MemberVisibilityCanBePrivate", "KDocUnresolvedReference")
package com.example.lakbaylaya.ui.screens.map.navigation.pedometer

import android.content.Context
import android.util.Log
import com.example.lakbaylaya.data.repository.NavigationStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * PedometerStatsTracker
 *
 * Bridges [AndroidStepDetector] with [NavigationStatsRepository] so that
 * every step detected during an active navigation session is immediately
 * persisted to the Room database (today's row, auto-pruned after 30 days).
 *
 * Usage:
 *   1. Call [start] when navigation begins.
 *   2. Call [stop] when navigation ends or the app is paused.
 *   3. Steps are written to DB incrementally — no double-counting because
 *      we only add the **delta** since the last flush, not the running total.
 *
 * Thread-safety: step callbacks arrive on the sensor thread; we post to IO.
 */
class PedometerStatsTracker(
    private val context: Context,
    private val statsRepo: NavigationStatsRepository = NavigationStatsRepository(context)
) {

    companion object {
        private const val TAG = "PedometerStatsTracker"
    }

    private val stepDetector = AndroidStepDetector(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Running totals for this session (reset on start)
    private var sessionSteps    = 0
    private var sessionDistance = 0.0

    // Last flushed values — we only write the delta to avoid double-counting
    private var lastFlushedSteps    = 0
    private var lastFlushedDistance = 0.0

    @Volatile var isRunning = false
        private set

    /** Expose current steps for UI display */
    val currentSteps: Int get() = sessionSteps
    /** Expose current distance (metres) for UI display */
    val currentDistance: Double get() = sessionDistance

    // External callbacks for UI updates
    var onStepUpdate:    ((steps: Int,    distanceMeters: Double) -> Unit)? = null
    var onDistanceStep:  ((deltaMeters: Double) -> Unit)?                   = null

    /**
     * Start step detection for a navigation session.
     * Resets session counters.
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running — ignoring start()")
            return
        }
        sessionSteps    = 0
        sessionDistance = 0.0
        lastFlushedSteps    = 0
        lastFlushedDistance = 0.0
        isRunning = true

        stepDetector.start(
            onStepDetected = { totalSteps ->
                sessionSteps = totalSteps
                onStepUpdate?.invoke(sessionSteps, sessionDistance)
                flushDeltaToDb()
            },
            onDistanceUpdated = { totalDistanceMeters ->
                val delta = totalDistanceMeters - sessionDistance
                sessionDistance = totalDistanceMeters
                if (delta > 0) onDistanceStep?.invoke(delta)
                onStepUpdate?.invoke(sessionSteps, sessionDistance)
                flushDistanceDeltaToDb()
            }
        )
        Log.d(TAG, "Pedometer started")
    }

    /**
     * Stop step detection.  Flushes any remaining delta to the DB.
     */
    fun stop() {
        if (!isRunning) return
        stepDetector.stop()
        isRunning = false
        flushDeltaToDb(force = true)
        flushDistanceDeltaToDb(force = true)
        Log.d(TAG, "Pedometer stopped — session: ${sessionSteps}steps / %.1fm".format(sessionDistance))
    }

    /**
     * Call when navigation is marked complete so we increment routesCompleted.
     * Passes actual pedometer step count to avoid double-counting with distance estimates.
     */
    fun recordSessionComplete() {
        val stepsToRecord = sessionSteps
        val distToRecord  = sessionDistance
        scope.launch {
            statsRepo.recordSession(distToRecord, actualSteps = stepsToRecord)
            Log.d(TAG, "Session recorded to DB: %.1fm / ${stepsToRecord} steps".format(distToRecord))
        }
        // Reset so we don't double-count on the next flush
        lastFlushedSteps    = sessionSteps
        lastFlushedDistance = sessionDistance
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /** Flush step delta only (called on every step sensor event). */
    private fun flushDeltaToDb(force: Boolean = false) {
        val deltaSteps = sessionSteps - lastFlushedSteps
        if (!force && deltaSteps < 5) return
        lastFlushedSteps = sessionSteps
        scope.launch { statsRepo.addSteps(deltaSteps) }
    }

    /** Flush distance delta only (called on distance sensor event). */
    private fun flushDistanceDeltaToDb(force: Boolean = false) {
        val deltaDistance = sessionDistance - lastFlushedDistance
        if (!force && deltaDistance < 2.0) return
        lastFlushedDistance = sessionDistance
        scope.launch { statsRepo.addDistance(deltaDistance) }
    }
}


