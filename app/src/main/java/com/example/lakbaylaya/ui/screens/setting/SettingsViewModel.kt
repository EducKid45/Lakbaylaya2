package com.example.lakbaylaya.ui.screens.setting

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================
// Voice & Audio Settings
// ============================================

data class VoiceAudioSettings(
    val voiceGuidanceEnabled: Boolean = true,
    val voiceSpeed: VoiceSpeed = VoiceSpeed.NORMAL,
    val voiceVolume: Int = 80, // 0-100
    val selectedLanguage: Language = Language.ENGLISH
)

enum class VoiceSpeed(val displayName: String, val multiplier: Float) {
    SLOW("Slow", 0.75f),
    NORMAL("Normal", 1.0f),
    FAST("Fast", 1.25f)
}

enum class Language(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    FILIPINO("Filipino", "fil"),
    TAGALOG("Tagalog", "tl"),
    CEBUANO("Cebuano", "ceb")
}

// ============================================
// Vibration & Haptic Settings
// ============================================

data class VibrationSettings(
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: VibrationStrength = VibrationStrength.MEDIUM,
    val normalPathPattern: VibrationPattern = VibrationPattern.SHORT_PULSE,
    val difficultPathPattern: VibrationPattern = VibrationPattern.DOUBLE_PULSE,
    val arrivalPattern: VibrationPattern = VibrationPattern.LONG_VIBRATION
)

enum class VibrationStrength(val displayName: String, val intensity: Int) {
    LIGHT("Light", 50),
    MEDIUM("Medium", 128),
    STRONG("Strong", 200),
    MAXIMUM("Maximum", 255)
}

enum class VibrationPattern(val displayName: String, val description: String) {
    SHORT_PULSE("Short Pulse", "Quick single vibration"),
    DOUBLE_PULSE("Double Pulse", "Two quick vibrations"),
    LONG_VIBRATION("Long Vibration", "Extended vibration"),
    PATTERN_WAVE("Wave Pattern", "Increasing then decreasing"),
    RAPID_PULSES("Rapid Pulses", "Multiple quick vibrations")
}

// ============================================
// Navigation Preferences
// ============================================

data class NavigationPreferences(
    val defaultWalkingMode: Boolean = true,
    val autoCameraOrientation: Boolean = true,
    val autoReroute: Boolean = true,
    val routePreviewEnabled: Boolean = true
)

// ============================================
// Safety & Emergency Settings
// ============================================

data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String = ""
)

data class SafetySettings(
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val emergencyMessage: String = "I need help. Please call me or send assistance to my location.",
    val autoSendArrivalNotification: Boolean = true
)

// ============================================
// Device & Connectivity Settings
// ============================================

data class DeviceSettings(
    val pairedDeviceName: String = "",
    val isDeviceConnected: Boolean = false,
    val deviceBatteryLevel: Int? = null
)

// ============================================
// App & Data Settings
// ============================================

data class AppDataSettings(
    val routeHistoryCount: Int = 0,
    val familiarRoutesCount: Int = 0,
    val offlineMapsDownloaded: Boolean = false,
    val appVersion: String = "1.0.0",
    val lastUpdateCheck: String = ""
)

// ============================================
// Complete Settings State
// ============================================

data class SettingsUiState(
    val voiceAudioSettings: VoiceAudioSettings = VoiceAudioSettings(),
    val vibrationSettings: VibrationSettings = VibrationSettings(),
    val navigationPreferences: NavigationPreferences = NavigationPreferences(),
    val safetySettings: SafetySettings = SafetySettings(),
    val deviceSettings: DeviceSettings = DeviceSettings(),
    val appDataSettings: AppDataSettings = AppDataSettings(),

    // Dialog states
    val isEditingEmergencyContacts: Boolean = false,
    val isEditingEmergencyMessage: Boolean = false,
    val isPairingDevice: Boolean = false,
    val isTestingVibration: Boolean = false,
    val showClearHistoryConfirmation: Boolean = false,
    val showResetRoutesConfirmation: Boolean = false,

    // Feedback
    val feedbackMessage: String? = null
)

// ============================================
// ViewModel
// ============================================

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // Load sample/default settings
        _uiState.value = SettingsUiState(
            voiceAudioSettings = VoiceAudioSettings(
                voiceGuidanceEnabled = true,
                voiceSpeed = VoiceSpeed.NORMAL,
                voiceVolume = 80,
                selectedLanguage = Language.ENGLISH
            ),
            vibrationSettings = VibrationSettings(
                vibrationEnabled = true,
                vibrationStrength = VibrationStrength.MEDIUM,
                normalPathPattern = VibrationPattern.SHORT_PULSE,
                difficultPathPattern = VibrationPattern.DOUBLE_PULSE,
                arrivalPattern = VibrationPattern.LONG_VIBRATION
            ),
            navigationPreferences = NavigationPreferences(
                defaultWalkingMode = true,
                autoCameraOrientation = true,
                autoReroute = true,
                routePreviewEnabled = true
            ),
            safetySettings = SafetySettings(
                emergencyContacts = listOf(
                    EmergencyContact(
                        id = "1",
                        name = "Maria Dela Cruz",
                        phoneNumber = "+63 912 345 6789",
                        relationship = "Mother"
                    )
                ),
                emergencyMessage = "I need help. Please call me or send assistance to my location.",
                autoSendArrivalNotification = true
            ),
            deviceSettings = DeviceSettings(
                pairedDeviceName = "LakbayLaya Band",
                isDeviceConnected = true,
                deviceBatteryLevel = 75
            ),
            appDataSettings = AppDataSettings(
                routeHistoryCount = 24,
                familiarRoutesCount = 5,
                offlineMapsDownloaded = false,
                appVersion = "1.0.0",
                lastUpdateCheck = "January 25, 2026"
            )
        )
    }

    // ============================================
    // Voice & Audio Settings Actions
    // ============================================

    fun toggleVoiceGuidance() {
        val current = _uiState.value.voiceAudioSettings.voiceGuidanceEnabled
        _uiState.value = _uiState.value.copy(
            voiceAudioSettings = _uiState.value.voiceAudioSettings.copy(voiceGuidanceEnabled = !current),
            feedbackMessage = if (!current) "Voice guidance enabled" else "Voice guidance disabled"
        )
    }

    fun setVoiceSpeed(speed: VoiceSpeed) {
        _uiState.value = _uiState.value.copy(
            voiceAudioSettings = _uiState.value.voiceAudioSettings.copy(voiceSpeed = speed),
            feedbackMessage = "Voice speed set to ${speed.displayName}"
        )
    }

    fun setVoiceVolume(volume: Int) {
        _uiState.value = _uiState.value.copy(
            voiceAudioSettings = _uiState.value.voiceAudioSettings.copy(voiceVolume = volume)
        )
    }

    fun setLanguage(language: Language) {
        _uiState.value = _uiState.value.copy(
            voiceAudioSettings = _uiState.value.voiceAudioSettings.copy(selectedLanguage = language),
            feedbackMessage = "Language set to ${language.displayName}"
        )
    }

    // ============================================
    // Vibration Settings Actions
    // ============================================

    fun toggleVibration() {
        val current = _uiState.value.vibrationSettings.vibrationEnabled
        _uiState.value = _uiState.value.copy(
            vibrationSettings = _uiState.value.vibrationSettings.copy(vibrationEnabled = !current),
            feedbackMessage = if (!current) "Vibration enabled" else "Vibration disabled"
        )
    }

    fun setVibrationStrength(strength: VibrationStrength) {
        _uiState.value = _uiState.value.copy(
            vibrationSettings = _uiState.value.vibrationSettings.copy(vibrationStrength = strength),
            feedbackMessage = "Vibration strength set to ${strength.displayName}"
        )
    }

    fun setNormalPathPattern(pattern: VibrationPattern) {
        _uiState.value = _uiState.value.copy(
            vibrationSettings = _uiState.value.vibrationSettings.copy(normalPathPattern = pattern),
            feedbackMessage = "Normal path vibration set to ${pattern.displayName}"
        )
    }

    fun setDifficultPathPattern(pattern: VibrationPattern) {
        _uiState.value = _uiState.value.copy(
            vibrationSettings = _uiState.value.vibrationSettings.copy(difficultPathPattern = pattern),
            feedbackMessage = "Difficult path vibration set to ${pattern.displayName}"
        )
    }

    fun setArrivalPattern(pattern: VibrationPattern) {
        _uiState.value = _uiState.value.copy(
            vibrationSettings = _uiState.value.vibrationSettings.copy(arrivalPattern = pattern),
            feedbackMessage = "Arrival vibration set to ${pattern.displayName}"
        )
    }

    fun testVibration() {
        _uiState.value = _uiState.value.copy(
            isTestingVibration = true,
            feedbackMessage = "Testing vibration..."
        )
        // TODO: Trigger actual vibration
        _uiState.value = _uiState.value.copy(isTestingVibration = false)
    }

    // ============================================
    // Navigation Preferences Actions
    // ============================================

    fun toggleWalkingMode() {
        val current = _uiState.value.navigationPreferences.defaultWalkingMode
        _uiState.value = _uiState.value.copy(
            navigationPreferences = _uiState.value.navigationPreferences.copy(defaultWalkingMode = !current),
            feedbackMessage = if (!current) "Walking mode enabled" else "Walking mode disabled"
        )
    }

    fun toggleAutoCameraOrientation() {
        val current = _uiState.value.navigationPreferences.autoCameraOrientation
        _uiState.value = _uiState.value.copy(
            navigationPreferences = _uiState.value.navigationPreferences.copy(autoCameraOrientation = !current),
            feedbackMessage = if (!current) "Auto camera orientation enabled" else "Auto camera orientation disabled"
        )
    }

    fun toggleAutoReroute() {
        val current = _uiState.value.navigationPreferences.autoReroute
        _uiState.value = _uiState.value.copy(
            navigationPreferences = _uiState.value.navigationPreferences.copy(autoReroute = !current),
            feedbackMessage = if (!current) "Auto reroute enabled" else "Auto reroute disabled"
        )
    }

    fun toggleRoutePreview() {
        val current = _uiState.value.navigationPreferences.routePreviewEnabled
        _uiState.value = _uiState.value.copy(
            navigationPreferences = _uiState.value.navigationPreferences.copy(routePreviewEnabled = !current),
            feedbackMessage = if (!current) "Route preview enabled" else "Route preview disabled"
        )
    }

    // ============================================
    // Safety & Emergency Actions
    // ============================================

    fun showEditEmergencyContacts() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = true)
    }

    fun hideEditEmergencyContacts() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = false)
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String) {
        val newContact = EmergencyContact(
            id = System.currentTimeMillis().toString(),
            name = name,
            phoneNumber = phone,
            relationship = relationship
        )
        val updatedContacts = _uiState.value.safetySettings.emergencyContacts + newContact
        _uiState.value = _uiState.value.copy(
            safetySettings = _uiState.value.safetySettings.copy(emergencyContacts = updatedContacts),
            feedbackMessage = "Emergency contact added"
        )
    }

    fun removeEmergencyContact(contactId: String) {
        val updatedContacts =
            _uiState.value.safetySettings.emergencyContacts.filter { it.id != contactId }
        _uiState.value = _uiState.value.copy(
            safetySettings = _uiState.value.safetySettings.copy(emergencyContacts = updatedContacts),
            feedbackMessage = "Emergency contact removed"
        )
    }

    fun showEditEmergencyMessage() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = true)
    }

    fun hideEditEmergencyMessage() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = false)
    }

    fun updateEmergencyMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            safetySettings = _uiState.value.safetySettings.copy(emergencyMessage = message),
            isEditingEmergencyMessage = false,
            feedbackMessage = "Emergency message updated"
        )
    }

    fun toggleAutoArrivalNotification() {
        val current = _uiState.value.safetySettings.autoSendArrivalNotification
        _uiState.value = _uiState.value.copy(
            safetySettings = _uiState.value.safetySettings.copy(autoSendArrivalNotification = !current),
            feedbackMessage = if (!current) "Auto-send arrival notification enabled" else "Auto-send arrival notification disabled"
        )
    }

    // ============================================
    // Device & Connectivity Actions
    // ============================================

    fun startPairingDevice() {
        _uiState.value = _uiState.value.copy(
            isPairingDevice = true,
            feedbackMessage = "Searching for devices..."
        )
        // TODO: Implement actual Bluetooth pairing
    }

    fun stopPairingDevice() {
        _uiState.value = _uiState.value.copy(isPairingDevice = false)
    }

    fun reconnectDevice() {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Reconnecting device...")
        // TODO: Implement actual Bluetooth reconnection
        _uiState.value = _uiState.value.copy(
            deviceSettings = _uiState.value.deviceSettings.copy(isDeviceConnected = true),
            feedbackMessage = "Device reconnected successfully"
        )
    }

    // ============================================
    // App & Data Actions
    // ============================================

    fun showClearHistoryConfirmation() {
        _uiState.value = _uiState.value.copy(showClearHistoryConfirmation = true)
    }

    fun hideClearHistoryConfirmation() {
        _uiState.value = _uiState.value.copy(showClearHistoryConfirmation = false)
    }

    fun clearRouteHistory() {
        _uiState.value = _uiState.value.copy(
            appDataSettings = _uiState.value.appDataSettings.copy(routeHistoryCount = 0),
            showClearHistoryConfirmation = false,
            feedbackMessage = "Route history cleared"
        )
    }

    fun showResetRoutesConfirmation() {
        _uiState.value = _uiState.value.copy(showResetRoutesConfirmation = true)
    }

    fun hideResetRoutesConfirmation() {
        _uiState.value = _uiState.value.copy(showResetRoutesConfirmation = false)
    }

    fun resetFamiliarRoutes() {
        _uiState.value = _uiState.value.copy(
            appDataSettings = _uiState.value.appDataSettings.copy(familiarRoutesCount = 0),
            showResetRoutesConfirmation = false,
            feedbackMessage = "Familiar routes reset"
        )
    }

    fun downloadOfflineMaps() {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Downloading offline maps...")
        // TODO: Implement actual download
        _uiState.value = _uiState.value.copy(
            appDataSettings = _uiState.value.appDataSettings.copy(offlineMapsDownloaded = true),
            feedbackMessage = "Offline maps downloaded"
        )
    }

    fun checkForUpdates() {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Checking for updates...")
        // TODO: Implement actual update check
        _uiState.value = _uiState.value.copy(
            appDataSettings = _uiState.value.appDataSettings.copy(lastUpdateCheck = "January 26, 2026"),
            feedbackMessage = "App is up to date"
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    // ============================================
    // Voice Command Processing
    // ============================================

    fun executeVoiceCommand(command: String) {
        val normalizedCommand = command.lowercase().trim()

        when {
            // Voice & Audio
            normalizedCommand.contains("turn voice on") || normalizedCommand.contains("enable voice") -> {
                if (!_uiState.value.voiceAudioSettings.voiceGuidanceEnabled) toggleVoiceGuidance()
            }

            normalizedCommand.contains("turn voice off") || normalizedCommand.contains("disable voice") -> {
                if (_uiState.value.voiceAudioSettings.voiceGuidanceEnabled) toggleVoiceGuidance()
            }

            normalizedCommand.contains("change voice speed") || normalizedCommand.contains("voice speed") -> {
                val current = _uiState.value.voiceAudioSettings.voiceSpeed
                val next = when (current) {
                    VoiceSpeed.SLOW -> VoiceSpeed.NORMAL
                    VoiceSpeed.NORMAL -> VoiceSpeed.FAST
                    VoiceSpeed.FAST -> VoiceSpeed.SLOW
                }
                setVoiceSpeed(next)
            }

            // Vibration
            normalizedCommand.contains("test vibration") -> testVibration()
            normalizedCommand.contains("turn vibration on") || normalizedCommand.contains("enable vibration") -> {
                if (!_uiState.value.vibrationSettings.vibrationEnabled) toggleVibration()
            }

            normalizedCommand.contains("turn vibration off") || normalizedCommand.contains("disable vibration") -> {
                if (_uiState.value.vibrationSettings.vibrationEnabled) toggleVibration()
            }

            // Navigation
            normalizedCommand.contains("turn on auto orientation") || normalizedCommand.contains("enable auto orientation") -> {
                if (!_uiState.value.navigationPreferences.autoCameraOrientation) toggleAutoCameraOrientation()
            }

            normalizedCommand.contains("turn off auto orientation") || normalizedCommand.contains("disable auto orientation") -> {
                if (_uiState.value.navigationPreferences.autoCameraOrientation) toggleAutoCameraOrientation()
            }

            // Safety
            normalizedCommand.contains("edit emergency contacts") -> showEditEmergencyContacts()
            normalizedCommand.contains("change emergency message") -> showEditEmergencyMessage()

            // Device
            normalizedCommand.contains("reconnect device") -> reconnectDevice()
            normalizedCommand.contains("pair device") || normalizedCommand.contains("pair new device") -> startPairingDevice()

            // App & Data
            normalizedCommand.contains("clear route history") || normalizedCommand.contains("clear history") -> showClearHistoryConfirmation()
            normalizedCommand.contains("reset familiar routes") || normalizedCommand.contains("reset routes") -> showResetRoutesConfirmation()
            normalizedCommand.contains("download offline maps") || normalizedCommand.contains("download maps") -> downloadOfflineMaps()
            normalizedCommand.contains("check for updates") || normalizedCommand.contains("check updates") -> checkForUpdates()
        }
    }
}
