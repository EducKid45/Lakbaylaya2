package com.example.lakbaylaya.ui.screens.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lakbaylaya.ui.screens.map.navigation.voice.NavigationVoiceManager
import com.example.lakbaylaya.ui.screens.map.navigation.voice.VoiceCommand

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToMap: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Retry state for home voice commands
    val homeVoiceRetries = remember { mutableStateOf(0) }
    val maxHomeVoiceRetries = 2
    val homeRelaunchFlag = remember { mutableStateOf(false) }

    // NavigationVoiceManager to process commands and speak feedback (reuses existing manager)
    val navigationVoiceManager = remember {
        val handler =
            object : com.example.lakbaylaya.ui.screens.map.navigation.voice.VoiceCommandHandler {
                override fun onRepeatInstruction() {
                    // Home doesn't run navigation; just reflect in UI
                    viewModel.executeVoiceCommand("RepeatInstruction")
                }

                override fun onPauseNavigation() {
                    viewModel.executeVoiceCommand("PauseNavigation")
                }

                override fun onResumeNavigation() {
                    viewModel.executeVoiceCommand("ResumeNavigation")
                }

                override fun onCheckCurrentPosition(latitude: Double, longitude: Double) {
                    viewModel.executeVoiceCommand("CheckCurrentPosition: $latitude,$longitude")
                }

                override fun onDistanceToDestination(distanceMeters: Double) {
                    viewModel.executeVoiceCommand("DistanceToDestination: $distanceMeters")
                }

                override fun onSwitchRoute() {
                    viewModel.executeVoiceCommand("SwitchRoute")
                }

                override fun onCancelNavigation() {
                    viewModel.executeVoiceCommand("CancelNavigation")
                }

                override fun onActivateEmergencyMode() {
                    viewModel.executeVoiceCommand("ActivateEmergencyMode")
                }
            }

        NavigationVoiceManager(application, handler)
    }

    // coroutine scope for launching delayed relaunches
    val composeScope = rememberCoroutineScope()

    // Helper vibrator
    fun vibrateShort() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
            android.util.Log.w("HomeScreen", "Vibration failed: ${e.message}")
        }
    }

    // Launchers for system speech recognizer and mic permission
    val homeVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Keep listening state controlled by ViewModel; only stop when user cancels or retries exhausted
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.firstOrNull { it.isNotBlank() }?.trim() ?: ""

                if (recognizedText.isNotBlank()) {
                    android.util.Log.d("HomeScreen", "Home voice recognized: '$recognizedText'")

                    // First, check for Home-specific commands (Start navigation, Go to <destination>, Emergency, etc.)
                    when (val homeAction = parseHomeAction(recognizedText)) {
                        is HomeAction.StartNavigation -> {
                            navigationVoiceManager.speak("Opening navigation")
                            vibrateShort()
                            viewModel.executeVoiceCommand("StartNavigation")
                            onNavigateToMap()
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.GoTo -> {
                            navigationVoiceManager.speak("Navigating to ${homeAction.destination}")
                            vibrateShort()
                            viewModel.executeVoiceCommand("GoTo:${homeAction.destination}")
                            onNavigateToMap()
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.SendEmergencyAlert -> {
                            navigationVoiceManager.speak("Sending emergency alert")
                            vibrateShort()
                            viewModel.executeVoiceCommand("SendEmergencyAlert")
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.CallEmergencyContact -> {
                            navigationVoiceManager.speak("Calling your emergency contact")
                            vibrateShort()
                            viewModel.executeVoiceCommand("CallEmergencyContact")
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.CheckDeviceStatus -> {
                            // Query status from ViewModel and speak a summary
                            viewModel.checkInternetStatus()
                            viewModel.checkGpsStatus()
                            viewModel.checkWearableStatus()
                            val statusText =
                                buildDeviceStatusText(viewModel.uiState.value.deviceStatus)
                            navigationVoiceManager.speak(statusText)
                            vibrateShort()
                            viewModel.executeVoiceCommand("CheckDeviceStatus")
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.ShowCurrentLocation -> {
                            navigationVoiceManager.speak("Showing current location on the map")
                            vibrateShort()
                            viewModel.executeVoiceCommand("ShowCurrentLocation")
                            onNavigateToMap()
                            homeVoiceRetries.value = 0
                        }

                        is HomeAction.ShowFamiliarRoutes -> {
                            navigationVoiceManager.speak("Showing your familiar routes")
                            vibrateShort()
                            viewModel.executeVoiceCommand("ShowFamiliarRoutes")
                            homeVoiceRetries.value = 0
                        }

                        HomeAction.None -> {
                            // Not a home-specific command: fall back to navigation voice command processing
                            val command = navigationVoiceManager.processVoiceCommand(recognizedText)

                            if (command is VoiceCommand.UnknownCommand) {
                                homeVoiceRetries.value =
                                    (homeVoiceRetries.value + 1).coerceAtMost(maxHomeVoiceRetries)
                                if (homeVoiceRetries.value <= maxHomeVoiceRetries) {
                                    navigationVoiceManager.speak("Sorry, I didn't catch that. Please say the command again.")
                                    vibrateShort()
                                    if (uiState.isListening) homeRelaunchFlag.value = true
                                } else {
                                    navigationVoiceManager.speak("Sorry, I couldn't understand. Try again later.")
                                    homeVoiceRetries.value = 0
                                    viewModel.stopVoiceRecognition()
                                }
                            } else {
                                // Successful command - reset retry counter and record
                                homeVoiceRetries.value = 0
                                vibrateShort()
                                viewModel.executeVoiceCommand(command.commandName)
                            }
                        }
                    }
                } else {
                    android.util.Log.d("HomeScreen", "Home voice result empty")
                    navigationVoiceManager.speak("No speech detected. Please say the command again.")
                    homeVoiceRetries.value =
                        (homeVoiceRetries.value + 1).coerceAtMost(maxHomeVoiceRetries)
                    if (homeVoiceRetries.value <= maxHomeVoiceRetries) {
                        vibrateShort()
                        if (uiState.isListening) homeRelaunchFlag.value = true
                    } else {
                        navigationVoiceManager.speak("No input detected. Cancelling voice mode.")
                        homeVoiceRetries.value = 0
                        viewModel.stopVoiceRecognition()
                    }
                }
            }

            android.app.Activity.RESULT_CANCELED -> {
                android.util.Log.d("HomeScreen", "Home voice cancelled")
                homeVoiceRetries.value = 0
                viewModel.stopVoiceRecognition()
            }

            else -> {
                android.util.Log.e(
                    "HomeScreen",
                    "Home voice failed with code: ${result.resultCode}"
                )
                homeVoiceRetries.value = 0
                viewModel.stopVoiceRecognition()
            }
        }
    }

    val homeMicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Start listening immediately
            viewModel.startVoiceRecognition()
            navigationVoiceManager.speakListeningPrompt()

            // Slight delay then launch using coroutine scope (can't use LaunchedEffect here)
            composeScope.launch {
                kotlinx.coroutines.delay(400)
                launchHomeVoiceCommand(context, homeVoiceLauncher)
            }
        } else {
            viewModel.stopVoiceRecognition()
        }
    }

    // Handle relaunch requests triggered after TTS prompts
    LaunchedEffect(homeRelaunchFlag.value) {
        if (homeRelaunchFlag.value) {
            if (!uiState.isListening) {
                homeRelaunchFlag.value = false
                return@LaunchedEffect
            }
            try {
                kotlinx.coroutines.delay(600)
                navigationVoiceManager.speakListeningPrompt()
                kotlinx.coroutines.delay(350)
                launchHomeVoiceCommand(context, homeVoiceLauncher)
            } catch (e: Exception) {
                android.util.Log.w(
                    "HomeScreen",
                    "Failed to relaunch home voice recognizer: ${e.message}"
                )
            } finally {
                homeRelaunchFlag.value = false
            }
        }
    }

    // When viewModel signals start listening, request permission / launch recognizer
    LaunchedEffect(uiState.isListening) {
        if (uiState.isListening) {
            // Check mic permission
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                // speak cue then launch
                navigationVoiceManager.speakListeningPrompt()
                kotlinx.coroutines.delay(350)
                launchHomeVoiceCommand(context, homeVoiceLauncher)
            } else {
                homeMicPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        } else {
            // stopped listening - reset retries
            homeVoiceRetries.value = 0
            homeRelaunchFlag.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Welcome Header
            item {
                WelcomeHeader()
            }

            // 2. Main Voice Command Button
            item {
                VoiceCommandButton(
                    isListening = uiState.isListening,
                    onStartListening = { viewModel.startVoiceRecognition() },
                    onStopListening = { viewModel.stopVoiceRecognition() }
                )
            }

            // 3. Voice Command Help List
            item {
                VoiceCommandHelpList(
                    isExpanded = uiState.isCommandListExpanded,
                    onToggleExpand = { viewModel.toggleCommandList() },
                    onCommandTap = { command ->
                        // Map the displayed example command text to the same actions used by
                        // the voice recognizer path so taps behave like spoken commands.
                        val t = command.trim().lowercase()

                        when {
                            t == "start navigation" || t.contains("start navigation") -> {
                                navigationVoiceManager.speak("Opening navigation")
                                vibrateShort()
                                viewModel.executeVoiceCommand("StartNavigation")
                                onNavigateToMap()
                            }

                            t.startsWith("go to") || t.startsWith("navigate to") || t.contains("go to [") -> {
                                // Placeholder destination - open map so user can search / select destination
                                navigationVoiceManager.speak("Open the map to choose a destination")
                                vibrateShort()
                                viewModel.executeVoiceCommand("GoTo:")
                                onNavigateToMap()
                            }

                            t == "send emergency alert" || t.contains("send emergency") || t.contains(
                                "send help"
                            ) -> {
                                navigationVoiceManager.speak("Sending emergency alert")
                                vibrateShort()
                                viewModel.executeVoiceCommand("SendEmergencyAlert")
                            }

                            t == "call emergency contact" || t.contains("call emergency") || t.contains(
                                "call my contact"
                            ) -> {
                                navigationVoiceManager.speak("Calling your emergency contact")
                                vibrateShort()
                                viewModel.executeVoiceCommand("CallEmergencyContact")
                            }

                            t == "check device status" || t.contains("device status") -> {
                                viewModel.checkInternetStatus()
                                viewModel.checkGpsStatus()
                                viewModel.checkWearableStatus()
                                val statusText =
                                    buildDeviceStatusText(viewModel.uiState.value.deviceStatus)
                                navigationVoiceManager.speak(statusText)
                                vibrateShort()
                                viewModel.executeVoiceCommand("CheckDeviceStatus")
                            }

                            t == "show current location" || t.contains("current location") || t.contains(
                                "where am i"
                            ) -> {
                                navigationVoiceManager.speak("Showing current location on the map")
                                vibrateShort()
                                viewModel.executeVoiceCommand("ShowCurrentLocation")
                                onNavigateToMap()
                            }

                            t == "show familiar routes" || t.contains("familiar routes") || t.contains(
                                "my routes"
                            ) -> {
                                navigationVoiceManager.speak("Showing your familiar routes")
                                vibrateShort()
                                viewModel.executeVoiceCommand("ShowFamiliarRoutes")
                            }

                            else -> {
                                // Unknown example -- just record it in the ViewModel
                                viewModel.executeVoiceCommand(command)
                            }
                        }
                    }
                )
            }

            // 4. Device Status Panel
            item {
                DeviceStatusPanel(deviceStatus = uiState.deviceStatus)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LakbayLaya",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics {
                contentDescription = "LakbayLaya, Navigation App"
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Voice Navigation Assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            modifier = Modifier.semantics {
                contentDescription = "Voice Navigation Assistant"
            }
        )
    }
}

@Composable
private fun VoiceCommandButton(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Large circular microphone button
        Surface(
            modifier = Modifier
                .size(120.dp)
                .semantics {
                    contentDescription = if (isListening) {
                        "Listening for voice command. Tap to stop."
                    } else {
                        "Start voice command. Tap to speak a command."
                    }
                }
                .clickable(
                    onClick = {
                        if (isListening) onStopListening() else onStartListening()
                    }
                ),
            shape = CircleShape,
            color = if (isListening) Color(0xFFE53935) else Color(0xFF1976D2),
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null, // Described by parent semantics
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instruction text
        Text(
            text = if (isListening) "Listening..." else "Tap and say a command",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = if (isListening) Color(0xFFE53935) else Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                contentDescription = if (isListening) {
                    "Currently listening for your voice command"
                } else {
                    "Tap the microphone button and say a command"
                }
            }
        )
    }
}

@Composable
private fun VoiceCommandHelpList(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCommandTap: (String) -> Unit
) {
    val commands = listOf(
        "Start navigation",
        "Go to [destination]",
        "Send emergency alert",
        "Call emergency contact",
        "Check device status",
        "Show current location",
        "Show familiar routes"
    )

    // Light tinted panel to indicate guidance for speaking
    val panelBackground = Color(0xFFE3F2FD) // soft light blue
    val titleColor = Color(0xFF0D47A1) // dark blue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isExpanded) {
                    "Voice command examples expanded. This is guidance for speaking. Tap any command to execute it. Tap header to collapse."
                } else {
                    "Voice command examples collapsed. This is guidance for speaking. Tap to expand and see available commands."
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = panelBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with small mic icon, title, and expand/collapse indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleExpand)
                    .semantics {
                        contentDescription = if (isExpanded) {
                            "You can say, heading. Tap to collapse command list."
                        } else {
                            "You can say, heading. Tap to expand and see available commands."
                        }
                    }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small mic icon in accent color
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic icon",
                        tint = titleColor,
                        modifier = Modifier
                            .size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Commands:",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = titleColor,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, // Described by parent semantics
                    tint = titleColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Caption to visually indicate the panel is guidance for speaking
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is guidance for speaking",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
                    .semantics { contentDescription = "This is guidance for speaking" }
            )

            // Command list (only shown when expanded)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                commands.forEachIndexed { index, command ->
                    CommandItem(
                        command = command,
                        onClick = { onCommandTap(command) }
                    )
                    if (index < commands.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Command: $command. Tap to execute this command."
            },
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Command text with minimum touch target
            Text(
                text = "\"$command\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp
                ),
                color = Color(0xFF000000), // changed to pure black
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DeviceStatusPanel(
    deviceStatus: DeviceStatus
) {
    val statusText = buildDeviceStatusText(deviceStatus)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = statusText
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Device Status",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .semantics { heading() }
                    .padding(bottom = 16.dp)
            )

            // Internet status
            StatusRow(
                label = "Internet",
                status = when (deviceStatus.internetStatus) {
                    ConnectionStatus.ONLINE -> "Online"
                    ConnectionStatus.OFFLINE -> "Offline"
                    else -> "Unknown"
                },
                icon = when (deviceStatus.internetStatus) {
                    ConnectionStatus.ONLINE -> Icons.Default.Wifi
                    ConnectionStatus.OFFLINE -> Icons.Default.WifiOff
                    else -> Icons.Default.SignalWifiStatusbarConnectedNoInternet4
                },
                statusColor = when (deviceStatus.internetStatus) {
                    ConnectionStatus.ONLINE -> Color(0xFF4CAF50)
                    ConnectionStatus.OFFLINE -> Color(0xFFE53935)
                    else -> Color(0xFF9E9E9E)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GPS status
            StatusRow(
                label = "GPS",
                status = when (deviceStatus.gpsStatus) {
                    GpsStatus.READY -> "Ready"
                    GpsStatus.SEARCHING -> "Searching"
                    GpsStatus.UNAVAILABLE -> "Unavailable"
                },
                icon = when (deviceStatus.gpsStatus) {
                    GpsStatus.READY -> Icons.Default.GpsFixed
                    else -> Icons.Default.GpsNotFixed
                },
                statusColor = when (deviceStatus.gpsStatus) {
                    GpsStatus.READY -> Color(0xFF4CAF50)
                    GpsStatus.SEARCHING -> Color(0xFFF57C00)
                    GpsStatus.UNAVAILABLE -> Color(0xFFE53935)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Wearable status
            StatusRow(
                label = "Wearable Device",
                status = when (deviceStatus.wearableStatus) {
                    ConnectionStatus.CONNECTED -> "Connected"
                    ConnectionStatus.DISCONNECTED -> "Not Connected"
                    else -> "Unknown"
                },
                icon = when (deviceStatus.wearableStatus) {
                    ConnectionStatus.CONNECTED -> Icons.Default.Bluetooth
                    else -> Icons.Default.BluetoothDisabled
                },
                statusColor = when (deviceStatus.wearableStatus) {
                    ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                    else -> Color(0xFF9E9E9E)
                }
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    status: String,
    icon: ImageVector,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $status"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp
            ),
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = statusColor
        )
    }
}

private fun buildDeviceStatusText(deviceStatus: DeviceStatus): String {
    val internet = when (deviceStatus.internetStatus) {
        ConnectionStatus.ONLINE -> "Internet is online"
        ConnectionStatus.OFFLINE -> "Internet is offline"
        else -> "Internet status unknown"
    }
    val gps = when (deviceStatus.gpsStatus) {
        GpsStatus.READY -> "GPS is ready"
        GpsStatus.SEARCHING -> "GPS is searching"
        GpsStatus.UNAVAILABLE -> "GPS is unavailable"
    }
    val wearable = when (deviceStatus.wearableStatus) {
        ConnectionStatus.CONNECTED -> "Wearable device is connected"
        ConnectionStatus.DISCONNECTED -> "Wearable device is not connected"
        else -> "Wearable device status unknown"
    }
    return "Device Status: $internet, $gps, $wearable"
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

/**
 * Launch the system speech recognizer for Home voice commands
 */
private fun launchHomeVoiceCommand(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            val deviceLocale = java.util.Locale.getDefault()
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "🎤 Say a command for LakbayLaya")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        val pm = context.packageManager
        val activities = pm.queryIntentActivities(intent, 0)
        if (activities.isNotEmpty()) {
            launcher.launch(intent)
        } else {
            android.util.Log.e("HomeScreen", "No speech recognizer available")
        }
    } catch (e: Exception) {
        android.util.Log.e("HomeScreen", "Failed to launch speech recognizer: ${e.message}")
    }
}

// Home-specific action types and parser
sealed class HomeAction {
    object None : HomeAction()
    object StartNavigation : HomeAction()
    data class GoTo(val destination: String) : HomeAction()
    object SendEmergencyAlert : HomeAction()
    object CallEmergencyContact : HomeAction()
    object CheckDeviceStatus : HomeAction()
    object ShowCurrentLocation : HomeAction()
    object ShowFamiliarRoutes : HomeAction()
}

// Lightweight parser for typical home commands (case-insensitive)
fun parseHomeAction(text: String): HomeAction {
    val t = text.trim().lowercase()

    // Start navigation
    if (t.contains("start navigation") || t.startsWith("navigate") || t.startsWith("start route")) {
        return HomeAction.StartNavigation
    }

    // Go to <destination>
    val goPrefixes = listOf("go to ", "navigate to ", "take me to ", "directions to ")
    for (p in goPrefixes) {
        if (t.startsWith(p)) {
            val dest = text.substring(p.length).trim()
            if (dest.isNotEmpty()) return HomeAction.GoTo(dest)
        }
    }

    // Emergency actions
    if (t.contains("emergency") || t.contains("send emergency") || t.contains("send help") || t.contains(
            "help me"
        )
    ) {
        return HomeAction.SendEmergencyAlert
    }

    if (t.contains("call emergency") || t.contains("call contact") || t.contains("call my contact")) {
        return HomeAction.CallEmergencyContact
    }

    // Device queries
    if (t.contains("device status") || t.contains("check device status") || t.contains("status of device")) {
        return HomeAction.CheckDeviceStatus
    }

    if (t.contains("current location") || t.contains("where am i") || t.contains("my location")) {
        return HomeAction.ShowCurrentLocation
    }

    if (t.contains("familiar routes") || t.contains("my routes") || t.contains("saved routes")) {
        return HomeAction.ShowFamiliarRoutes
    }

    return HomeAction.None
}
