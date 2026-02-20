package com.example.lakbaylaya.ui.screens.map.navigation.voice

import android.app.Application
import android.util.Log
import com.example.lakbaylaya.voice.VoiceManager
import java.util.Locale
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

private const val TAG = "NavVoiceManager"

/**
 * Manager for navigation voice command system
 * Handles TTS feedback and voice command processing during navigation
 *
 * Coordinates:
 * - Voice recognition setup and result handling
 * - TTS feedback for all actions
 * - Voice command processing and execution
 */
class NavigationVoiceManager(
    private val application: Application,
    private val commandHandler: VoiceCommandHandler
) {
    private val voiceManager by lazy { VoiceManager(application) }
    private val commandProcessor = NavigationVoiceCommandProcessor()

    private fun vibrateShort() {
        try {
            val vibrator = application.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            120,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(120)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Speak an initial "I'm listening" prompt when voice is activated
     */
    fun speakListeningPrompt() {
        try {
            voiceManager.speak("I'm listening for commands. Say repeat, pause, resume, distance, or emergency")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to speak listening prompt: ${e.message}")
        }
    }

    /**
     * Process voice input and execute command
     *
     * @param transcript Raw voice transcript from speech recognizer
     * @return The recognized command
     */
    fun processVoiceCommand(
        transcript: String,
        currentLocationLat: Double = 0.0,
        currentLocationLng: Double = 0.0,
        distanceToDestination: Double = 0.0
    ): VoiceCommand {
        Log.d(TAG, "Processing voice input: '$transcript'")

        val command = commandProcessor.processVoiceInput(transcript)

        // Speak the action feedback
        try {
            voiceManager.speak(command.ttsFeedback)
            // Haptic confirmation for recognized command or unknown feedback
            vibrateShort()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to speak feedback for command ${command.commandName}: ${e.message}")
        }

        // Execute the command
        executeCommand(command, currentLocationLat, currentLocationLng, distanceToDestination)

        return command
    }

    /**
     * Execute the voice command
     */
    private fun executeCommand(
        command: VoiceCommand,
        latitude: Double,
        longitude: Double,
        distanceMeters: Double
    ) {
        when (command) {
            is VoiceCommand.RepeatInstruction -> {
                Log.d(TAG, "Executing: Repeat instruction")
                commandHandler.onRepeatInstruction()
            }

            is VoiceCommand.PauseNavigation -> {
                Log.d(TAG, "Executing: Pause navigation")
                commandHandler.onPauseNavigation()
            }

            is VoiceCommand.ResumeNavigation -> {
                Log.d(TAG, "Executing: Resume navigation")
                commandHandler.onResumeNavigation()
            }

            is VoiceCommand.CheckCurrentPosition -> {
                Log.d(TAG, "Executing: Check current position")
                commandHandler.onCheckCurrentPosition(latitude, longitude)

                // Speak position feedback with coordinates
                try {
                    val positionMessage = String.format(
                        Locale.US,
                        "Your position: latitude %.4f, longitude %.4f",
                        latitude,
                        longitude
                    )
                    voiceManager.speak(positionMessage)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to speak position: ${e.message}")
                }
            }

            is VoiceCommand.DistanceToDestination -> {
                Log.d(TAG, "Executing: Distance to destination")
                commandHandler.onDistanceToDestination(distanceMeters)

                // Speak distance feedback
                try {
                    val distanceMessage = if (distanceMeters >= 1000) {
                        String.format(
                            Locale.US,
                            "%.1f kilometers to destination",
                            distanceMeters / 1000
                        )
                    } else {
                        String.format(Locale.US, "%.0f meters to destination", distanceMeters)
                    }
                    voiceManager.speak(distanceMessage)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to speak distance: ${e.message}")
                }
            }

            is VoiceCommand.SwitchRoute -> {
                Log.d(TAG, "Executing: Switch route")
                commandHandler.onSwitchRoute()
                try {
                    voiceManager.speak("Switched to alternative route")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to speak route switch feedback: ${e.message}")
                }
            }

            is VoiceCommand.CancelNavigation -> {
                Log.d(TAG, "Executing: Cancel navigation")
                commandHandler.onCancelNavigation()
                try {
                    voiceManager.speak("Navigation cancelled")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to speak cancel feedback: ${e.message}")
                }
            }

            is VoiceCommand.ActivateEmergencyMode -> {
                Log.d(TAG, "Executing: Activate emergency mode")
                commandHandler.onActivateEmergencyMode()
                try {
                    voiceManager.speak("Emergency mode activated. Sending help request")
                    vibrateShort()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to speak emergency feedback: ${e.message}")
                }
            }

            is VoiceCommand.UnknownCommand -> {
                Log.w(TAG, "Unknown command - no action taken")
                // give a short haptic to signal unknown
                vibrateShort()
            }
        }
    }

    /**
     * Speak a generic message
     */
    fun speak(message: String) {
        try {
            voiceManager.speak(message)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to speak: ${e.message}")
        }
    }

    /**
     * Stop any ongoing speech
     */
    fun stop() {
        try {
            voiceManager.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop speech: ${e.message}")
        }
    }

    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean {
        return try {
            voiceManager.isSpeaking()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check speaking status: ${e.message}")
            false
        }
    }

    /**
     * Shutdown voice manager and release resources
     */
    fun shutdown() {
        try {
            voiceManager.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to shutdown: ${e.message}")
        }
    }

    /**
     * Get suggested voice commands for user guidance
     */
    fun getSuggestedCommands(): List<String> {
        return commandProcessor.getSuggestedCommands()
    }
}
