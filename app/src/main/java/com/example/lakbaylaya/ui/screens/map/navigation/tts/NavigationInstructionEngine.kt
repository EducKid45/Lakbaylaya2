@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.ui.screens.map.navigation.tts

import android.util.Log
import com.example.lakbaylaya.bluetooth.Esp32VibrationManager
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep

/**
 * NavigationInstructionEngine
 *
 * Central wiring point for turn-by-turn navigation:
 *  1. Receives a [DirectionStep] (already enriched with spokenInstruction + landmark).
 *  2. Speaks the instruction via [TextToSpeechEngine].
 *  3. Sends the matching vibration code + selected pattern to ESP32 via [Esp32VibrationManager].
 *
 * TTS and vibration are both gated:
 *  - [ttsEnabled]    → controls whether speech is produced
 *  - ESP32 haptic is gated inside [Esp32VibrationManager.hapticEnabled]
 */
class NavigationInstructionEngine(
    private val ttsEngine: TextToSpeechEngine,
    private val vibrationManager: Esp32VibrationManager
) {

    companion object {
        private const val TAG = "NavInstructionEngine"
    }

    /** When false, TTS is suppressed (muted navigation mode). */
    @Volatile var ttsEnabled: Boolean = true

    /**
     * Announce a navigation step and send the matching ESP32 vibration code.
     *
     * @param step       The enriched [DirectionStep].
     * @param useParsed  If true, prefer [DirectionStep.spokenInstruction]; fallback to raw instruction.
     * @param priority   Pass true to interrupt any currently playing speech.
     */
    fun announceStep(step: DirectionStep, useParsed: Boolean = true, priority: Boolean = true) {
        val textToSpeak = if (useParsed && step.spokenInstruction.isNotBlank()) {
            step.spokenInstruction
        } else {
            step.instruction
        }

        Log.d(TAG, "Announcing: $textToSpeak")

        if (ttsEnabled) ttsEngine.speak(textToSpeak, priority)

        val direction = simplifyTurn(step.bearingBefore, step.bearingAfter, step.instruction)
        vibrationManager.sendVibration(direction)
    }

    /** Announce plain text + optional vibration direction. */
    fun announce(text: String, vibrationDirection: String = "forward", priority: Boolean = false) {
        if (ttsEnabled) ttsEngine.speak(text, priority)
        vibrationManager.sendVibration(vibrationDirection)
    }

    /** Announce arrival and send the ARRIVAL ('A') vibration code. */
    fun announceArrival(destinationName: String = "your destination") {
        val text = "You have arrived at $destinationName."
        if (ttsEnabled) ttsEngine.speak(text, priority = true)
        vibrationManager.sendCode('A')
        Log.d(TAG, "Arrival: $text")
    }

    /** Announce a danger / caution warning. */
    fun announceDanger(warning: String) {
        if (ttsEnabled) ttsEngine.speak(warning, priority = true)
        vibrationManager.sendCode('D')
        Log.d(TAG, "Danger: $warning")
    }
}
