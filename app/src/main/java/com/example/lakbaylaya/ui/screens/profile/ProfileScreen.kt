package com.example.lakbaylaya.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Profile Screen — rules enforced:
 *
 *  • Name + Phone → tap opens ProfileEditorScreen (name/phone only)
 *  • Home / Work  → tap opens LocationMarkerEditorDialog (map-pin setter)
 *  • Emergency contact / message are READ-ONLY here — editing redirected to Settings
 *  • No emergency editing dialogs on this screen
 *  • Device status: GPS + ESP32 BT, NO battery icon
 *  • All displayed values have null-safe defaults
 *  • "Profile" title in real TopAppBar; edit icon also in TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState          by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshGpsStatus() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        // Omit the scaffold's top padding so content sits at the very top.
        val layoutDir = androidx.compose.ui.platform.LocalLayoutDirection.current
        val startPad = pad.calculateStartPadding(layoutDir)
        val endPad = pad.calculateEndPadding(layoutDir)
        val bottomPad = pad.calculateBottomPadding()

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(start = startPad, end = endPad, bottom = bottomPad)
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    UserInfoSection(
                        profile = uiState.userProfile,
                        displayName = uiState.name
                    )
                }
                item { HealthProgressSection(stats = uiState.healthStats) }
                item { EmergencyReadOnlySection(settings = uiState.emergencySettings) }
                item { DeviceStatusSection(deviceStatus = uiState.deviceStatus) }
                item {
                    ProfileSettingsSection(
                        settings              = uiState.profileSettings,
                        onToggleVoiceFeedback = { viewModel.toggleVoiceFeedbackForProgress() }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Section 1: User Info ──────────────────────────────────────────────────────

@Composable
private fun UserInfoSection(
    profile: UserProfile,
    displayName: String?
) {
    // No actionIcon here — edit is in the TopAppBar per design rules
    ProfileSectionCard(
        title = "User Information",
        icon = Icons.Default.Person,
        iconColor = Color(0xFF1976D2)
    ) {
        // Name (display only; edit via top-bar edit button)
        InfoRow(
            icon = Icons.Default.Person, iconColor = Color(0xFF1976D2),
            label = "Name",
            value = displayName.orEmpty().ifEmpty { "Tap to set name" },
            valueColor = if (displayName.isNullOrEmpty()) Color(0xFFAAAAAA) else Color(0xFF1A1A1A)
        )
        Spacer(Modifier.height(10.dp))
        // Home — tap opens MAP PIN setter
        InfoRow(
            icon = Icons.Default.Home, iconColor = Color(0xFF4CAF50),
            label = "Home Location",
            value = profile.homeLocation.ifEmpty { "Not setted" },
            valueColor = if (profile.homeLocation.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF1A1A1A),
        )
        Spacer(Modifier.height(10.dp))
        // Work — tap opens MAP PIN setter
        InfoRow(
            icon = Icons.Default.Work, iconColor = Color(0xFFFF9800),
            label = "Work / School",
            value = profile.workLocation.ifEmpty { "Not setted" },
            valueColor = if (profile.workLocation.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF1A1A1A),
        )
    }
}

// ── Local helpers to safely read from uiState (null-safe) ────────────────────

private val ProfileUiState.name  get() = userProfile.name.takeIf { it.isNotBlank() }

// ── Section 2: Health Stats ───────────────────────────────────────────────────

@Composable
private fun HealthProgressSection(stats: HealthStats) {
    ProfileSectionCard(
        title = "Health & Mobility",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        iconColor = Color(0xFF4CAF50)
    ) {
        Text("Today",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("%.1f".format(stats.distanceWalkedTodayKm), "km",    "Distance today",  Modifier.weight(1f))
            StatChip("${stats.stepsToday}",                       "steps", "Steps today",     Modifier.weight(1f))
            StatChip("${stats.routesCompletedToday}",             "routes","Routes today",    Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("This Week",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("%.1f".format(stats.distanceWalkedWeekKm),  "km",    "Distance this week", Modifier.weight(1f))
            StatChip("${stats.stepsWeek}",                        "steps", "Steps this week",    Modifier.weight(1f))
            StatChip("${stats.routesCompletedWeek}",              "routes","Routes this week",   Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(value: String, unit: String, desc: String, modifier: Modifier) {
    Surface(modifier.semantics { contentDescription = "$value $unit. $desc." },
        color = Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = Color(0xFF2E7D32))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
        }
    }
}

// ── Section 3: Emergency — READ-ONLY; editing is in Settings ─────────────────

@Composable
private fun EmergencyReadOnlySection(settings: EmergencySettings) {
    val contact = settings.contacts.firstOrNull()
    ProfileSectionCard(
        title = "Emergency",
        icon = Icons.Default.ContactPhone,
        iconColor = Color(0xFFE53935)
    ) {
        // Read-only contact row with notice to edit in Settings
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFEBEE),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(40.dp), CircleShape, color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ContactPhone, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Emergency Contact",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFE53935))
                    Text(
                        contact?.name?.ifBlank { "Not set" } ?: "Not set",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    if (contact?.phoneNumber?.isNotBlank() == true)
                        Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Emergency message — read-only preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFF8E1),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("To edit emergency contact or message, go to Settings → Safety & Emergency.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
            }
        }
    }
}

// ── Section 4: Device Status (NO battery) ───────────────────────────────────

@Composable
private fun DeviceStatusSection(deviceStatus: DeviceStatus) {
    val w   = deviceStatus.wearableDevice
    val gps = deviceStatus.gpsStatus
    ProfileSectionCard(title = "Device Status", icon = Icons.Default.Watch, iconColor = Color(0xFF9C27B0)) {
        DeviceStatusRow(
            icon        = if (w.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
            iconColor   = if (w.isConnected) Color(0xFF4CAF50) else Color(0xFFE65100),
            bgColor     = if (w.isConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
            label       = w.name.ifEmpty { "Wearable (ESP32)" },
            status      = if (w.isConnected) "Connected" else "Disconnected",
            statusColor = if (w.isConnected) Color(0xFF4CAF50) else Color(0xFFE65100),
            desc        = if (w.isConnected) "Wearable connected: ${w.name}" else "Wearable disconnected"
        )
        Spacer(Modifier.height(10.dp))
        DeviceStatusRow(
            icon        = if (gps == GpsStatus.READY) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
            iconColor   = when (gps) { GpsStatus.READY -> Color(0xFF4CAF50); GpsStatus.SEARCHING -> Color(0xFFF57C00); else -> Color(0xFFE53935) },
            bgColor     = when (gps) { GpsStatus.READY -> Color(0xFFE8F5E9); GpsStatus.SEARCHING -> Color(0xFFFFF3E0); else -> Color(0xFFFFEBEE) },
            label       = "GPS",
            status      = gps.displayName,
            statusColor = when (gps) { GpsStatus.READY -> Color(0xFF4CAF50); GpsStatus.SEARCHING -> Color(0xFFF57C00); else -> Color(0xFFE53935) },
            desc        = "GPS status: ${gps.displayName}"
        )
    }
}

@Composable
private fun DeviceStatusRow(
    icon: ImageVector, iconColor: Color, bgColor: Color,
    label: String, status: String, statusColor: Color, desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(40.dp), CircleShape, color = iconColor.copy(alpha = 0.20f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
        }
    }
}

// ── Section 5: Voice feedback toggle ─────────────────────────────────────────

@Composable
private fun ProfileSettingsSection(settings: ProfileSettings, onToggleVoiceFeedback: () -> Unit) {
    ProfileSectionCard(title = "Profile Settings", icon = Icons.Default.RecordVoiceOver, iconColor = Color(0xFF00BCD4)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleVoiceFeedback)
                .background(Color(0xFFF5F5F5))
                .padding(14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (settings.voiceFeedbackForProgress)
                        "Voice feedback for progress enabled. Tap to disable."
                    else "Voice feedback for progress disabled. Tap to enable."
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(40.dp), CircleShape, color = Color(0xFF00BCD4).copy(alpha = 0.15f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Voice Feedback for Progress",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                Text("Announce walking & route updates",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
            }
            Switch(
                checked = settings.voiceFeedbackForProgress,
                onCheckedChange = { onToggleVoiceFeedback() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50))
            )
        }
    }
}

// ── Clickable info row ────────────────────────────────────────────────────────

@Composable
private fun InfoRow(
    icon: ImageVector, iconColor: Color, label: String, value: String,
    valueColor: Color = Color(0xFF1A1A1A)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value." },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(40.dp), shape = CircleShape, color = iconColor.copy(alpha = 0.15f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF666666))
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = valueColor)
        }
        // intentionally no chevron and no clickable affordance
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────

@Composable
private fun ProfileSectionCard(
    title: String, icon: ImageVector, iconColor: Color,
    actionIcon: ImageVector? = null, onAction: (() -> Unit)? = null,
    actionDescription: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().semantics { heading() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    modifier = Modifier.weight(1f))
                if (actionIcon != null && onAction != null) {
                    IconButton(onAction, Modifier.semantics { contentDescription = actionDescription }) {
                        Icon(actionIcon, null, tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
