package com.example.lakbaylaya.ui.screens.map.navigation.pedometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Android sensor-based implementation of StepDetector
 *
 * Uses the device's step counter sensor to detect steps and calculate
 * distance based on average step length.
 *
 * Falls back to accelerometer-based step detection if step counter
 * is not available.
 */
class AndroidStepDetector(
    private val context: Context
) : StepDetector, SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private var isRunning = false
    private var stepCount = 0
    private var initialStepCount = -1  // For step counter sensor offset

    // Average step length in meters (can be calibrated)
    private var stepLengthMeters = 0.762

    // Callbacks
    private var onStepDetectedCallback: ((Int) -> Unit)? = null
    private var onDistanceUpdatedCallback: ((Double) -> Unit)? = null

    // Accelerometer-based step detection variables
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var lastStepTime = 0L
    private val stepThreshold = 10f  // Acceleration threshold for step detection
    private val minStepInterval = 250L  // Minimum time between steps (ms)

    companion object {
        private const val TAG = "AndroidStepDetector"
    }

    init {
        // Try to get step counter sensor (more accurate)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Fallback to accelerometer if step counter not available
        if (stepCounterSensor == null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Log.w(TAG, "Step counter sensor not available, using accelerometer")
        } else {
            Log.d(TAG, "Using step counter sensor")
        }
    }

    /**
     * Start step detection using available sensors
     */
    override fun start(
        onStepDetected: (Int) -> Unit,
        onDistanceUpdated: (Double) -> Unit
    ) {
        if (isRunning) {
            Log.w(TAG, "Step detector already running")
            return
        }

        onStepDetectedCallback = onStepDetected
        onDistanceUpdatedCallback = onDistanceUpdated

        // Try step counter first
        stepCounterSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            if (registered) {
                isRunning = true
                Log.d(TAG, "Step counter sensor registered")
                return
            }
        }

        // Fall back to accelerometer
        accelerometerSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            if (registered) {
                isRunning = true
                currentAcceleration = SensorManager.GRAVITY_EARTH
                lastAcceleration = SensorManager.GRAVITY_EARTH
                Log.d(TAG, "Accelerometer sensor registered")
                return
            }
        }

        Log.e(TAG, "No sensors available for step detection")
    }

    /**
     * Stop step detection
     */
    override fun stop() {
        if (!isRunning) return

        sensorManager.unregisterListener(this)
        isRunning = false
        Log.d(TAG, "Step detector stopped")
    }

    /**
     * Reset step count and distance
     */
    override fun reset() {
        stepCount = 0
        initialStepCount = -1
        lastStepTime = 0L
        Log.d(TAG, "Step detector reset")
    }

    /**
     * Get current step count
     */
    override fun getStepCount(): Int = stepCount

    /**
     * Get current distance in meters
     */
    override fun getDistance(): Double = stepCount * stepLengthMeters

    /**
     * Check if detector is active
     */
    override fun isActive(): Boolean = isRunning

    /**
     * Set custom step length for distance calculation
     */
    override fun setStepLength(lengthMeters: Double) {
        stepLengthMeters = lengthMeters.coerceIn(0.3, 1.5)  // Reasonable bounds
    }

    /**
     * Handle sensor events
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    /**
     * Handle step counter sensor data (more accurate)
     */
    private fun handleStepCounter(event: SensorEvent) {
        val totalSteps = event.values[0].toInt()

        // Initialize offset on first reading
        if (initialStepCount < 0) {
            initialStepCount = totalSteps
            stepCount = 0
        } else {
            // Calculate steps since start
            val newStepCount = totalSteps - initialStepCount

            // Only update if steps increased
            if (newStepCount > stepCount) {
                stepCount = newStepCount
                val distance = getDistance()

                onStepDetectedCallback?.invoke(stepCount)
                onDistanceUpdatedCallback?.invoke(distance)

                Log.d(TAG, "Step detected: $stepCount, Distance: ${"%.2f".format(distance)}m")
            }
        }
    }

    /**
     * Handle accelerometer sensor data (fallback method)
     *
     * Detects steps by analyzing acceleration changes.
     * Less accurate than step counter but works on all devices.
     */
    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate magnitude of acceleration
        lastAcceleration = currentAcceleration
        currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val delta = currentAcceleration - lastAcceleration

        // Detect step if acceleration change exceeds threshold
        val currentTime = System.currentTimeMillis()
        if (kotlin.math.abs(delta) > stepThreshold &&
            (currentTime - lastStepTime) > minStepInterval) {

            stepCount++
            lastStepTime = currentTime
            val distance = getDistance()

            onStepDetectedCallback?.invoke(stepCount)
            onDistanceUpdatedCallback?.invoke(distance)

            Log.d(TAG, "Step detected (accel): $stepCount, Distance: ${"%.2f".format(distance)}m")
        }
    }

    /**
     * Handle sensor accuracy changes (optional)
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step detection
    }
}

