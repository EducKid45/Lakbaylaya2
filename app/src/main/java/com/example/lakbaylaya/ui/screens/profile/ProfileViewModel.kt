@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.ui.screens.profile

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.bluetooth.BluetoothManagerHelper
import com.example.lakbaylaya.bluetooth.BluetoothState
import com.example.lakbaylaya.data.repository.NavigationStatsRepository
import com.example.lakbaylaya.data.repository.UserProfileRepository
import com.example.lakbaylaya.data.room.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── Data models ──────────────────────────────────────────────────────────────

data class UserProfile(
    val name: String = "",
    val phoneNumber: String = "",
    val homeLocation: String = "",
    val homeLat: Double = 0.0,
    val homeLon: Double = 0.0,
    val workLocation: String = "",
    val workLat: Double = 0.0,
    val workLon: Double = 0.0
)

data class HealthStats(
    val distanceWalkedTodayKm: Double = 0.0,
    val distanceWalkedWeekKm: Double = 0.0,
    val stepsToday: Int = 0,
    val stepsWeek: Int = 0,
    val routesCompletedToday: Int = 0,
    val routesCompletedWeek: Int = 0
)

data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String = ""
)

data class EmergencySettings(
    val contacts: List<EmergencyContact> = emptyList(),
    val emergencyMessage: String = "I need help. Please call me or send assistance to my location.",
    val autoArrivalNotification: Boolean = true
)

data class WearableDevice(
    val name: String = "",
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null
)

enum class GpsStatus(val displayName: String) {
    READY("Ready"),
    SEARCHING("Searching"),
    UNAVAILABLE("Unavailable")
}

data class DeviceStatus(
    val wearableDevice: WearableDevice = WearableDevice(),
    val gpsStatus: GpsStatus = GpsStatus.SEARCHING
)

data class ProfileSettings(
    val voiceFeedbackForProgress: Boolean = true
)

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

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UserProfileRepository.create(application.applicationContext)
    private val statsRepo = NavigationStatsRepository(application.applicationContext)
    private val bluetoothHelper = BluetoothManagerHelper(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "ProfileViewModel" }

    init {
        observeProfile()
        observeBluetoothState()
        observeNavigationStats()
        refreshGpsStatus()
    }

    // ── Navigation stats from DB ───────────────────────────────────────────────

    private fun observeNavigationStats() {
        viewModelScope.launch {
            // observe last 7 days
            statsRepo.observeSince(7).collect { rows ->
                val todayEpoch = LocalDate.now().toEpochDay()
                val weekEpoch  = todayEpoch - 7

                val todayRow = rows.firstOrNull { it.dateEpochDay == todayEpoch }
                val weekRows = rows.filter { it.dateEpochDay >= weekEpoch }

                _uiState.value = _uiState.value.copy(
                    healthStats = HealthStats(
                        distanceWalkedTodayKm = (todayRow?.distanceMeters ?: 0.0) / 1000.0,
                        distanceWalkedWeekKm  = weekRows.sumOf { it.distanceMeters } / 1000.0,
                        stepsToday            = todayRow?.steps ?: 0,
                        stepsWeek             = weekRows.sumOf { it.steps },
                        routesCompletedToday  = todayRow?.routesCompleted ?: 0,
                        routesCompletedWeek   = weekRows.sumOf { it.routesCompleted }
                    )
                )
            }
        }
    }

    // ── Profile observation from Room ─────────────────────────────────────────

    private fun observeProfile() {
        viewModelScope.launch {
            repo.observeProfile().collect { saved ->
                if (saved != null) {
                    Log.d(TAG, "Profile loaded: name='${saved.name}'")
                    val contact = if (saved.emergencyContactName.isNotBlank() ||
                        saved.emergencyContactNumber.isNotBlank()
                    ) {
                        listOf(
                            EmergencyContact(
                                id = "primary",
                                name = saved.emergencyContactName,
                                phoneNumber = saved.emergencyContactNumber
                            )
                        )
                    } else emptyList()

                    _uiState.value = _uiState.value.copy(
                        userProfile = UserProfile(
                            name = saved.name,
                            phoneNumber = saved.phoneNumber,
                            homeLocation = saved.homeAddress,
                            homeLat = saved.homeLat,
                            homeLon = saved.homeLon,
                            workLocation = saved.workAddress,
                            workLat = saved.workLat,
                            workLon = saved.workLon
                        ),
                        emergencySettings = _uiState.value.emergencySettings.copy(
                            contacts = contact,
                            emergencyMessage = saved.emergencyMessage
                        )
                    )
                }
            }
        }
    }

    // ── GPS (real, checked on demand) ─────────────────────────────────────────

    fun refreshGpsStatus() {
        val lm = getApplication<Application>()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val gpsStatus = if (enabled) GpsStatus.READY else GpsStatus.UNAVAILABLE
        _uiState.value = _uiState.value.copy(
            deviceStatus = _uiState.value.deviceStatus.copy(gpsStatus = gpsStatus)
        )
    }

    // ── Bluetooth (real, from BluetoothManagerHelper) ─────────────────────────

    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothHelper.bluetoothState.collect { state ->
                val isConnected = state == BluetoothState.CONNECTED
                val name = if (isConnected)
                    bluetoothHelper.connectedDeviceName.value ?: "ESP32" else ""
                _uiState.value = _uiState.value.copy(
                    deviceStatus = _uiState.value.deviceStatus.copy(
                        wearableDevice = WearableDevice(
                            name = name,
                            isConnected = isConnected,
                            batteryLevel = null  // ESP32 doesn't report battery over SPP
                        )
                    )
                )
            }
        }
    }

    // ── Profile CRUD ──────────────────────────────────────────────────────────

    fun showEditProfile() { _uiState.value = _uiState.value.copy(isEditingProfile = true) }
    fun hideEditProfile() { _uiState.value = _uiState.value.copy(isEditingProfile = false) }

    /** Save name, phone, home & work immediately to Room. */
    fun updateProfile(
        name: String,
        phoneNumber: String,
        homeLocation: String,
        homeLat: Double = _uiState.value.userProfile.homeLat,
        homeLon: Double = _uiState.value.userProfile.homeLon,
        workLocation: String,
        workLat: Double = _uiState.value.userProfile.workLat,
        workLon: Double = _uiState.value.userProfile.workLon
    ) {
        _uiState.value = _uiState.value.copy(
            userProfile = UserProfile(name, phoneNumber, homeLocation, homeLat, homeLon, workLocation, workLat, workLon),
            isEditingProfile = false,
            feedbackMessage = "Profile updated successfully"
        )
        persistProfile()
    }

    /** Save home marker (lat/lon + address) from map editor. */
    fun updateHomeLocation(address: String, lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(
                homeLocation = address, homeLat = lat, homeLon = lon
            ),
            feedbackMessage = "Home location updated"
        )
        persistProfile()
    }

    /** Save work marker (lat/lon + address) from map editor. */
    fun updateWorkLocation(address: String, lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(
                workLocation = address, workLat = lat, workLon = lon
            ),
            feedbackMessage = "Work location updated"
        )
        persistProfile()
    }

    // ── Emergency contact CRUD ────────────────────────────────────────────────

    fun showEditEmergencyContacts() { _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = true) }
    fun hideEditEmergencyContacts() { _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = false) }

    /** Only one fixed contact is supported; replaces any existing one. */
    fun upsertEmergencyContact(name: String, phoneNumber: String) {
        val contact = EmergencyContact(id = "primary", name = name, phoneNumber = phoneNumber)
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(contacts = listOf(contact)),
            isEditingEmergencyContacts = false,
            feedbackMessage = "Emergency contact saved"
        )
        persistProfile()
    }

    fun removeEmergencyContact() {
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(contacts = emptyList()),
            feedbackMessage = "Emergency contact removed"
        )
        persistProfile()
    }

    // ── Emergency message ─────────────────────────────────────────────────────

    fun showEditEmergencyMessage() { _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = true) }
    fun hideEditEmergencyMessage() { _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = false) }

    fun updateEmergencyMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(emergencyMessage = message),
            isEditingEmergencyMessage = false,
            feedbackMessage = "Emergency message updated"
        )
        persistProfile()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun persistProfile() {
        val ui = _uiState.value
        val contact = ui.emergencySettings.contacts.firstOrNull()
        viewModelScope.launch {
            repo.saveProfile(
                UserProfileEntity(
                    id = 0,
                    name = ui.userProfile.name,
                    phoneNumber = ui.userProfile.phoneNumber,
                    emergencyContactName = contact?.name ?: "",
                    emergencyContactNumber = contact?.phoneNumber ?: "",
                    emergencyMessage = ui.emergencySettings.emergencyMessage,
                    homeAddress = ui.userProfile.homeLocation,
                    homeLat = ui.userProfile.homeLat,
                    homeLon = ui.userProfile.homeLon,
                    workAddress = ui.userProfile.workLocation,
                    workLat = ui.userProfile.workLat,
                    workLon = ui.userProfile.workLon
                )
            )
        }
    }

    // ── Misc ─────────────────────────────────────────────────────────────────

    fun toggleAutoArrivalNotification() {
        val cur = _uiState.value.emergencySettings.autoArrivalNotification
        _uiState.value = _uiState.value.copy(
            emergencySettings = _uiState.value.emergencySettings.copy(autoArrivalNotification = !cur)
        )
    }

    fun toggleVoiceFeedbackForProgress() {
        val cur = _uiState.value.profileSettings.voiceFeedbackForProgress
        _uiState.value = _uiState.value.copy(
            profileSettings = _uiState.value.profileSettings.copy(voiceFeedbackForProgress = !cur)
        )
    }

    fun reconnectDevice() {
        _uiState.value = _uiState.value.copy(isReconnectingDevice = true)
        bluetoothHelper.enableBluetooth()
        _uiState.value = _uiState.value.copy(isReconnectingDevice = false)
    }

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

    fun clearFeedback() { _uiState.value = _uiState.value.copy(feedbackMessage = null) }

    fun executeVoiceCommand(command: String) {
        val cmd = command.lowercase().trim()
        when {
            cmd.contains("edit profile") -> showEditProfile()
            cmd.contains("edit emergency") -> showEditEmergencyContacts()
            cmd.contains("emergency message") -> showEditEmergencyMessage()
            cmd.contains("reconnect") -> reconnectDevice()
        }
    }
}
