package com.example.lakbaylaya.ui.screens.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User profile information
 */
data class UserProfile(
    val name: String = "",
    val homeLocation: String = "",
    val workLocation: String = ""
)

/**
 * Health and mobility statistics
 */
data class HealthStats(
    val distanceWalkedTodayKm: Double = 0.0,
    val distanceWalkedWeekKm: Double = 0.0,
    val stepsToday: Int = 0,
    val stepsWeek: Int = 0,
    val routesCompletedToday: Int = 0,
    val routesCompletedWeek: Int = 0
)

/**
 * Emergency contact information
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String = ""
)

/**
 * Emergency and safety settings
 */
data class EmergencySettings(
    val contacts: List<EmergencyContact> = emptyList(),
    val emergencyMessage: String = "I need help. Please call me or send assistance to my location.",
    val autoArrivalNotification: Boolean = true
)

/**
 * Connected wearable device information
 */
data class WearableDevice(
    val name: String = "",
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null // null if not available
)

/**
 * GPS status information
 */
enum class GpsStatus(val displayName: String) {
    READY("Ready"),
    SEARCHING("Searching"),
    UNAVAILABLE("Unavailable")
}

/**
 * Device status overview (read-only display)
 */
data class DeviceStatus(
    val wearableDevice: WearableDevice = WearableDevice(),
    val gpsStatus: GpsStatus = GpsStatus.READY
)

/**
 * Minimal profile settings (optional toggles)
 */
data class ProfileSettings(
    val voiceFeedbackForProgress: Boolean = true
)

/**
 * App preferences
 */
data class AppPreferences(
    val voiceSpeed: VoiceSpeed = VoiceSpeed.NORMAL,
    val vibrationStrength: VibrationStrength = VibrationStrength.MEDIUM,
    val mapStyle: MapStyle = MapStyle.AUTO
)

enum class VoiceSpeed(val displayName: String, val multiplier: Float) {
    SLOW("Slow", 0.75f),
    NORMAL("Normal", 1.0f),
    FAST("Fast", 1.25f),
    VERY_FAST("Very Fast", 1.5f)
}

enum class VibrationStrength(val displayName: String, val intensity: Int) {
    OFF("Off", 0),
    LIGHT("Light", 50),
    MEDIUM("Medium", 128),
    STRONG("Strong", 200),
    MAXIMUM("Maximum", 255)
}

enum class MapStyle(val displayName: String) {
    DAY("Day (Light)"),
    NIGHT("Night (Dark)"),
    AUTO("Auto (Follow System)")
}

/**
 * UI state for the Profile screen
 */
data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val healthStats: HealthStats = HealthStats(),
    val emergencySettings: EmergencySettings = EmergencySettings(),
    val wearableDevice: WearableDevice = WearableDevice(),
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val profileSettings: ProfileSettings = ProfileSettings(),
    val appPreferences: AppPreferences = AppPreferences(),
    val isEditingProfile: Boolean = false,
    val isEditingEmergencyContacts: Boolean = false,
    val isEditingEmergencyMessage: Boolean = false,
    val isReconnectingDevice: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * ViewModel for managing user profile and settings
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        _uiState.value = ProfileUiState(
            userProfile = UserProfile(
                name = "Juan Dela Cruz",
                homeLocation = "123 Main Street, Quezon City",
                workLocation = "University of the Philippines, Diliman"
            ),
            healthStats = HealthStats(
                distanceWalkedTodayKm = 2.3,
                distanceWalkedWeekKm = 15.7,
                stepsToday = 3200,
                stepsWeek = 22400,
                routesCompletedToday = 2,
                routesCompletedWeek = 12
            ),
            emergencySettings = EmergencySettings(
                contacts = listOf(
                    EmergencyContact(
                        id = "1",
                        name = "Maria Dela Cruz",
                        phoneNumber = "+63 912 345 6789",
                        relationship = "Mother"
                    ),
                    EmergencyContact(
                        id = "2",
                        name = "Jose Dela Cruz",
                        phoneNumber = "+63 923 456 7890",
                        relationship = "Father"
                    )
                ),
                emergencyMessage = "I need help. Please call me or send assistance to my location.",
                autoArrivalNotification = true
            ),
            wearableDevice = WearableDevice(
                name = "LakbayLaya Band",
                isConnected = true,
                batteryLevel = 85
            ),
            deviceStatus = DeviceStatus(
                wearableDevice = WearableDevice(
                    name = "LakbayLaya Band",
                    isConnected = true,
                    batteryLevel = 85
                ),
                gpsStatus = GpsStatus.READY
            ),
            profileSettings = ProfileSettings(
                voiceFeedbackForProgress = true
            ),
            appPreferences = AppPreferences(
                voiceSpeed = VoiceSpeed.NORMAL,
                vibrationStrength = VibrationStrength.MEDIUM,
                mapStyle = MapStyle.AUTO
            )
        )
    }

    // Profile editing
    fun showEditProfile() {
        _uiState.value = _uiState.value.copy(isEditingProfile = true)
    }

    fun hideEditProfile() {
        _uiState.value = _uiState.value.copy(isEditingProfile = false)
    }

    fun updateProfile(name: String, homeLocation: String, workLocation: String) {
        _uiState.value = _uiState.value.copy(
            userProfile = UserProfile(name, homeLocation, workLocation),
            isEditingProfile = false,
            feedbackMessage = "Profile updated successfully"
        )
    }

    // Emergency contacts
    fun showEditEmergencyContacts() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = true)
    }

    fun hideEditEmergencyContacts() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = false)
    }

    fun addEmergencyContact(name: String, phoneNumber: String, relationship: String) {
        val newContact = EmergencyContact(
            id = System.currentTimeMillis().toString(),
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship
        )
        val updatedContacts = _uiState.value.emergencySettings.contacts + newContact
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(contacts = updatedContacts),
            feedbackMessage = "Emergency contact added"
        )
    }

    fun removeEmergencyContact(contactId: String) {
        val updatedContacts =
            _uiState.value.emergencySettings.contacts.filter { it.id != contactId }
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(contacts = updatedContacts),
            feedbackMessage = "Emergency contact removed"
        )
    }

    // Emergency message
    fun showEditEmergencyMessage() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = true)
    }

    fun hideEditEmergencyMessage() {
        _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = false)
    }

    fun updateEmergencyMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(emergencyMessage = message),
            isEditingEmergencyMessage = false,
            feedbackMessage = "Emergency message updated"
        )
    }

    // Auto-arrival notification
    fun toggleAutoArrivalNotification() {
        val current = _uiState.value.emergencySettings.autoArrivalNotification
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(autoArrivalNotification = !current),
            feedbackMessage = if (!current) "Auto-arrival notification enabled" else "Auto-arrival notification disabled"
        )
    }

    // Wearable device
    fun reconnectDevice() {
        _uiState.value = _uiState.value.copy(isReconnectingDevice = true)
        // TODO: Implement actual Bluetooth reconnection logic
        // Simulating reconnection for now
        _uiState.value = _uiState.value.copy(
            wearableDevice = _uiState.value.wearableDevice.copy(isConnected = true),
            isReconnectingDevice = false,
            feedbackMessage = "Device reconnected successfully"
        )
    }

    // App preferences
    fun setVoiceSpeed(speed: VoiceSpeed) {
        _uiState.value = _uiState.value.copy(
            appPreferences = _uiState.value.appPreferences.copy(voiceSpeed = speed),
            feedbackMessage = "Voice speed set to ${speed.displayName}"
        )
    }

    fun setVibrationStrength(strength: VibrationStrength) {
        _uiState.value = _uiState.value.copy(
            appPreferences = _uiState.value.appPreferences.copy(vibrationStrength = strength),
            feedbackMessage = "Vibration strength set to ${strength.displayName}"
        )
    }

    fun setMapStyle(style: MapStyle) {
        _uiState.value = _uiState.value.copy(
            appPreferences = _uiState.value.appPreferences.copy(mapStyle = style),
            feedbackMessage = "Map style set to ${style.displayName}"
        )
    }

    fun increaseVibration() {
        val currentStrength = _uiState.value.appPreferences.vibrationStrength
        val nextStrength = when (currentStrength) {
            VibrationStrength.OFF -> VibrationStrength.LIGHT
            VibrationStrength.LIGHT -> VibrationStrength.MEDIUM
            VibrationStrength.MEDIUM -> VibrationStrength.STRONG
            VibrationStrength.STRONG -> VibrationStrength.MAXIMUM
            VibrationStrength.MAXIMUM -> VibrationStrength.MAXIMUM
        }
        setVibrationStrength(nextStrength)
    }

    fun decreaseVibration() {
        val currentStrength = _uiState.value.appPreferences.vibrationStrength
        val prevStrength = when (currentStrength) {
            VibrationStrength.MAXIMUM -> VibrationStrength.STRONG
            VibrationStrength.STRONG -> VibrationStrength.MEDIUM
            VibrationStrength.MEDIUM -> VibrationStrength.LIGHT
            VibrationStrength.LIGHT -> VibrationStrength.OFF
            VibrationStrength.OFF -> VibrationStrength.OFF
        }
        setVibrationStrength(prevStrength)
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    // Profile settings toggles
    fun toggleVoiceFeedbackForProgress() {
        val current = _uiState.value.profileSettings.voiceFeedbackForProgress
        _uiState.value = _uiState.value.copy(
            profileSettings = _uiState.value.profileSettings.copy(voiceFeedbackForProgress = !current),
            feedbackMessage = if (!current) "Voice feedback for progress enabled" else "Voice feedback for progress disabled"
        )
    }

    // Voice command processing
    fun executeVoiceCommand(command: String) {
        val normalizedCommand = command.lowercase().trim()

        when {
            normalizedCommand.contains("edit profile") -> showEditProfile()
            normalizedCommand.contains("edit emergency contacts") -> showEditEmergencyContacts()
            normalizedCommand.contains("change emergency message") -> showEditEmergencyMessage()
            normalizedCommand.contains("reconnect device") -> reconnectDevice()
            normalizedCommand.contains("change voice speed") -> {
                // Cycle through voice speeds
                val current = _uiState.value.appPreferences.voiceSpeed
                val next = when (current) {
                    VoiceSpeed.SLOW -> VoiceSpeed.NORMAL
                    VoiceSpeed.NORMAL -> VoiceSpeed.FAST
                    VoiceSpeed.FAST -> VoiceSpeed.VERY_FAST
                    VoiceSpeed.VERY_FAST -> VoiceSpeed.SLOW
                }
                setVoiceSpeed(next)
            }

            normalizedCommand.contains("increase vibration") -> increaseVibration()
            normalizedCommand.contains("decrease vibration") -> decreaseVibration()
        }
    }
}
