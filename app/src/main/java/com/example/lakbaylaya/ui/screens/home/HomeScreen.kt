package com.example.lakbaylaya.ui.screens.home

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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToMap: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // NOTE: This screen no longer hosts its own Scaffold/topBar.
    // The parent `LakbayLayaApp` provides the TopBar via its Scaffold so this content
    // should be treated as the scaffold content and will receive padding from the parent.
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
                        viewModel.executeVoiceCommand(command)
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
