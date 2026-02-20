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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
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
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

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
            item {
                VoiceAudioSection(
                    settings = uiState.voiceAudioSettings,
                    onToggleVoice = { viewModel.toggleVoiceGuidance() },
                    onSpeedChange = { viewModel.setVoiceSpeed(it) },
                    onVolumeChange = { viewModel.setVoiceVolume(it) }
                )
            }

            item {
                VibrationSection(
                    settings = uiState.vibrationSettings,
                    isTestingVibration = uiState.isTestingVibration,
                    onToggleVibration = { viewModel.toggleVibration() },
                    onStrengthChange = { viewModel.setVibrationStrength(it) },
                    onLeftPatternChange = { viewModel.setLeftPattern(it) },
                    onRightPatternChange = { viewModel.setRightPattern(it) },
                    onForwardPatternChange = { viewModel.setForwardPattern(it) },
                    onBackwardPatternChange = { viewModel.setBackwardPattern(it) },
                    onArrivalPatternChange = { viewModel.setArrivalPattern(it) },
                    onTestVibration = { viewModel.testVibration() }
                )
            }

            item {
                SafetySection(
                    settings = uiState.safetySettings,
                    onEditContacts = { viewModel.showEditEmergencyContacts() },
                    onEditMessage = { viewModel.showEditEmergencyMessage() },
                    onToggleAutoArrival = { viewModel.toggleAutoArrivalNotification() }
                )
            }

            item {
                AppDataSection(
                    settings = uiState.appDataSettings,
                    onClearHistory = { viewModel.showClearHistoryConfirmation() },
                    onResetRoutes = { viewModel.showResetRoutesConfirmation() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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

@Composable
private fun VoiceAudioSection(
    settings: VoiceAudioSettings,
    onToggleVoice: () -> Unit,
    onSpeedChange: (VoiceSpeed) -> Unit,
    onVolumeChange: (Int) -> Unit
) {
    var showSpeedOptions by remember { mutableStateOf(false) }

    SectionCard(
        title = "Voice & Audio",
        titleDescription = "Voice and Audio settings section.",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        iconColor = Color(0xFF1976D2)
    ) {
        SettingToggleRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "Voice Guidance",
            description = "Spoken turn-by-turn directions",
            isEnabled = settings.voiceGuidanceEnabled,
            onToggle = onToggleVoice,
            accessibilityDescription = if (settings.voiceGuidanceEnabled) "Voice guidance is enabled. Tap to disable." else "Voice guidance is disabled. Tap to enable."
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingOptionRow(
            icon = Icons.Default.Speed,
            title = "Voice Speed",
            currentValue = settings.voiceSpeed.displayName,
            isExpanded = showSpeedOptions,
            onToggleExpand = { showSpeedOptions = !showSpeedOptions },
            accessibilityDescription = "Voice speed is set to ${settings.voiceSpeed.displayName}. Tap to change."
        ) {
            VoiceSpeed.values().forEach { speed ->
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Voice Volume",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF1A1A1A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        var sliderValue by remember { mutableFloatStateOf(settings.voiceVolume.toFloat()) }
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

@Composable
private fun VibrationSection(
    settings: VibrationSettings,
    isTestingVibration: Boolean,
    onToggleVibration: () -> Unit,
    onStrengthChange: (VibrationStrength) -> Unit,
    onLeftPatternChange: (VibrationPattern) -> Unit,
    onRightPatternChange: (VibrationPattern) -> Unit,
    onForwardPatternChange: (VibrationPattern) -> Unit,
    onBackwardPatternChange: (VibrationPattern) -> Unit,
    onArrivalPatternChange: (VibrationPattern) -> Unit,
    onTestVibration: () -> Unit
) {
    var showStrengthOptions by remember { mutableStateOf(false) }
    var showLeftOptions by remember { mutableStateOf(false) }
    var showRightOptions by remember { mutableStateOf(false) }
    var showForwardOptions by remember { mutableStateOf(false) }
    var showBackwardOptions by remember { mutableStateOf(false) }
    var showArrivalOptions by remember { mutableStateOf(false) }

    SectionCard(
        title = "Vibration & Haptic",
        titleDescription = "Vibration and Haptic feedback section.",
        icon = Icons.Default.Vibration,
        iconColor = Color(0xFF9C27B0)
    ) {
        SettingToggleRow(
            icon = Icons.Default.Vibration,
            title = "Vibration Feedback",
            description = "Haptic feedback during navigation",
            isEnabled = settings.vibrationEnabled,
            onToggle = onToggleVibration,
            accessibilityDescription = if (settings.vibrationEnabled) "Vibration feedback is enabled. Tap to disable." else "Vibration feedback is disabled. Tap to enable."
        )

        if (settings.vibrationEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(16.dp))

            SettingOptionRow(
                icon = Icons.Default.Speed,
                title = "Vibration Strength",
                currentValue = settings.vibrationStrength.displayName,
                isExpanded = showStrengthOptions,
                onToggleExpand = { showStrengthOptions = !showStrengthOptions },
                accessibilityDescription = "Vibration strength is set to ${settings.vibrationStrength.displayName}. Tap to change."
            ) {
                VibrationStrength.values().forEach { strength ->
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

            SettingOptionRow(
                icon = Icons.Default.ArrowBack,
                title = "Left",
                currentValue = settings.leftPattern.displayName,
                isExpanded = showLeftOptions,
                onToggleExpand = { showLeftOptions = !showLeftOptions },
                accessibilityDescription = "Left vibration pattern is ${settings.leftPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.values().forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.leftPattern == pattern,
                        onClick = {
                            onLeftPatternChange(pattern)
                            showLeftOptions = false
                        },
                        description = "Set left vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingOptionRow(
                icon = Icons.Default.ArrowForward,
                title = "Right",
                currentValue = settings.rightPattern.displayName,
                isExpanded = showRightOptions,
                onToggleExpand = { showRightOptions = !showRightOptions },
                accessibilityDescription = "Right vibration pattern is ${settings.rightPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.values().forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.rightPattern == pattern,
                        onClick = {
                            onRightPatternChange(pattern)
                            showRightOptions = false
                        },
                        description = "Set right vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingOptionRow(
                icon = Icons.Default.PlayArrow,
                title = "Forward",
                currentValue = settings.forwardPattern.displayName,
                isExpanded = showForwardOptions,
                onToggleExpand = { showForwardOptions = !showForwardOptions },
                accessibilityDescription = "Forward vibration pattern is ${settings.forwardPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.values().forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.forwardPattern == pattern,
                        onClick = {
                            onForwardPatternChange(pattern)
                            showForwardOptions = false
                        },
                        description = "Set forward vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingOptionRow(
                icon = Icons.Default.Reply,
                title = "Backward",
                currentValue = settings.backwardPattern.displayName,
                isExpanded = showBackwardOptions,
                onToggleExpand = { showBackwardOptions = !showBackwardOptions },
                accessibilityDescription = "Backward vibration pattern is ${settings.backwardPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.values().forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.backwardPattern == pattern,
                        onClick = {
                            onBackwardPatternChange(pattern)
                            showBackwardOptions = false
                        },
                        description = "Set backward vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingOptionRow(
                icon = Icons.Default.Explore,
                title = "Arrival",
                currentValue = settings.arrivalPattern.displayName,
                isExpanded = showArrivalOptions,
                onToggleExpand = { showArrivalOptions = !showArrivalOptions },
                accessibilityDescription = "Arrival vibration pattern is ${settings.arrivalPattern.displayName}. Tap to change."
            ) {
                VibrationPattern.values().forEach { pattern ->
                    OptionItem(
                        label = pattern.displayName,
                        subtitle = pattern.description,
                        isSelected = settings.arrivalPattern == pattern,
                        onClick = {
                            onArrivalPatternChange(pattern)
                            showArrivalOptions = false
                        },
                        description = "Set arrival vibration to ${pattern.displayName}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onTestVibration,
                enabled = !isTestingVibration,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTestingVibration) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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

@Composable
private fun SafetySection(
    settings: SafetySettings,
    onEditContacts: () -> Unit,
    onEditMessage: () -> Unit,
    onToggleAutoArrival: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var useGateway by remember { mutableStateOf(prefs.getBoolean("use_sms_gateway", false)) }

    SectionCard(
        title = "Safety & Emergency",
        titleDescription = "Safety and Emergency settings section.",
        icon = Icons.Default.Security,
        iconColor = Color(0xFFE53935)
    ) {
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
                color = Color(0xFF666666)
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
                .height(48.dp),
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
            modifier = Modifier.fillMaxWidth(),
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
                .height(48.dp),
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

        SettingToggleRow(
            icon = Icons.Default.Check,
            title = "Auto-Send Arrival Notification",
            description = "Notify emergency contacts when you arrive safely",
            isEnabled = settings.autoSendArrivalNotification,
            onToggle = onToggleAutoArrival,
            accessibilityDescription = if (settings.autoSendArrivalNotification) "Auto-send arrival notification is enabled. Tap to disable." else "Auto-send arrival notification is disabled. Tap to enable."
        )

        Spacer(modifier = Modifier.height(12.dp))

        // New toggle: Use SMS Gateway
        SettingToggleRow(
            icon = Icons.Default.Phone,
            title = "Use SMS Gateway",
            description = "Send emergency messages via configured SMS gateway instead of device SMS",
            isEnabled = useGateway,
            onToggle = {
                useGateway = !useGateway
                prefs.edit { putBoolean("use_sms_gateway", useGateway) }
            },
            accessibilityDescription = if (useGateway) "Sending via SMS gateway is enabled." else "Sending via device SMS by default."
        )
    }
}

@Composable
private fun EmergencyContactRow(contact: EmergencyContact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFFFEBEE)) {
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

@Composable
private fun AppDataSection(
    settings: AppDataSettings,
    onClearHistory: () -> Unit,
    onResetRoutes: () -> Unit
) {
    SectionCard(
        title = "App & Data",
        titleDescription = "App and Data settings section.",
        icon = Icons.Default.Storage,
        iconColor = Color(0xFF607D8B)
    ) {
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

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    text = "v${settings.appVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.fillMaxWidth(),
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
            .padding(vertical = 8.dp),
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
                .padding(vertical = 8.dp),
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
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically
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

@Composable
private fun EditEmergencyMessageDialog(
    currentMessage: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var message by remember { mutableStateOf(currentMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Emergency Message") },
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(message) }, enabled = message.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
