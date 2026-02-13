package com.example.lakbaylaya.ui.screens.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Parent `LakbayLayaApp` provides the TopBar. Treat this as scaffold content.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Voice & Audio Settings
            item {
                VoiceAudioSection(
                    settings = uiState.voiceAudioSettings,
                    onToggleVoice = { viewModel.toggleVoiceGuidance() },
                    onSpeedChange = { viewModel.setVoiceSpeed(it) },
                    onVolumeChange = { viewModel.setVoiceVolume(it) },
                    onLanguageChange = { viewModel.setLanguage(it) }
                )
            }

            // 2. Vibration & Haptic Feedback
            item {
                VibrationSection(
                    settings = uiState.vibrationSettings,
                    isTestingVibration = uiState.isTestingVibration,
                    onToggleVibration = { viewModel.toggleVibration() },
                    onStrengthChange = { viewModel.setVibrationStrength(it) },
                    onNormalPatternChange = { viewModel.setNormalPathPattern(it) },
                    onDifficultPatternChange = { viewModel.setDifficultPathPattern(it) },
                    onArrivalPatternChange = { viewModel.setArrivalPattern(it) },
                    onTestVibration = { viewModel.testVibration() }
                )
            }

            // 3. Navigation Preferences
            item {
                NavigationPreferencesSection(
                    preferences = uiState.navigationPreferences,
                    onToggleWalkingMode = { viewModel.toggleWalkingMode() },
                    onToggleAutoCameraOrientation = { viewModel.toggleAutoCameraOrientation() },
                    onToggleAutoReroute = { viewModel.toggleAutoReroute() },
                    onToggleRoutePreview = { viewModel.toggleRoutePreview() }
                )
            }

            // 4. Safety & Emergency Configuration
            item {
                SafetySection(
                    settings = uiState.safetySettings,
                    onEditContacts = { viewModel.showEditEmergencyContacts() },
                    onEditMessage = { viewModel.showEditEmergencyMessage() },
                    onToggleAutoArrival = { viewModel.toggleAutoArrivalNotification() }
                )
            }

            // 5. Device & Connectivity
            item {
                DeviceConnectivitySection(
                    settings = uiState.deviceSettings,
                    isPairing = uiState.isPairingDevice,
                    onPairDevice = { viewModel.startPairingDevice() },
                    onReconnect = { viewModel.reconnectDevice() }
                )
            }

            // 6. App & Data Settings
            item {
                AppDataSection(
                    settings = uiState.appDataSettings,
                    onClearHistory = { viewModel.showClearHistoryConfirmation() },
                    onResetRoutes = { viewModel.showResetRoutesConfirmation() },
                    onDownloadMaps = { viewModel.downloadOfflineMaps() },
                    onCheckUpdates = { viewModel.checkForUpdates() }
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialogs
    if (uiState.isEditingEmergencyMessage) {
        EditEmergencyMessageDialog(
            currentMessage = uiState.safetySettings.emergencyMessage,
            onDismiss = { viewModel.hideEditEmergencyMessage() },
            onSave = { viewModel.updateEmergencyMessage(it) }
        )
    }

    if (uiState.showClearHistoryConfirmation) {
        ConfirmationDialog(
            title = "Clear Route History?",
            message = "This will delete all ${uiState.appDataSettings.routeHistoryCount} routes from your history. This action cannot be undone.",
            confirmText = "Clear History",
            onDismiss = { viewModel.hideClearHistoryConfirmation() },
            onConfirm = { viewModel.clearRouteHistory() }
        )
    }

    if (uiState.showResetRoutesConfirmation) {
        ConfirmationDialog(
            title = "Reset Familiar Routes?",
            message = "This will delete all ${uiState.appDataSettings.familiarRoutesCount} familiar routes. This action cannot be undone.",
            confirmText = "Reset Routes",
            onDismiss = { viewModel.hideResetRoutesConfirmation() },
            onConfirm = { viewModel.resetFamiliarRoutes() }
        )
    }
}

// ============================================
// Section 1: Voice & Audio Settings
// ============================================

@Composable
private fun VoiceAudioSection(
    settings: VoiceAudioSettings,
    onToggleVoice: () -> Unit,
    onSpeedChange: (VoiceSpeed) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLanguageChange: (com.example.lakbaylaya.ui.screens.setting.Language) -> Unit
) {
    var showSpeedOptions by remember { mutableStateOf(false) }
    var showLanguageOptions by remember { mutableStateOf(false) }

    SectionCard(
        title = "Voice & Audio",
        titleDescription = "Voice and Audio settings section. Configure voice guidance, speed, volume, and language.",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        iconColor = Color(0xFF1976D2)
    ) {
        // Voice guidance toggle
        SettingToggleRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "Voice Guidance",
            description = "Spoken turn-by-turn directions",
            isEnabled = settings.voiceGuidanceEnabled,
            onToggle = onToggleVoice,
            accessibilityDescription = if (settings.voiceGuidanceEnabled) {
                "Voice guidance is enabled. Tap to disable."
            } else {
                "Voice guidance is disabled. Tap to enable."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Voice speed
        SettingOptionRow(
            icon = Icons.Default.Speed,
            title = "Voice Speed",
            currentValue = settings.voiceSpeed.displayName,
            isExpanded = showSpeedOptions,
            onToggleExpand = { showSpeedOptions = !showSpeedOptions },
            accessibilityDescription = "Voice speed is set to ${settings.voiceSpeed.displayName}. Tap to change."
        ) {
            VoiceSpeed.entries.forEach { speed ->
                OptionItem(
                    label = speed.displayName,
                    isSelected = settings.voiceSpeed == speed,
                    onClick = {
                        onSpeedChange(speed)
                        showSpeedOptions = false
                    },
                    description = "Set voice speed to ${speed.displayName}"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Voice volume slider
        VolumeSlider(
            volume = settings.voiceVolume,
            onVolumeChange = onVolumeChange
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Language selection
        SettingOptionRow(
            icon = Icons.Default.Language,
            title = "Language",
            currentValue = settings.selectedLanguage.displayName,
            isExpanded = showLanguageOptions,
            onToggleExpand = { showLanguageOptions = !showLanguageOptions },
            accessibilityDescription = "Language is set to ${settings.selectedLanguage.displayName}. Tap to change."
        ) {
            Language.entries.forEach { language ->
                OptionItem(
                    label = language.displayName,
                    isSelected = settings.selectedLanguage == language,
                    onClick = {
                        onLanguageChange(language)
                        showLanguageOptions = false
                    },
                    description = "Set language to ${language.displayName}"
                )
            }
        }
    }
}

@Composable
private fun VolumeSlider(
    volume: Int,
    onVolumeChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(volume.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Voice volume is set to ${volume} percent. Drag slider to adjust."
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Voice Volume",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }
            Text(
                text = "${sliderValue.toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onVolumeChange(sliderValue.toInt()) },
            valueRange = 0f..100f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1976D2),
                activeTrackColor = Color(0xFF1976D2),
                inactiveTrackColor = Color(0xFFE0E0E0)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================
// Section 2: Vibration & Haptic Feedback
// ============================================

@Composable
private fun VibrationSection(
    settings: com.example.lakbaylaya.ui.screens.setting.VibrationSettings,
    isTestingVibration: Boolean,
    onToggleVibration: () -> Unit,
    onStrengthChange: (com.example.lakbaylaya.ui.screens.setting.VibrationStrength) -> Unit,
    onNormalPatternChange: (com.example.lakbaylaya.ui.screens.setting.VibrationPattern) -> Unit,
    onDifficultPatternChange: (com.example.lakbaylaya.ui.screens.setting.VibrationPattern) -> Unit,
    onArrivalPatternChange: (com.example.lakbaylaya.ui.screens.setting.VibrationPattern) -> Unit,
    onTestVibration: () -> Unit
) {
    var showStrengthOptions by remember { mutableStateOf(false) }
    var showNormalPatternOptions by remember { mutableStateOf(false) }
    var showDifficultPatternOptions by remember { mutableStateOf(false) }
    var showArrivalPatternOptions by remember { mutableStateOf(false) }

    SectionCard(
        title = "Vibration & Haptic",
        titleDescription = "Vibration and Haptic feedback section. Configure vibration strength and patterns for different navigation events.",
        icon = Icons.Default.Vibration,
        iconColor = Color(0xFF9C27B0)
    ) {
        // Vibration toggle
        SettingToggleRow(
            icon = Icons.Default.Vibration,
            title = "Vibration Feedback",
            description = "Haptic feedback during navigation",
            isEnabled = settings.vibrationEnabled,
            onToggle = onToggleVibration,
            accessibilityDescription = if (settings.vibrationEnabled) {
                "Vibration feedback is enabled. Tap to disable."
            } else {
                "Vibration feedback is disabled. Tap to enable."
            }
        )

        if (settings.vibrationEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(16.dp))

            // Vibration strength
            SettingOptionRow(
                icon = Icons.Default.Speed,
                title = "Vibration Strength",
                currentValue = settings.vibrationStrength.displayName,
                isExpanded = showStrengthOptions,
                onToggleExpand = { showStrengthOptions = !showStrengthOptions },
                accessibilityDescription = "Vibration strength is set to ${settings.vibrationStrength.displayName}. Tap to change."
            ) {
                VibrationStrength.entries.forEach { strength ->
                    OptionItem(
                        label = strength.displayName,
                        isSelected = settings.vibrationStrength == strength,
                        onClick = {
                            onStrengthChange(strength)
                            showStrengthOptions = false
                        },
                        description = "Set vibration strength to ${strength.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(16.dp))

            // Pattern settings header
            Text(
                text = "Vibration Patterns",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Normal path pattern
            SettingOptionRow(
                icon = Icons.Default.Route,
                title = "Normal Path",
                currentValue = settings.normalPathPattern.displayName,
                isExpanded = showNormalPatternOptions,
                onToggleExpand = { showNormalPatternOptions = !showNormalPatternOptions },
                accessibilityDescription = "Normal path vibration pattern is ${settings.normalPathPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.entries.forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.normalPathPattern == pattern,
                        onClick = {
                            onNormalPatternChange(pattern)
                            showNormalPatternOptions = false
                        },
                        description = "Set normal path vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Difficult path pattern
            SettingOptionRow(
                icon = Icons.Default.Security,
                title = "Difficult Path",
                currentValue = settings.difficultPathPattern.displayName,
                isExpanded = showDifficultPatternOptions,
                onToggleExpand = { showDifficultPatternOptions = !showDifficultPatternOptions },
                accessibilityDescription = "Difficult path vibration pattern is ${settings.difficultPathPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.entries.forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.difficultPathPattern == pattern,
                        onClick = {
                            onDifficultPatternChange(pattern)
                            showDifficultPatternOptions = false
                        },
                        description = "Set difficult path vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arrival pattern
            SettingOptionRow(
                icon = Icons.Default.Explore,
                title = "Arrival",
                currentValue = settings.arrivalPattern.displayName,
                isExpanded = showArrivalPatternOptions,
                onToggleExpand = { showArrivalPatternOptions = !showArrivalPatternOptions },
                accessibilityDescription = "Arrival vibration pattern is ${settings.arrivalPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.entries.forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.arrivalPattern == pattern,
                        onClick = {
                            onArrivalPatternChange(pattern)
                            showArrivalPatternOptions = false
                        },
                        description = "Set arrival vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test vibration button
            OutlinedButton(
                onClick = onTestVibration,
                enabled = !isTestingVibration,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription =
                            "Test vibration. Tap to feel the current vibration pattern."
                    },
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTestingVibration) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Vibration", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ============================================
// Section 3: Navigation Preferences
// ============================================

@Composable
private fun NavigationPreferencesSection(
    preferences: com.example.lakbaylaya.ui.screens.setting.NavigationPreferences,
    onToggleWalkingMode: () -> Unit,
    onToggleAutoCameraOrientation: () -> Unit,
    onToggleAutoReroute: () -> Unit,
    onToggleRoutePreview: () -> Unit
) {
    SectionCard(
        title = "Navigation",
        titleDescription = "Navigation preferences section. Configure default navigation behavior.",
        icon = Icons.Default.Navigation,
        iconColor = Color(0xFF4CAF50)
    ) {
        // Walking mode
        SettingToggleRow(
            icon = Icons.Default.Navigation,
            title = "Walking Mode Default",
            description = "Start navigation in walking mode",
            isEnabled = preferences.defaultWalkingMode,
            onToggle = onToggleWalkingMode,
            accessibilityDescription = if (preferences.defaultWalkingMode) {
                "Walking mode default is enabled. Tap to disable."
            } else {
                "Walking mode default is disabled. Tap to enable."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Auto camera orientation
        SettingToggleRow(
            icon = Icons.Default.ScreenRotation,
            title = "Auto Camera Orientation",
            description = "Rotate map with your movement",
            isEnabled = preferences.autoCameraOrientation,
            onToggle = onToggleAutoCameraOrientation,
            accessibilityDescription = if (preferences.autoCameraOrientation) {
                "Auto camera orientation is enabled. Map rotates with your movement. Tap to disable."
            } else {
                "Auto camera orientation is disabled. Tap to enable."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Auto reroute
        SettingToggleRow(
            icon = Icons.Default.Refresh,
            title = "Auto Reroute",
            description = "Automatically find new route if off-track",
            isEnabled = preferences.autoReroute,
            onToggle = onToggleAutoReroute,
            accessibilityDescription = if (preferences.autoReroute) {
                "Auto reroute is enabled. App will find new route if you go off track. Tap to disable."
            } else {
                "Auto reroute is disabled. Tap to enable."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Route preview
        SettingToggleRow(
            icon = Icons.Default.Visibility,
            title = "Route Preview",
            description = "Show route overview before starting",
            isEnabled = preferences.routePreviewEnabled,
            onToggle = onToggleRoutePreview,
            accessibilityDescription = if (preferences.routePreviewEnabled) {
                "Route preview is enabled. Shows route overview before navigation. Tap to disable."
            } else {
                "Route preview is disabled. Tap to enable."
            }
        )
    }
}

// ============================================
// Section 4: Safety & Emergency
// ============================================

@Composable
private fun SafetySection(
    settings: com.example.lakbaylaya.ui.screens.setting.SafetySettings,
    onEditContacts: () -> Unit,
    onEditMessage: () -> Unit,
    onToggleAutoArrival: () -> Unit
) {
    SectionCard(
        title = "Safety & Emergency",
        titleDescription = "Safety and Emergency settings section. Configure emergency contacts and notifications.",
        icon = Icons.Default.Security,
        iconColor = Color(0xFFE53935)
    ) {
        // Emergency contacts
        Text(
            text = "Emergency Contacts",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (settings.emergencyContacts.isEmpty()) {
            Text(
                text = "No emergency contacts added",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                modifier = Modifier.semantics {
                    contentDescription = "No emergency contacts added. Tap edit to add contacts."
                }
            )
        } else {
            settings.emergencyContacts.take(2).forEach { contact ->
                EmergencyContactRow(contact = contact)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (settings.emergencyContacts.size > 2) {
                Text(
                    text = "+${settings.emergencyContacts.size - 2} more contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEditContacts,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription =
                        "Edit emergency contacts. Tap to add, edit, or remove emergency contacts."
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Emergency Contacts", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Emergency message
        Text(
            text = "Emergency Message",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Emergency message: ${settings.emergencyMessage}"
                },
            color = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = settings.emergencyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEditMessage,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription =
                        "Change emergency message. Tap to edit the message sent during emergencies."
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Emergency Message", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Auto-arrival notification
        SettingToggleRow(
            icon = Icons.Default.Check,
            title = "Auto-Send Arrival Notification",
            description = "Notify emergency contacts when you arrive safely",
            isEnabled = settings.autoSendArrivalNotification,
            onToggle = onToggleAutoArrival,
            accessibilityDescription = if (settings.autoSendArrivalNotification) {
                "Auto-send arrival notification is enabled. Contacts will be notified when you arrive. Tap to disable."
            } else {
                "Auto-send arrival notification is disabled. Tap to enable."
            }
        )
    }
}

@Composable
private fun EmergencyContactRow(contact: com.example.lakbaylaya.ui.screens.setting.EmergencyContact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Emergency contact: ${contact.name}, ${contact.relationship}, phone number ${contact.phoneNumber}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFFFFEBEE)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = if (contact.relationship.isNotEmpty()) "${contact.relationship} • ${contact.phoneNumber}" else contact.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================
// Section 5: Device & Connectivity
// ============================================

@Composable
private fun DeviceConnectivitySection(
    settings: com.example.lakbaylaya.ui.screens.setting.DeviceSettings,
    isPairing: Boolean,
    onPairDevice: () -> Unit,
    onReconnect: () -> Unit
) {
    SectionCard(
        title = "Device & Connectivity",
        titleDescription = "Device and Connectivity section. Manage wearable device connection.",
        icon = Icons.Default.Watch,
        iconColor = Color(0xFF673AB7)
    ) {
        // Device status
        if (settings.pairedDeviceName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (settings.isDeviceConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    )
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = buildString {
                            append("Wearable device: ${settings.pairedDeviceName}. ")
                            append(if (settings.isDeviceConnected) "Connected. " else "Disconnected. ")
                            settings.deviceBatteryLevel?.let { append("Battery level $it percent.") }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (settings.isDeviceConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (settings.isDeviceConnected) Color(0xFF4CAF50) else Color(0xFFE65100),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settings.pairedDeviceName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = if (settings.isDeviceConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.isDeviceConnected) Color(0xFF4CAF50) else Color(
                            0xFFE65100
                        )
                    )
                }
                if (settings.deviceBatteryLevel != null && settings.isDeviceConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = when {
                                settings.deviceBatteryLevel > 50 -> Color(0xFF4CAF50)
                                settings.deviceBatteryLevel > 20 -> Color(0xFFF57C00)
                                else -> Color(0xFFE53935)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${settings.deviceBatteryLevel}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF424242)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!settings.isDeviceConnected) {
                Button(
                    onClick = onReconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .semantics {
                            contentDescription =
                                "Reconnect device. Tap to reconnect your wearable device."
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reconnect Device", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Pair new device button
        OutlinedButton(
            onClick = onPairDevice,
            enabled = !isPairing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription =
                        "Pair new device. Tap to search and connect a new wearable device."
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isPairing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair New Device", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ============================================
// Section 6: App & Data Settings
// ============================================

@Composable
private fun AppDataSection(
    settings: com.example.lakbaylaya.ui.screens.setting.AppDataSettings,
    onClearHistory: () -> Unit,
    onResetRoutes: () -> Unit,
    onDownloadMaps: () -> Unit,
    onCheckUpdates: () -> Unit
) {
    SectionCard(
        title = "App & Data",
        titleDescription = "App and Data settings section. Manage route history, offline maps, and app updates.",
        icon = Icons.Default.Storage,
        iconColor = Color(0xFF607D8B)
    ) {
        // Route history
        ActionRow(
            icon = Icons.Default.History,
            title = "Clear Route History",
            subtitle = "${settings.routeHistoryCount} routes saved",
            buttonText = "Clear",
            buttonColor = Color(0xFFE53935),
            onClick = onClearHistory,
            accessibilityDescription = "Clear route history. You have ${settings.routeHistoryCount} routes saved. Tap to clear all."
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Familiar routes
        ActionRow(
            icon = Icons.Default.Route,
            title = "Reset Familiar Routes",
            subtitle = "${settings.familiarRoutesCount} familiar routes",
            buttonText = "Reset",
            buttonColor = Color(0xFFE53935),
            onClick = onResetRoutes,
            accessibilityDescription = "Reset familiar routes. You have ${settings.familiarRoutesCount} familiar routes saved. Tap to reset all."
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // Offline maps
        ActionRow(
            icon = Icons.Default.CloudDownload,
            title = "Download Offline Maps",
            subtitle = if (settings.offlineMapsDownloaded) "Maps available offline" else "Not downloaded",
            buttonText = if (settings.offlineMapsDownloaded) "Update" else "Download",
            buttonColor = Color(0xFF1976D2),
            onClick = onDownloadMaps,
            accessibilityDescription = if (settings.offlineMapsDownloaded) {
                "Offline maps are available. Tap to check for updates."
            } else {
                "Offline maps not downloaded. Tap to download for offline use."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(16.dp))

        // App updates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        "App version ${settings.appVersion}. Last checked for updates ${settings.lastUpdateCheck}. Tap to check for updates."
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Version",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "v${settings.appVersion} • Last checked: ${settings.lastUpdateCheck}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            TextButton(
                onClick = onCheckUpdates,
                modifier = Modifier.semantics {
                    contentDescription = "Check for updates"
                }
            ) {
                Text("Check", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit,
    accessibilityDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = buttonColor)
        ) {
            Text(buttonText, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================
// Shared Components
// ============================================

@Composable
private fun SectionCard(
    title: String,
    titleDescription: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = titleDescription
                        heading()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    accessibilityDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}

@Composable
private fun SettingOptionRow(
    icon: ImageVector,
    title: String,
    currentValue: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    accessibilityDescription: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 8.dp)
                .semantics {
                    contentDescription = accessibilityDescription
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Edit,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun OptionItem(
    label: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    description: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isSelected) "$label, currently selected" else description
            },
        color = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) Color(0xFF1976D2) else Color(0xFF424242),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================
// Dialogs
// ============================================

@Composable
private fun EditEmergencyMessageDialog(
    currentMessage: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var message by remember { mutableStateOf(currentMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Emergency Message",
                modifier = Modifier.semantics {
                    contentDescription = "Edit emergency message dialog"
                }
            )
        },
        text = {
            Column {
                Text(
                    text = "This message will be sent to your emergency contacts when you trigger an SOS alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Emergency Message") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Emergency message input field. Current message: $message"
                        }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(message) },
                enabled = message.isNotBlank(),
                modifier = Modifier.semantics {
                    contentDescription = "Save emergency message"
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel editing emergency message"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                modifier = Modifier.semantics {
                    contentDescription = "$title dialog"
                }
            )
        },
        text = {
            Text(
                message,
                modifier = Modifier.semantics {
                    contentDescription = message
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.semantics {
                    contentDescription = "Confirm $confirmText"
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
