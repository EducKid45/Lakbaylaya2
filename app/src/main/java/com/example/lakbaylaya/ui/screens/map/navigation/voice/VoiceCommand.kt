package com.example.lakbaylaya.ui.screens.map.navigation.voice

/**
 * Sealed class representing voice commands during navigation
 * Each command has associated TTS feedback and action handler
 */
sealed class VoiceCommand {
    abstract val commandName: String
    abstract val ttsFeedback: String

    object RepeatInstruction : VoiceCommand() {
        override val commandName = "Repeat instruction"
        override val ttsFeedback = "Repeating instruction"
    }

    object PauseNavigation : VoiceCommand() {
        override val commandName = "Pause navigation"
        override val ttsFeedback = "Navigation paused"
    }

    object ResumeNavigation : VoiceCommand() {
        override val commandName = "Resume navigation"
        override val ttsFeedback = "Resuming navigation"
    }

    object CheckCurrentPosition : VoiceCommand() {
        override val commandName = "Check current position"
        override val ttsFeedback = "Checking your position"
    }

    object DistanceToDestination : VoiceCommand() {
        override val commandName = "Distance to destination"
        override val ttsFeedback = "Checking distance to destination"
    }

    object SwitchRoute : VoiceCommand() {
        override val commandName = "Switch route"
        override val ttsFeedback = "Switching to alternative route"
    }

    object CancelNavigation : VoiceCommand() {
        override val commandName = "Cancel navigation"
        override val ttsFeedback = "Canceling navigation"
    }

    object ActivateEmergencyMode : VoiceCommand() {
        override val commandName = "Activate emergency mode"
        override val ttsFeedback = "Activating emergency mode"
    }

    object UnknownCommand : VoiceCommand() {
        override val commandName = "Unknown command"
        override val ttsFeedback = "Sorry, I didn't understand that command"
    }
}

/**
 * Interface for handling voice command actions
 */
interface VoiceCommandHandler {
    fun onRepeatInstruction()
    fun onPauseNavigation()
    fun onResumeNavigation()
    fun onCheckCurrentPosition(latitude: Double, longitude: Double)
    fun onDistanceToDestination(distanceMeters: Double)
    fun onSwitchRoute()
    fun onCancelNavigation()
    fun onActivateEmergencyMode()
}

