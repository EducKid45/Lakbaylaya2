package com.example.lakbaylaya.ui.screens.map.navigation.pedometer

/**
 * Interface for step detection and distance tracking
 *
 * Provides abstraction over step counting implementation,
 * allowing for testing and alternative implementations.
 */
interface StepDetector {

    /**
     * Start detecting steps
     *
     * @param onStepDetected Callback invoked when a step is detected
     * @param onDistanceUpdated Callback invoked when distance is updated (in meters)
     */
    fun start(
        onStepDetected: (stepCount: Int) -> Unit = {},
        onDistanceUpdated: (distanceMeters: Double) -> Unit = {}
    )

    /**
     * Stop detecting steps
     */
    fun stop()

    /**
     * Reset step count and distance to zero
     */
    fun reset()

    /**
     * Get current step count since last reset
     */
    fun getStepCount(): Int

    /**
     * Get current distance covered since last reset (in meters)
     */
    fun getDistance(): Double

    /**
     * Check if detector is currently active
     */
    fun isActive(): Boolean

    /**
     * Set average step length for distance calculation
     *
     * @param lengthMeters Average step length in meters (default ~0.762m)
     */
    fun setStepLength(lengthMeters: Double)
}

