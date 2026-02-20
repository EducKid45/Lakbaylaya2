package com.example.lakbaylaya.ui.screens.map.navigation.voice

import android.util.Log

private const val TAG = "NavVoiceCommandProcessor"

/**
 * Processor for navigation voice commands
 * Recognizes voice input and converts to actionable commands
 *
 * Supported commands:
 * - "repeat instruction", "repeat", "say again"
 * - "pause", "pause navigation"
 * - "resume", "resume navigation", "continue"
 * - "where am I", "current position", "my position"
 * - "distance", "how far", "distance to destination"
 * - "switch route", "alternate route", "different route"
 * - "cancel", "cancel navigation", "stop navigation"
 * - "emergency", "help", "activate emergency"
 */
class NavigationVoiceCommandProcessor {

    fun processVoiceInput(transcript: String): VoiceCommand {
        val normalized = transcript.lowercase().trim()

        // Token helpers
        fun containsAny(vararg probes: String) = probes.any { normalized.contains(it) }
        fun matchesWhole(vararg probes: String) = probes.any { normalized == it }

        // Improved recognition using token-based checks and common variants
        return when {
            // Repeat instruction commands
            containsAny(
                "repeat instruction",
                "repeat step",
                "repeat direction"
            ) || containsAny(
                "repeat",
                "say again",
                "again",
                "read again"
            ) && containsAny("instruction", "direction", "step") -> {
                Log.d(TAG, "Recognized: Repeat instruction")
                VoiceCommand.RepeatInstruction
            }
            // Short forms
            matchesWhole("repeat") || matchesWhole("say again") || normalized.endsWith("again") -> {
                Log.d(TAG, "Recognized: Repeat instruction (short form)")
                VoiceCommand.RepeatInstruction
            }

            // Pause navigation commands
            containsAny("pause navigation", "pause") || containsAny("hold on", "hold") -> {
                Log.d(TAG, "Recognized: Pause navigation")
                VoiceCommand.PauseNavigation
            }

            // Resume navigation commands
            containsAny(
                "resume navigation",
                "resume",
                "continue navigation",
                "continue",
                "go",
                "start navigation"
            ) -> {
                Log.d(TAG, "Recognized: Resume navigation")
                VoiceCommand.ResumeNavigation
            }

            // Check current position commands
            containsAny(
                "where am i",
                "where am i now",
                "where i am",
                "what is my location"
            ) || containsAny("current position", "my position", "my location") -> {
                Log.d(TAG, "Recognized: Check current position")
                VoiceCommand.CheckCurrentPosition
            }

            // Distance to destination commands
            containsAny(
                "distance to destination",
                "distance",
                "how far",
                "how far to",
                "how far is"
            ) || containsAny("how many meters", "meters to") -> {
                Log.d(TAG, "Recognized: Distance to destination")
                VoiceCommand.DistanceToDestination
            }

            // Switch route commands
            containsAny(
                "switch route",
                "alternate route",
                "different route",
                "another route",
                "other route"
            ) || (containsAny("switch", "change") && containsAny("route", "way")) -> {
                Log.d(TAG, "Recognized: Switch route")
                VoiceCommand.SwitchRoute
            }

            // Cancel navigation commands
            containsAny(
                "cancel navigation",
                "cancel",
                "stop navigation",
                "stop"
            ) || normalized.startsWith("stop") -> {
                Log.d(TAG, "Recognized: Cancel navigation")
                VoiceCommand.CancelNavigation
            }

            // Emergency mode commands
            containsAny(
                "emergency",
                "help",
                "activate emergency",
                "mayday",
                "sos"
            ) || containsAny("call for help", "send help") -> {
                Log.d(TAG, "Recognized: Activate emergency mode")
                VoiceCommand.ActivateEmergencyMode
            }

            else -> {
                Log.w(TAG, "Unknown command: '$normalized'")
                VoiceCommand.UnknownCommand
            }
        }
    }

    fun getSuggestedCommands(): List<String> {
        return listOf(
            "Repeat instruction",
            "Pause navigation",
            "Resume navigation",
            "Where am I",
            "Distance to destination",
            "Switch route",
            "Cancel navigation",
            "Emergency"
        )
    }
}
