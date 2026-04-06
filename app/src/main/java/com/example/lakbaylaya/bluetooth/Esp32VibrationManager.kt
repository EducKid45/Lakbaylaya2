@file:Suppress("unused", "UNUSED_PARAMETER", "KDocUnresolvedReference")
package com.example.lakbaylaya.bluetooth

import android.util.Log

/**
 * Vibration codes sent to the ESP32 wearable via Bluetooth.
 *
 *  L → turn left
 *  R → turn right
 *  F → go forward / straight
 *  A → arrival / destination reached
 *  D → danger / caution
 */
object VibrationCode {
    const val LEFT    = 'L'
    const val RIGHT   = 'R'
    const val FORWARD = 'F'
    const val ARRIVAL = 'A'
    const val DANGER  = 'D'
}

/**
 * Maps a simplified turn label or navigation event to a
 * single-character vibration code and sends it to the connected ESP32 over the
 * existing [BluetoothManagerHelper] RFCOMM socket.
 *
 * Pattern selection is driven by the internal `patterns` map — updated whenever the user
 * changes vibration settings in the Settings screen.
 */
class Esp32VibrationManager(
    private val bluetoothHelper: BluetoothManagerHelper,
    private val socketWriter: Esp32SocketWriter
) {

    companion object {
        private const val TAG = "Esp32VibrationManager"

        // Singleton so navigation and settings both share one instance
        @Volatile private var instance: Esp32VibrationManager? = null

        fun getInstance(bluetoothHelper: BluetoothManagerHelper): Esp32VibrationManager =
            instance ?: synchronized(this) {
                instance ?: Esp32VibrationManager(bluetoothHelper, Esp32SocketWriter.getInstance())
                    .also { instance = it }
            }
    }

    // Whether haptic feedback is globally enabled (toggled via Settings screen)
    @Volatile var hapticEnabled: Boolean = true

    /**
     * Pattern map: direction → pattern name.
     * Updated from [com.example.lakbaylaya.ui.screens.setting.SettingsViewModel]
     * whenever the user changes a pattern.
     */
    private val patterns: MutableMap<Char, String> = mutableMapOf(
        VibrationCode.LEFT    to "DOUBLE_PULSE",
        VibrationCode.RIGHT   to "DOUBLE_PULSE",
        VibrationCode.FORWARD to "SHORT_PULSE",
        VibrationCode.ARRIVAL to "LONG_VIBRATION",
        VibrationCode.DANGER  to "RAPID_PULSES"
    )

    /** Called from Settings when the user picks a new pattern for a direction. */
    fun setPattern(code: Char, patternName: String) {
        patterns[code] = patternName
        Log.d(TAG, "Pattern set: '$code' -> $patternName")
    }

    /**
     * Convert a human-readable turn label into the matching vibration code.
     *
     * @param direction Any of: "turn left", "turn slightly left",
     *                           "turn right", "turn slightly right",
     *                           "go straight", "turn around",
     *                           "arrival", "danger"
     * @param patternOverride Optional pattern name to use instead of the stored one.
     */
    fun generateVibrationCode(direction: String, patternOverride: String? = null): Char {
        val d = direction.lowercase().trim()
        return when {
            d.contains("left")    -> VibrationCode.LEFT
            d.contains("right")   -> VibrationCode.RIGHT
            d.contains("arrival") || d.contains("arrive") || d.contains("destination") -> VibrationCode.ARRIVAL
            d.contains("danger")  || d.contains("caution") || d.contains("warning")    -> VibrationCode.DANGER
            else                  -> VibrationCode.FORWARD
        }
    }

    /**
     * Send a vibration command to ESP32 based on direction string.
     * Command format: "<CODE>:<PATTERN>\n"  (e.g. "R:DOUBLE_PULSE\n")
     * The ESP32 firmware parses this with a simple split on ':'.
     */
    fun sendVibration(direction: String, patternOverride: String? = null) {
        if (!hapticEnabled) {
            Log.d(TAG, "Haptic disabled — skipping vibration for '$direction'")
            return
        }

        if (!Esp32SocketWriter.getInstance().isAttached()) {
            Log.d(TAG, "No RFCOMM OutputStream attached — skipping vibration for '$direction'")
            return
        }

        val code = generateVibrationCode(direction, patternOverride)
        val pattern = patternOverride ?: patterns[code] ?: "SHORT_PULSE"
        socketWriter.write("$code:$pattern\n")
        Log.d(TAG, "Sent → code='$code' pattern='$pattern' direction='$direction'")
    }

    /**
     * Send a raw code+pattern directly — used by the test vibration dialog.
     * Returns true if the command was likely sent; false if skipped or failed.
     */
    fun sendCode(code: Char, patternOverride: String? = null): Boolean {
        if (!hapticEnabled) return false

        val writer = Esp32SocketWriter.getInstance()
        if (!writer.isAttached()) {
            Log.d(TAG, "No RFCOMM OutputStream attached — skipping code '$code'")
            return false
        }

        // Capture pre-write attachment state and attempt write.
        val wasAttached = writer.isAttached()
        val pattern = patternOverride ?: patterns[code] ?: "SHORT_PULSE"
        socketWriter.write("$code:$pattern\n")

        // If writer detached after write, a write error likely occurred.
        val stillAttached = writer.isAttached()
        val success = wasAttached && stillAttached
        if (success) {
            Log.d(TAG, "Sent code → '$code:$pattern'")
        } else {
            Log.d(TAG, "Failed to send code (detached during write) → '$code:$pattern'")
        }
        return success
    }
}
