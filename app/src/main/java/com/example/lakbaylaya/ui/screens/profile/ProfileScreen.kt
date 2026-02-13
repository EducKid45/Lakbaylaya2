package com.example.lakbaylaya.ui.screens.profile

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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
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
            // 1. User Information Section
            item {
                UserInformationSection(profile = uiState.userProfile)
            }

            // 2. Health & Mobility Progress
            item {
                HealthProgressSection(stats = uiState.healthStats)
            }

            // 3. Emergency Overview (Read-Only)
            item {
                EmergencyOverviewSection(settings = uiState.emergencySettings)
            }

            // 4. Device Status Overview (Read-Only)
            item {
                DeviceStatusOverviewSection(deviceStatus = uiState.deviceStatus)
            }

            // 5. Minimal Profile Settings
            item {
                ProfileSettingsSection(
                    settings = uiState.profileSettings,
                    onToggleVoiceFeedback = { viewModel.toggleVoiceFeedbackForProgress() }
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ============================================
// Section 1: User Information (Display Only)
// ============================================

@Composable
private fun UserInformationSection(profile: UserProfile) {
    SectionCard(
        title = "User Information",
        titleDescription = "User Information section. Your personal details and saved locations.",
        icon = Icons.Default.Person,
        iconColor = Color(0xFF1976D2)
    ) {
        // User name
        if (profile.name.isNotEmpty()) {
            InfoRow(
                icon = Icons.Default.Person,
                iconColor = Color(0xFF1976D2),
                label = "Name",
                value = profile.name,
                accessibilityDescription = "Your name is ${profile.name}"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Home location
        InfoRow(
            icon = Icons.Default.Home,
            iconColor = Color(0xFF4CAF50),
            label = "Home Location",
            value = profile.homeLocation.ifEmpty { "Not set" },
            accessibilityDescription = if (profile.homeLocation.isNotEmpty())
                "Home location: ${profile.homeLocation}"
            else
                "Home location: Not set"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Work/School location
        InfoRow(
            icon = Icons.Default.Work,
            iconColor = Color(0xFFFF9800),
            label = "Work/School Location",
            value = profile.workLocation.ifEmpty { "Not set" },
            accessibilityDescription = if (profile.workLocation.isNotEmpty())
                "Work or school location: ${profile.workLocation}"
            else
                "Work or school location: Not set"
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    accessibilityDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                fontSize = 13.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

// ============================================
// Section 2: Health & Mobility Progress
// ============================================

@Composable
private fun HealthProgressSection(stats: HealthStats) {
    SectionCard(
        title = "Health & Mobility",
        titleDescription = "Health and Mobility section. Your walking progress and activity statistics.",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        iconColor = Color(0xFF4CAF50)
    ) {
        // Today's stats heading
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .semantics { heading() }
                .padding(bottom = 12.dp)
        )

        // Today stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressStatCard(
                value = "${stats.distanceWalkedTodayKm}",
                unit = "km",
                label = "Distance",
                accessibilityDescription = "You walked ${stats.distanceWalkedTodayKm} kilometers today.",
                modifier = Modifier.weight(1f)
            )
            ProgressStatCard(
                value = "${stats.stepsToday}",
                unit = "steps",
                label = "Steps",
                accessibilityDescription = "You took ${stats.stepsToday} steps today.",
                modifier = Modifier.weight(1f)
            )
            ProgressStatCard(
                value = "${stats.routesCompletedToday}",
                unit = "routes",
                label = "Completed",
                accessibilityDescription = "You completed ${stats.routesCompletedToday} routes today.",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // This Week heading
        Text(
            text = "This Week",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .semantics { heading() }
                .padding(bottom = 12.dp)
        )

        // Week stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressStatCard(
                value = "${stats.distanceWalkedWeekKm}",
                unit = "km",
                label = "Distance",
                accessibilityDescription = "You walked ${stats.distanceWalkedWeekKm} kilometers this week.",
                modifier = Modifier.weight(1f)
            )
            ProgressStatCard(
                value = "${stats.stepsWeek}",
                unit = "steps",
                label = "Steps",
                accessibilityDescription = "You took ${stats.stepsWeek} steps this week.",
                modifier = Modifier.weight(1f)
            )
            ProgressStatCard(
                value = "${stats.routesCompletedWeek}",
                unit = "routes",
                label = "Completed",
                accessibilityDescription = "You completed ${stats.routesCompletedWeek} routes this week.",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProgressStatCard(
    value: String,
    unit: String,
    label: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = Color(0xFF2E7D32)
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF424242),
                fontSize = 12.sp
            )
        }
    }
}

// ============================================
// Section 3: Emergency Overview (Read-Only)
// ============================================

@Composable
private fun EmergencyOverviewSection(settings: EmergencySettings) {
    val contactCount = settings.contacts.size
    val contactsDescription = when (contactCount) {
        0 -> "No emergency contacts saved."
        1 -> "Emergency contacts: 1 saved."
        else -> "Emergency contacts: $contactCount saved."
    }

    SectionCard(
        title = "Emergency Overview",
        titleDescription = "Emergency Overview section. Quick view of your safety settings.",
        icon = Icons.Default.ContactPhone,
        iconColor = Color(0xFFE53935)
    ) {
        // Emergency contacts count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFEBEE))
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contactsDescription
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFE53935).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = when (contactCount) {
                        0 -> "None saved"
                        1 -> "1 contact saved"
                        else -> "$contactCount contacts saved"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE53935)
                )
            }
            // Show contact names if available
            if (contactCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE53935)
                ) {
                    Text(
                        text = "$contactCount",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto-arrival notification status
        val arrivalStatus = if (settings.autoArrivalNotification) "enabled" else "disabled"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Auto-arrival notifications are $arrivalStatus."
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (settings.autoArrivalNotification)
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    Color(0xFF9E9E9E).copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (settings.autoArrivalNotification) Color(0xFF4CAF50) else Color(
                            0xFF9E9E9E
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Arrival Notifications",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = if (settings.autoArrivalNotification) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settings.autoArrivalNotification) Color(0xFF4CAF50) else Color(
                        0xFF9E9E9E
                    )
                )
            }
            if (settings.autoArrivalNotification) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============================================
// Section 4: Device Status Overview (Read-Only)
// ============================================

@Composable
private fun DeviceStatusOverviewSection(deviceStatus: DeviceStatus) {
    val wearable = deviceStatus.wearableDevice
    val gps = deviceStatus.gpsStatus

    // Build wearable accessibility description
    val wearableDescription = if (wearable.isConnected) {
        val battery = wearable.batteryLevel?.let { "Battery $it percent." } ?: ""
        "Wearable connected. ${wearable.name}. $battery"
    } else {
        "Wearable disconnected."
    }

    // Build GPS accessibility description
    val gpsDescription = when (gps) {
        GpsStatus.READY -> "GPS is ready."
        GpsStatus.SEARCHING -> "GPS is searching for signal."
        GpsStatus.UNAVAILABLE -> "GPS is unavailable."
    }

    SectionCard(
        title = "Device Status",
        titleDescription = "Device Status section. Shows connected devices and GPS status.",
        icon = Icons.Default.Watch,
        iconColor = Color(0xFF9C27B0)
    ) {
        // Wearable device status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (wearable.isConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = wearableDescription
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (wearable.isConnected)
                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                else
                    Color(0xFFE65100).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (wearable.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = if (wearable.isConnected) Color(0xFF4CAF50) else Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wearable.name.ifEmpty { "No Device" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = if (wearable.isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (wearable.isConnected) Color(0xFF4CAF50) else Color(0xFFE65100)
                )
            }
            // Battery level
            if (wearable.batteryLevel != null && wearable.isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = when {
                            wearable.batteryLevel > 50 -> Color(0xFF4CAF50)
                            wearable.batteryLevel > 20 -> Color(0xFFF57C00)
                            else -> Color(0xFFE53935)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${wearable.batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF424242)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // GPS status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (gps) {
                        GpsStatus.READY -> Color(0xFFE8F5E9)
                        GpsStatus.SEARCHING -> Color(0xFFFFF3E0)
                        GpsStatus.UNAVAILABLE -> Color(0xFFFFEBEE)
                    }
                )
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = gpsDescription
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = when (gps) {
                    GpsStatus.READY -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    GpsStatus.SEARCHING -> Color(0xFFF57C00).copy(alpha = 0.2f)
                    GpsStatus.UNAVAILABLE -> Color(0xFFE53935).copy(alpha = 0.2f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (gps == GpsStatus.READY) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = when (gps) {
                            GpsStatus.READY -> Color(0xFF4CAF50)
                            GpsStatus.SEARCHING -> Color(0xFFF57C00)
                            GpsStatus.UNAVAILABLE -> Color(0xFFE53935)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GPS Status",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = gps.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (gps) {
                        GpsStatus.READY -> Color(0xFF4CAF50)
                        GpsStatus.SEARCHING -> Color(0xFFF57C00)
                        GpsStatus.UNAVAILABLE -> Color(0xFFE53935)
                    }
                )
            }
            if (gps == GpsStatus.READY) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============================================
// Section 5: Minimal Profile Settings
// ============================================

@Composable
private fun ProfileSettingsSection(
    settings: ProfileSettings,
    onToggleVoiceFeedback: () -> Unit
) {
    SectionCard(
        title = "Profile Settings",
        titleDescription = "Profile Settings section. Optional toggles for personal monitoring.",
        icon = Icons.Default.RecordVoiceOver,
        iconColor = Color(0xFF00BCD4)
    ) {
        // Voice feedback for progress toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleVoiceFeedback)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (settings.voiceFeedbackForProgress) {
                        "Voice feedback for progress updates is enabled. Tap to disable."
                    } else {
                        "Voice feedback for progress updates is disabled. Tap to enable."
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFF00BCD4).copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice Feedback for Progress",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "Announce walking and route updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            Switch(
                checked = settings.voiceFeedbackForProgress,
                onCheckedChange = { onToggleVoiceFeedback() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50)
                )
            )
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
            // Section header
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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
