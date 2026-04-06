package com.example.lakbaylaya.bluetooth.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.bluetooth.BluetoothState
import com.example.lakbaylaya.bluetooth.DiscoveredDevice

/**
 * Bluetooth Device Bottom Sheet
 *
 * Overlays the current screen when the Bluetooth icon in the TopBar is tapped.
 * Displays a scrollable list of all nearby discovered Bluetooth devices.
 *
 * Features:
 * - Real-time reactive list of scanned devices
 * - Each item shows: icon, name, address, connection status chip
 * - Tapping a disconnected row → connect
 * - Tapping a connected row → disconnect
 * - Scanning indicator while discovery is active
 * - "Scan again" button to restart discovery
 * - Proper accessibility descriptions
 * - Clean Material 3 design
 *
 * @param bluetoothState Current global Bluetooth state
 * @param devices List of discovered devices (reactive from ViewModel)
 * @param onDismiss Called when the sheet is dismissed
 * @param onToggleDevice Called with device address on row tap (connect or disconnect)
 * @param onRescan Called when the user taps "Scan again"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceBottomSheet(
    bluetoothState: BluetoothState,
    devices: List<DiscoveredDevice>,
    onDismiss: () -> Unit,
    onToggleDevice: (address: String) -> Unit,
    onRescan: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Compute a max sheet height (e.g., 60% of screen height) so it is not fullscreen
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxSheetHeight = (screenHeightDp * 0.60).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        dragHandle = {
            // Custom drag handle (pill)
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Respect system navigation bars so content won't render behind gesture/navigation bar
                .navigationBarsPadding()
                // Set fixed height so sheet does not collapse to a small size
                .height(maxSheetHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            SheetHeader(
                bluetoothState = bluetoothState,
                onRescan = onRescan
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Scanning indicator ──────────────────────────────────────────
            if (bluetoothState == BluetoothState.SCANNING ||
                bluetoothState == BluetoothState.ENABLING
            ) {
                ScanningIndicator()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Device list ─────────────────────────────────────────────────
            if (devices.isEmpty() && bluetoothState !in listOf(
                    BluetoothState.SCANNING,
                    BluetoothState.ENABLING,
                    BluetoothState.CONNECTING
                )
            ) {
                EmptyDeviceList(onRescan = onRescan)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = devices,
                        key = { it.address }
                    ) { device ->
                        BluetoothDeviceRow(
                            device = device,
                            onTap = { onToggleDevice(device.address) }
                        )
                        if (devices.last().address != device.address) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun SheetHeader(
    bluetoothState: BluetoothState,
    onRescan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Bluetooth Devices",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (bluetoothState) {
                    BluetoothState.SCANNING, BluetoothState.ENABLING -> "Scanning for devices…"
                    BluetoothState.CONNECTING -> "Connecting…"
                    BluetoothState.CONNECTED -> "Device connected"
                    BluetoothState.DISCONNECTED -> "Tap a device to connect"
                    BluetoothState.ERROR -> "Error occurred"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Rescan button
        IconButton(
            onClick = onRescan,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Scan for Bluetooth devices again" }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Scanning indicator ────────────────────────────────────────────────────

@Composable
private fun ScanningIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Searching for nearby devices…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.semantics { contentDescription = "Scanning for nearby Bluetooth devices" }
        )
    }
}

// ── Empty state ────────────────────────────────────────────────────────────

@Composable
private fun EmptyDeviceList(onRescan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "No devices found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Make sure the device is powered on and in range",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Button(
            onClick = onRescan,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Again")
        }
    }
}

// ── Device Row ─────────────────────────────────────────────────────────────

@Composable
fun BluetoothDeviceRow(
    device: DiscoveredDevice,
    onTap: () -> Unit
) {
    // Pulse animation for connecting state
    val pulseTransition = rememberInfiniteTransition(label = "bt_row_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "row_pulse_alpha"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            device.isConnected -> Color(0xFF1976D2)   // blue — connected
            device.isConnecting -> Color(0xFFF57C00)  // amber — connecting
            else -> Color(0xFF9E9E9E)                  // grey — idle
        },
        animationSpec = tween(300),
        label = "icon_tint"
    )

    val statusText = when {
        device.isConnected -> "Connected"
        device.isConnecting -> "Connecting…"
        else -> "Tap to connect"
    }

    val statusColor = when {
        device.isConnected -> Color(0xFF4CAF50)
        device.isConnecting -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .clickable(
                onClick = onTap,
                enabled = !device.isConnecting
            )
            .semantics {
                contentDescription = "${device.name}: $statusText. Tap to ${if (device.isConnected) "disconnect" else "connect"}"
            },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth icon in tinted circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = iconTint.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .then(
                        if (device.isConnecting) Modifier.alpha(pulseAlpha) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        device.isConnected -> Icons.Default.BluetoothConnected
                        device.isConnecting -> Icons.AutoMirrored.Filled.BluetoothSearching
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Device name + address
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            // Status chip
            StatusChip(
                text = statusText,
                color = statusColor,
                isConnecting = device.isConnecting,
                pulseAlpha = pulseAlpha
            )
        }
    }
}

// ── Status chip ───────────────────────────────────────────────────────────

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    isConnecting: Boolean,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .then(
                if (isConnecting) Modifier.alpha(pulseAlpha) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Small status dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = color
        )
    }
}
