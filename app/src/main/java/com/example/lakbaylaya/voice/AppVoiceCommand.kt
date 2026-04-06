package com.example.lakbaylaya.voice

import com.example.lakbaylaya.ui.navigationbars.nav.NavRoutes

/**
 * Sealed hierarchy of all app-wide voice commands understood by the
 * GlobalVoiceViewModel / AppVoiceCommandProcessor.
 *
 * Two categories:
 *  1. [NavigationCommand] – jump to a screen, respects backstack.
 *  2. [ActionCommand]     – perform a feature action without navigating.
 *  3. [UnknownCommand]    – returned when no pattern matched.
 */
sealed class AppVoiceCommand {
    abstract val commandName: String
    abstract val ttsFeedback: String
    /** Canonical spoken phrases that trigger this command (lowercase). */
    abstract val triggers: List<String>

    // ── Navigation commands ──────────────────────────────────────────────────

    sealed class NavigationCommand : AppVoiceCommand() {
        abstract val targetRoute: String

        data object GoHome : NavigationCommand() {
            override val commandName = "Go to Home"
            override val ttsFeedback = "Navigating to Home"
            override val targetRoute = NavRoutes.Home.route
            override val triggers = listOf(
                "go home", "home screen", "open home", "home", "go to home",
                "main screen", "main page"
            )
        }

        data object GoMap : NavigationCommand() {
            override val commandName = "Go to Map"
            override val ttsFeedback = "Navigating to Map"
            override val targetRoute = NavRoutes.Map.route
            override val triggers = listOf(
                "go to map", "open map", "show map", "map", "navigate map",
                "start map", "open navigation"
            )
        }

        data object GoRoutes : NavigationCommand() {
            override val commandName = "Go to Routes"
            override val ttsFeedback = "Opening your saved routes"
            override val targetRoute = NavRoutes.Route.route
            override val triggers = listOf(
                "go to routes", "open routes", "show routes", "routes",
                "my routes", "saved routes", "familiar routes", "show familiar routes"
            )
        }

        data object GoProfile : NavigationCommand() {
            override val commandName = "Go to Profile"
            override val ttsFeedback = "Opening your profile"
            override val targetRoute = NavRoutes.Profile.route
            override val triggers = listOf(
                "go to profile", "open profile", "my profile", "profile",
                "show profile", "account"
            )
        }

        data object GoSettings : NavigationCommand() {
            override val commandName = "Go to Settings"
            override val ttsFeedback = "Opening settings"
            override val targetRoute = NavRoutes.Settings.route
            override val triggers = listOf(
                "go to settings", "open settings", "settings", "preferences",
                "show settings"
            )
        }
    }

    // ── Feature-action commands ──────────────────────────────────────────────

    sealed class ActionCommand : AppVoiceCommand() {

        data object StopNavigation : ActionCommand() {
            override val commandName = "Stop Navigation"
            override val ttsFeedback = "Stopping navigation"
            override val triggers = listOf(
                "stop navigation", "cancel navigation", "end navigation",
                "stop navigating", "cancel route"
            )
        }

        data object SendEmergency : ActionCommand() {
            override val commandName = "Send Emergency Alert"
            override val ttsFeedback = "Sending emergency alert now"
            override val triggers = listOf(
                "send emergency", "emergency", "help me", "send help",
                "call for help", "i need help", "emergency alert"
            )
        }

        data object CallEmergencyContact : ActionCommand() {
            override val commandName = "Call Emergency Contact"
            override val ttsFeedback = "Calling your emergency contact"
            override val triggers = listOf(
                "call emergency contact", "call contact", "call my contact",
                "call emergency", "emergency call"
            )
        }

        data object CheckDeviceStatus : ActionCommand() {
            override val commandName = "Check Device Status"
            override val ttsFeedback = "Checking device status"
            override val triggers = listOf(
                "check device status", "device status", "status",
                "check status", "how is my device"
            )
        }

        data object CheckBluetooth : ActionCommand() {
            override val commandName = "Check Bluetooth"
            override val ttsFeedback = "Opening Bluetooth device list"
            override val triggers = listOf(
                "check bluetooth", "bluetooth", "open bluetooth",
                "connect device", "bluetooth devices"
            )
        }

        data object RepeatLastInstruction : ActionCommand() {
            override val commandName = "Repeat Last Instruction"
            override val ttsFeedback = "Repeating last instruction"
            override val triggers = listOf(
                "repeat", "say again", "repeat instruction",
                "repeat last", "again"
            )
        }

        data object StopListening : ActionCommand() {
            override val commandName = "Stop Listening"
            override val ttsFeedback = "Voice commands stopped"
            override val triggers = listOf(
                "stop listening", "stop voice", "disable voice",
                "turn off voice", "quiet"
            )
        }

        /**
         * Triggers the multi-turn navigation dialog.
         * Covers all natural navigation phrases including "navigate", "start navigation", etc.
         */
        data object NavigateDialog : ActionCommand() {
            override val commandName = "Navigate Dialog"
            override val ttsFeedback = "" // dialog engine provides its own TTS
            override val triggers = listOf(
                // navigation start phrases
                "start navigation", "begin navigation", "start route", "start navigating",
                // natural navigate phrases
                "navigate", "i want to navigate", "let's navigate",
                // search / directions phrases
                "navigate to a place", "i want to go somewhere", "find me a place",
                "take me somewhere", "search for a place",
                "i need directions", "get directions", "find directions",
                // ── saved route / saved location — correct spellings ──────────
                "use saved route", "pick a route", "open saved route",
                "saved route", "saved routes", "my routes", "my saved routes",
                "saved location", "saved locations", "my saved location",
                "use my route", "use a saved route", "use saved location",
                "my location", "my place", "saved place", "saved places",
                // ── typo / mishear variants — missing 'd' in "saved" ─────────
                "save route", "save routes", "save location", "save locations",
                "save place", "save places", "my save route", "my save location",
                "use save route", "use save location",
                // ── typo — missing 'd' in "saved" + other variations ─────────
                "saves route", "saves location", "saves place",
                // ── typo — missing letters in "location" ─────────────────────
                "saved locaion", "saved locaton", "saved locatin", "saved locatn",
                "saved lacation", "saved loacation", "save locaion", "save locaton",
                // ── typo — missing letters in "route" ────────────────────────
                "saved rout", "save rout", "my rout",
                // ── typo — missing letters in "place" ────────────────────────
                "saved plac", "save plac", "my plac",
                // ── speech recognition common mishears ────────────────────────
                "same location", "same route", "same place",
                "safe route", "safe location", "say route", "say location"
            )
        }
    }

    // ── Unknown fallback ─────────────────────────────────────────────────────

    data object UnknownCommand : AppVoiceCommand() {
        override val commandName = "Unknown Command"
        override val ttsFeedback = "Sorry, I didn't understand. Please repeat your command."
        override val triggers = emptyList<String>()
    }

    // ── All built-in commands list (used by processor & docs) ────────────────
    companion object {
        val ALL_BUILTIN: List<AppVoiceCommand> = listOf(
            NavigationCommand.GoHome,
            NavigationCommand.GoMap,
            NavigationCommand.GoRoutes,
            NavigationCommand.GoProfile,
            NavigationCommand.GoSettings,
            ActionCommand.NavigateDialog,      // handles all navigation + search dialog flow
            ActionCommand.StopNavigation,
            ActionCommand.SendEmergency,
            ActionCommand.CallEmergencyContact,
            ActionCommand.CheckDeviceStatus,
            ActionCommand.CheckBluetooth,
            ActionCommand.RepeatLastInstruction,
            ActionCommand.StopListening,
        )
    }
}

