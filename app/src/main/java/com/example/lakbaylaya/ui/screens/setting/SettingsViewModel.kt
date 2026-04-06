@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.ui.screens.setting

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.bluetooth.BluetoothManagerHelper
import com.example.lakbaylaya.bluetooth.Esp32VibrationManager
import com.example.lakbaylaya.bluetooth.VibrationCode
import com.example.lakbaylaya.ui.screens.map.navigation.tts.AndroidTextToSpeechEngine
import com.example.lakbaylaya.data.repository.UserProfileRepository
import com.example.lakbaylaya.data.room.UserProfileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── DataStore for settings persistence ───────────────────────────────────────

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

private object SettingsKeys {
    val VOICE_GUIDANCE   = booleanPreferencesKey("voice_guidance")
    val NAV_INSTRUCTION  = booleanPreferencesKey("nav_instruction")
    val VOICE_SPEED      = stringPreferencesKey("voice_speed")
    val VOICE_VOLUME     = intPreferencesKey("voice_volume")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val VIBRATION_STRENGTH= stringPreferencesKey("vibration_strength")
    val PATTERN_LEFT     = stringPreferencesKey("pattern_left")
    val PATTERN_RIGHT    = stringPreferencesKey("pattern_right")
    val PATTERN_FORWARD  = stringPreferencesKey("pattern_forward")
    val PATTERN_BACKWARD = stringPreferencesKey("pattern_backward")
    val PATTERN_ARRIVAL  = stringPreferencesKey("pattern_arrival")
    val PATTERN_DANGER   = stringPreferencesKey("pattern_danger")
    val AUTO_ARRIVAL_SMS = booleanPreferencesKey("auto_arrival_sms")
}

// Safe enum resolver with default fallback — never crashes on corrupted/missing keys
private inline fun <reified E : Enum<E>> safeEnum(name: String?, default: E): E =
    if (name.isNullOrBlank()) default
    else runCatching { enumValueOf<E>(name) }.getOrDefault(default)



data class VoiceAudioSettings(
    val voiceGuidanceEnabled: Boolean = true,
    val navigationInstructionEnabled: Boolean = true,   // raw route instructions on/off
    val voiceSpeed: VoiceSpeed = VoiceSpeed.NORMAL,
    val voiceVolume: Int = 80
)

enum class VoiceSpeed(val displayName: String, val multiplier: Float) {
    SLOW("Slow", 0.75f),
    NORMAL("Normal", 1.0f),
    FAST("Fast", 1.25f)
}


data class VibrationSettings(
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: VibrationStrength = VibrationStrength.MEDIUM,
    val leftPattern: VibrationPattern = VibrationPattern.DOUBLE_PULSE,
    val rightPattern: VibrationPattern = VibrationPattern.DOUBLE_PULSE,
    val forwardPattern: VibrationPattern = VibrationPattern.SHORT_PULSE,
    val backwardPattern: VibrationPattern = VibrationPattern.SHORT_PULSE,
    val arrivalPattern: VibrationPattern = VibrationPattern.LONG_VIBRATION,
    val dangerPattern: VibrationPattern = VibrationPattern.RAPID_PULSES
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
    WAVE("Wave", "Increasing then decreasing"),
    RAPID_PULSES("Rapid Pulses", "Multiple quick vibrations")
}

// â”€â”€ Navigation Preferences â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class NavigationPreferences(
    val defaultWalkingMode: Boolean = false,
    val autoCameraOrientation: Boolean = true,
    val autoReroute: Boolean = true,
    val routePreviewEnabled: Boolean = true
)

// â”€â”€ Safety & Emergency â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€ Device & App settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class DeviceSettings(
    val pairedDeviceName: String = "",
    val isDeviceConnected: Boolean = false,
    val deviceBatteryLevel: Int? = null
)

data class AppDataSettings(
    val routeHistoryCount: Int = 0,
    val familiarRoutesCount: Int = 0,
    val offlineMapsDownloaded: Boolean = false,
    val appVersion: String = "1.0.0",
    val lastUpdateCheck: String = ""
)

// â”€â”€ UI State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class SettingsUiState(
    val voiceAudioSettings: VoiceAudioSettings = VoiceAudioSettings(),
    val vibrationSettings: VibrationSettings = VibrationSettings(),
    val navigationPreferences: NavigationPreferences = NavigationPreferences(),
    val safetySettings: SafetySettings = SafetySettings(),
    val deviceSettings: DeviceSettings = DeviceSettings(),
    val appDataSettings: AppDataSettings = AppDataSettings(),
    val isEditingEmergencyContacts: Boolean = false,
    val isEditingEmergencyMessage: Boolean = false,
    val isPairingDevice: Boolean = false,
    val isTestingVibration: Boolean = false,
    val showTestVibrationDialog: Boolean = false,
    val showClearHistoryConfirmation: Boolean = false,
    val showResetRoutesConfirmation: Boolean = false,
    val feedbackMessage: String? = null,
    // Currently editing a single contact in the dialog (null = none)
    val editingContact: EmergencyContact? = null
)

// â”€â”€ ViewModel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Profile repository to sync emergency contact and message with main profile
    private val profileRepo by lazy { UserProfileRepository.create(application.applicationContext) }
    private var currentProfileEntity: UserProfileEntity? = null

    // TTS engine for immediate spoken feedback
    private val tts = AndroidTextToSpeechEngine(application)
    private var ttsReady = false

    // Bluetooth + ESP32 vibration (lazy â€” safe if BT not available)
    private val bluetoothHelper by lazy { BluetoothManagerHelper(application) }
    private val esp32 by lazy { Esp32VibrationManager.getInstance(bluetoothHelper) }

    // Application context shortcut for toasts
    private val appContext = application.applicationContext

    init {
        tts.initialize(
            onReady = { ttsReady = true },
            onError = { ttsReady = false }
        )
        loadSettings()

        // Observe user profile so Settings shows and persists emergency contact and message
        viewModelScope.launch {
            profileRepo.observeProfile().collect { saved ->
                currentProfileEntity = saved
                if (saved != null) {
                    val contacts = if (saved.emergencyContactName.isNotBlank() || saved.emergencyContactNumber.isNotBlank()) {
                        listOf(EmergencyContact(id = "primary", name = saved.emergencyContactName, phoneNumber = saved.emergencyContactNumber))
                    } else emptyList()
                    _uiState.value = _uiState.value.copy(
                        safetySettings = _uiState.value.safetySettings.copy(
                            emergencyContacts = contacts,
                            emergencyMessage = saved.emergencyMessage,
                            autoSendArrivalNotification = saved.autoSendArrivalNotification
                        )
                    )
                }
            }
        }
    }

    // Small helper to show a short Toast from ViewModel
    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun speakFeedback(text: String) {
        if (!ttsReady) return
        tts.setSpeechRate(_uiState.value.voiceAudioSettings.voiceSpeed.multiplier)
        applySystemVolume(_uiState.value.voiceAudioSettings.voiceVolume)
        tts.speak(text, priority = true)
    }

    private fun applySystemVolume(volumePercent: Int) {
        val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, ((volumePercent / 100f) * maxVol).toInt().coerceIn(0, maxVol), 0)
    }

    private fun vibrate(pattern: LongArray, amplitude: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, amplitude, 0, amplitude), -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, amplitude, 0, amplitude), -1))
            }
        } catch (_: Exception) {}
    }

    /** Propagate the current vibration pattern for [code] to the ESP32 singleton. */
    private fun syncPatternToEsp32(code: Char, pattern: VibrationPattern) {
        esp32.setPattern(code, pattern.name)
    }

    // ── DataStore persistence ─────────────────────────────────────────────────

    private fun persistAudioSettings(audio: VoiceAudioSettings) {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[SettingsKeys.VOICE_GUIDANCE]  = audio.voiceGuidanceEnabled
                prefs[SettingsKeys.NAV_INSTRUCTION]  = audio.navigationInstructionEnabled
                prefs[SettingsKeys.VOICE_SPEED]      = audio.voiceSpeed.name
                prefs[SettingsKeys.VOICE_VOLUME]     = audio.voiceVolume.coerceIn(0, 100)
            }
        }
    }

    private fun persistVibrationSettings(vib: VibrationSettings) {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[SettingsKeys.VIBRATION_ENABLED]  = vib.vibrationEnabled
                prefs[SettingsKeys.VIBRATION_STRENGTH] = vib.vibrationStrength.name
                // Guard: never write blank/null pattern — fall back to safe default
                prefs[SettingsKeys.PATTERN_LEFT]     = vib.leftPattern.name.ifBlank { VibrationPattern.DOUBLE_PULSE.name }
                prefs[SettingsKeys.PATTERN_RIGHT]    = vib.rightPattern.name.ifBlank { VibrationPattern.DOUBLE_PULSE.name }
                prefs[SettingsKeys.PATTERN_FORWARD]  = vib.forwardPattern.name.ifBlank { VibrationPattern.SHORT_PULSE.name }
                prefs[SettingsKeys.PATTERN_BACKWARD] = vib.backwardPattern.name.ifBlank { VibrationPattern.SHORT_PULSE.name }
                prefs[SettingsKeys.PATTERN_ARRIVAL]  = vib.arrivalPattern.name.ifBlank { VibrationPattern.LONG_VIBRATION.name }
                prefs[SettingsKeys.PATTERN_DANGER]   = vib.dangerPattern.name.ifBlank { VibrationPattern.RAPID_PULSES.name }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().settingsDataStore.data
                .catch { emit(emptyPreferences()) }   // safe default on corruption or missing file
                .first()

            val audio = VoiceAudioSettings(
                voiceGuidanceEnabled         = prefs[SettingsKeys.VOICE_GUIDANCE]  ?: true,
                navigationInstructionEnabled = prefs[SettingsKeys.NAV_INSTRUCTION] ?: true,
                voiceSpeed  = safeEnum(prefs[SettingsKeys.VOICE_SPEED],  VoiceSpeed.NORMAL),
                voiceVolume = (prefs[SettingsKeys.VOICE_VOLUME] ?: 80).coerceIn(0, 100)
            )
            val vib = VibrationSettings(
                vibrationEnabled  = prefs[SettingsKeys.VIBRATION_ENABLED]  ?: true,
                vibrationStrength = safeEnum(prefs[SettingsKeys.VIBRATION_STRENGTH], VibrationStrength.MEDIUM),
                leftPattern     = safeEnum(prefs[SettingsKeys.PATTERN_LEFT],     VibrationPattern.DOUBLE_PULSE),
                rightPattern    = safeEnum(prefs[SettingsKeys.PATTERN_RIGHT],    VibrationPattern.DOUBLE_PULSE),
                forwardPattern  = safeEnum(prefs[SettingsKeys.PATTERN_FORWARD],  VibrationPattern.SHORT_PULSE),
                backwardPattern = safeEnum(prefs[SettingsKeys.PATTERN_BACKWARD], VibrationPattern.SHORT_PULSE),
                arrivalPattern  = safeEnum(prefs[SettingsKeys.PATTERN_ARRIVAL],  VibrationPattern.LONG_VIBRATION),
                dangerPattern   = safeEnum(prefs[SettingsKeys.PATTERN_DANGER],   VibrationPattern.RAPID_PULSES)
            )
            val autoArrivalSms = prefs[SettingsKeys.AUTO_ARRIVAL_SMS] ?: true
            _uiState.update { it.copy(
                voiceAudioSettings = audio,
                vibrationSettings  = vib,
                safetySettings = it.safetySettings.copy(autoSendArrivalNotification = autoArrivalSms),
                appDataSettings    = AppDataSettings(
                    routeHistoryCount = 0, familiarRoutesCount = 0,
                    appVersion = "1.0.0", lastUpdateCheck = "February 22, 2026"
                )
            )}
        }
    }

    // â”€â”€ Voice & Audio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun toggleVoiceGuidance() {
        val cur = _uiState.value.voiceAudioSettings.voiceGuidanceEnabled
        val updated = _uiState.value.voiceAudioSettings.copy(voiceGuidanceEnabled = !cur)
        _uiState.update { it.copy(voiceAudioSettings = updated) }
        persistAudioSettings(updated)
        speakFeedback(if (!cur) "Voice guidance enabled." else "Voice guidance disabled.")
    }

    fun toggleNavigationInstruction() {
        val cur = _uiState.value.voiceAudioSettings.navigationInstructionEnabled
        val updated = _uiState.value.voiceAudioSettings.copy(navigationInstructionEnabled = !cur)
        _uiState.update { it.copy(voiceAudioSettings = updated) }
        persistAudioSettings(updated)
        speakFeedback(if (!cur) "Navigation instructions on." else "Navigation instructions muted.")
    }

    fun setVoiceSpeed(speed: VoiceSpeed) {
        val updated = _uiState.value.voiceAudioSettings.copy(voiceSpeed = speed)
        _uiState.update { it.copy(voiceAudioSettings = updated, feedbackMessage = "Voice speed: ${speed.displayName}") }
        persistAudioSettings(updated)
        if (ttsReady) {
            tts.setSpeechRate(speed.multiplier)
            applySystemVolume(_uiState.value.voiceAudioSettings.voiceVolume)
            tts.speak("Voice speed is now ${speed.displayName}. Turn right in 100 meters.", priority = true)
        }
    }

    fun setVoiceVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        val updated = _uiState.value.voiceAudioSettings.copy(voiceVolume = clamped)
        _uiState.update { it.copy(voiceAudioSettings = updated, feedbackMessage = "Volume: $clamped%") }
        persistAudioSettings(updated)
        if (ttsReady) {
            applySystemVolume(clamped)
            tts.setSpeechRate(_uiState.value.voiceAudioSettings.voiceSpeed.multiplier)
            tts.speak("Voice volume set to $clamped percent.", priority = true)
        }
    }

    // â”€â”€ Vibration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun toggleVibration() {
        val cur = _uiState.value.vibrationSettings.vibrationEnabled
        val updated = _uiState.value.vibrationSettings.copy(vibrationEnabled = !cur)
        _uiState.update { it.copy(vibrationSettings = updated, feedbackMessage = if (!cur) "Haptic enabled" else "Haptic disabled") }
        persistVibrationSettings(updated)
        esp32.hapticEnabled = !cur
        speakFeedback(if (!cur) "Haptic feedback enabled." else "Haptic feedback disabled.")
    }

    fun setVibrationStrength(strength: VibrationStrength) {
        val updated = _uiState.value.vibrationSettings.copy(vibrationStrength = strength)
        _uiState.update { it.copy(vibrationSettings = updated, feedbackMessage = "Vibration strength: ${strength.displayName}") }
        persistVibrationSettings(updated)
        speakFeedback("Vibration strength set to ${strength.displayName}.")
        vibrate(longArrayOf(0, 200), strength.intensity)
    }

    fun setLeftPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(leftPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
        syncPatternToEsp32(VibrationCode.LEFT, p)
    }
    fun setRightPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(rightPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
        syncPatternToEsp32(VibrationCode.RIGHT, p)
    }
    fun setForwardPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(forwardPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
        syncPatternToEsp32(VibrationCode.FORWARD, p)
    }
    fun setBackwardPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(backwardPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
    }
    fun setArrivalPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(arrivalPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
        syncPatternToEsp32(VibrationCode.ARRIVAL, p)
    }
    fun setDangerPattern(p: VibrationPattern) {
        val updated = _uiState.value.vibrationSettings.copy(dangerPattern = p)
        _uiState.update { it.copy(vibrationSettings = updated) }
        persistVibrationSettings(updated)
        syncPatternToEsp32(VibrationCode.DANGER, p)
    }

    // Test vibration dialog
    fun showTestVibrationDialog() { _uiState.value = _uiState.value.copy(showTestVibrationDialog = true) }
    fun hideTestVibrationDialog() { _uiState.value = _uiState.value.copy(showTestVibrationDialog = false) }

    fun testVibration() {
        _uiState.value = _uiState.value.copy(isTestingVibration = true, feedbackMessage = "Testing vibrationâ€¦")
        val intensity = _uiState.value.vibrationSettings.vibrationStrength.intensity
        vibrate(longArrayOf(0, 200, 100, 200), intensity)
        speakFeedback("Vibration test.")
        viewModelScope.launch {
            kotlinx.coroutines.delay(700)
            _uiState.value = _uiState.value.copy(isTestingVibration = false)
        }
    }

    /** Send a test pattern to ESP32 directly â€” used by test dialog buttons. */
    fun sendTestPatternToEsp32(code: Char, patternName: String) {
        val sent = esp32.sendCode(code, patternName)
        if (sent) {
            vibrate(longArrayOf(0, 180), _uiState.value.vibrationSettings.vibrationStrength.intensity)
        } else {
            // Provide immediate feedback to the user that the device is not connected
            _uiState.update { it.copy(feedbackMessage = "ESP32 not connected. Please pair the device and try again.") }
            showToast("ESP32 not connected — unable to send pattern")
        }
    }


    fun toggleWalkingMode() { val cur = _uiState.value.navigationPreferences.defaultWalkingMode; _uiState.value = _uiState.value.copy(navigationPreferences = _uiState.value.navigationPreferences.copy(defaultWalkingMode = !cur)) }
    fun toggleAutoCameraOrientation() { val cur = _uiState.value.navigationPreferences.autoCameraOrientation; _uiState.value = _uiState.value.copy(navigationPreferences = _uiState.value.navigationPreferences.copy(autoCameraOrientation = !cur)) }
    fun toggleAutoReroute() { val cur = _uiState.value.navigationPreferences.autoReroute; _uiState.value = _uiState.value.copy(navigationPreferences = _uiState.value.navigationPreferences.copy(autoReroute = !cur)) }
    fun toggleRoutePreview() { val cur = _uiState.value.navigationPreferences.routePreviewEnabled; _uiState.value = _uiState.value.copy(navigationPreferences = _uiState.value.navigationPreferences.copy(routePreviewEnabled = !cur)) }


    fun showEditEmergencyContacts() { _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = true) }
    fun hideEditEmergencyContacts() { _uiState.value = _uiState.value.copy(isEditingEmergencyContacts = false) }
    fun addEmergencyContact(name: String, phone: String, relationship: String) {
        val new = EmergencyContact(id = System.currentTimeMillis().toString(), name = name, phoneNumber = phone, relationship = relationship)
        _uiState.value = _uiState.value.copy(safetySettings = _uiState.value.safetySettings.copy(emergencyContacts = _uiState.value.safetySettings.emergencyContacts + new))

        // Persist single primary contact behavior: save as primary if first
        if (_uiState.value.safetySettings.emergencyContacts.size <= 1) {
            viewModelScope.launch {
                val existing = currentProfileEntity
                val entity = UserProfileEntity(
                    id = existing?.id ?: 0,
                    name = existing?.name ?: "",
                    phoneNumber = existing?.phoneNumber ?: "",
                    emergencyContactName = name,
                    emergencyContactNumber = phone,
                    emergencyMessage = _uiState.value.safetySettings.emergencyMessage,
                    homeAddress = existing?.homeAddress ?: "",
                    homeLat = existing?.homeLat ?: 0.0,
                    homeLon = existing?.homeLon ?: 0.0,
                    workAddress = existing?.workAddress ?: "",
                    workLat = existing?.workLat ?: 0.0,
                    workLon = existing?.workLon ?: 0.0
                )
                profileRepo.saveProfile(entity)
                currentProfileEntity = entity
            }
        }
    }
    fun removeEmergencyContact(id: String) {
        _uiState.value = _uiState.value.copy(safetySettings = _uiState.value.safetySettings.copy(emergencyContacts = _uiState.value.safetySettings.emergencyContacts.filter { it.id != id }))
        // If primary removed, clear in profile
        viewModelScope.launch {
            val existing = currentProfileEntity
            if (existing != null) {
                val entity = existing.copy(emergencyContactName = "", emergencyContactNumber = "")
                profileRepo.saveProfile(entity)
                currentProfileEntity = entity
            }
        }
    }

    // Show single-contact edit dialog
    fun showEditEmergencyContact(contact: EmergencyContact) {
        _uiState.value = _uiState.value.copy(editingContact = contact)
    }

    fun hideEditEmergencyContact() {
        _uiState.value = _uiState.value.copy(editingContact = null)
    }

    fun updateEmergencyContact(id: String, name: String, phone: String, relationship: String) {
        // Update local UI state
        val updatedList = _uiState.value.safetySettings.emergencyContacts.map { c ->
            if (c.id == id) c.copy(name = name, phoneNumber = phone, relationship = relationship) else c
        }
        val finalList = if (updatedList.none { it.id == id }) _uiState.value.safetySettings.emergencyContacts + EmergencyContact(id = id, name = name, phoneNumber = phone, relationship = relationship) else updatedList
        _uiState.value = _uiState.value.copy(safetySettings = _uiState.value.safetySettings.copy(emergencyContacts = finalList), editingContact = null, feedbackMessage = "Emergency contact updated")

        // Persist to main profile table (single-row)
        viewModelScope.launch {
            val existing = currentProfileEntity
            val entity = UserProfileEntity(
                id = existing?.id ?: 0,
                name = existing?.name ?: _uiState.value.appDataSettings.appVersion /* placeholder: keep existing name if any */,
                phoneNumber = existing?.phoneNumber ?: "",
                emergencyContactName = name,
                emergencyContactNumber = phone,
                emergencyMessage = _uiState.value.safetySettings.emergencyMessage,
                homeAddress = existing?.homeAddress ?: "",
                homeLat = existing?.homeLat ?: 0.0,
                homeLon = existing?.homeLon ?: 0.0,
                workAddress = existing?.workAddress ?: "",
                workLat = existing?.workLat ?: 0.0,
                workLon = existing?.workLon ?: 0.0
            )
            profileRepo.saveProfile(entity)
            currentProfileEntity = entity
        }
    }

    // â”€â”€ Device â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun startPairingDevice() { _uiState.value = _uiState.value.copy(isPairingDevice = true, feedbackMessage = "Searching for devicesâ€¦") }
    fun stopPairingDevice() { _uiState.value = _uiState.value.copy(isPairingDevice = false) }
    fun reconnectDevice() {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Reconnectingâ€¦")
        bluetoothHelper.enableBluetooth()
        _uiState.value = _uiState.value.copy(feedbackMessage = "Reconnecting deviceâ€¦")
        speakFeedback("Attempting to reconnect wearable device.")
    }


    fun showClearHistoryConfirmation() { _uiState.value = _uiState.value.copy(showClearHistoryConfirmation = true) }
    fun hideClearHistoryConfirmation() { _uiState.value = _uiState.value.copy(showClearHistoryConfirmation = false) }
    fun clearRouteHistory() { _uiState.value = _uiState.value.copy(appDataSettings = _uiState.value.appDataSettings.copy(routeHistoryCount = 0), showClearHistoryConfirmation = false, feedbackMessage = "Route history cleared") }
    fun showResetRoutesConfirmation() { _uiState.value = _uiState.value.copy(showResetRoutesConfirmation = true) }
    fun hideResetRoutesConfirmation() { _uiState.value = _uiState.value.copy(showResetRoutesConfirmation = false) }
    fun resetFamiliarRoutes() { _uiState.value = _uiState.value.copy(appDataSettings = _uiState.value.appDataSettings.copy(familiarRoutesCount = 0), showResetRoutesConfirmation = false, feedbackMessage = "Familiar routes reset") }
    fun downloadOfflineMaps() { _uiState.value = _uiState.value.copy(feedbackMessage = "Downloading offline mapsâ€¦") }
    fun checkForUpdates() { _uiState.value = _uiState.value.copy(feedbackMessage = "App is up to date") }
    fun clearFeedback() { _uiState.value = _uiState.value.copy(feedbackMessage = null) }

    fun executeVoiceCommand(command: String) {
        val cmd = command.lowercase().trim()
        when {
            cmd.contains("enable voice") -> { if (!_uiState.value.voiceAudioSettings.voiceGuidanceEnabled) toggleVoiceGuidance() }
            cmd.contains("disable voice") -> { if (_uiState.value.voiceAudioSettings.voiceGuidanceEnabled) toggleVoiceGuidance() }
            cmd.contains("voice speed") || cmd.contains("change speed") -> { val next = VoiceSpeed.entries.toTypedArray().let { it[(it.indexOf(_uiState.value.voiceAudioSettings.voiceSpeed) + 1) % it.size] }; setVoiceSpeed(next) }
            cmd.contains("test vibration") -> testVibration()
            cmd.contains("enable vibration") || cmd.contains("enable haptic") -> { if (!_uiState.value.vibrationSettings.vibrationEnabled) toggleVibration() }
            cmd.contains("disable vibration") || cmd.contains("disable haptic") -> { if (_uiState.value.vibrationSettings.vibrationEnabled) toggleVibration() }
            cmd.contains("edit emergency contacts") -> showEditEmergencyContacts()
            cmd.contains("emergency message") -> showEditEmergencyMessage()
            cmd.contains("reconnect device") -> reconnectDevice()
            cmd.contains("clear history") -> showClearHistoryConfirmation()
        }
    }

    // Emergency message editing in Settings (persist to profile table)
    fun showEditEmergencyMessage() { _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = true) }
    fun hideEditEmergencyMessage() { _uiState.value = _uiState.value.copy(isEditingEmergencyMessage = false) }
    fun updateEmergencyMessage(message: String) {
        _uiState.value = _uiState.value.copy(safetySettings = _uiState.value.safetySettings.copy(emergencyMessage = message), isEditingEmergencyMessage = false, feedbackMessage = "Emergency message updated")
        // Persist into profile table
        viewModelScope.launch {
            val existing = currentProfileEntity
            val entity = UserProfileEntity(
                id = existing?.id ?: 0,
                name = existing?.name ?: "",
                phoneNumber = existing?.phoneNumber ?: "",
                emergencyContactName = existing?.emergencyContactName ?: "",
                emergencyContactNumber = existing?.emergencyContactNumber ?: "",
                emergencyMessage = message,
                homeAddress = existing?.homeAddress ?: "",
                homeLat = existing?.homeLat ?: 0.0,
                homeLon = existing?.homeLon ?: 0.0,
                workAddress = existing?.workAddress ?: "",
                workLat = existing?.workLat ?: 0.0,
                workLon = existing?.workLon ?: 0.0
            )
            profileRepo.saveProfile(entity)
            currentProfileEntity = entity
        }
    }

    fun toggleAutoArrivalNotification() {
        val cur = _uiState.value.safetySettings.autoSendArrivalNotification
        val newVal = !cur
        _uiState.value = _uiState.value.copy(safetySettings = _uiState.value.safetySettings.copy(autoSendArrivalNotification = newVal))
        speakFeedback(if (newVal) "Arrival notification enabled." else "Arrival notification disabled.")
        viewModelScope.launch {
            // Persist to DataStore
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[SettingsKeys.AUTO_ARRIVAL_SMS] = newVal
            }
            // Also persist to profile table
            val existing = currentProfileEntity
            val entity = (existing ?: UserProfileEntity()).copy(autoSendArrivalNotification = newVal)
            profileRepo.saveProfile(entity)
            currentProfileEntity = entity
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
    }
}
