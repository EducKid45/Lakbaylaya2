package com.example.lakbaylaya.ui.navigationbars.top

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.R
import com.example.lakbaylaya.ui.theme.BluetoothOff
import com.example.lakbaylaya.ui.theme.BluetoothOn
import com.example.lakbaylaya.ui.theme.DarkBluetoothOff
import com.example.lakbaylaya.ui.theme.DarkBluetoothOn
import com.example.lakbaylaya.ui.theme.DarkEmergency
import com.example.lakbaylaya.ui.theme.Emergency

/**
 * Top App Bar with global actions: Settings, Emergency, Bluetooth, Notifications
 *
 * Design:
 * - Fixed at top
 * - Left: Settings
 * - Center: App name "Lakbaylaya"
 * - Right: Emergency, Bluetooth, Notifications
 *
 * Accessibility:
 * - TalkBack announces each action with state
 * - Touch targets  48dp
 * - High contrast colors
 * - Respects system "reduce motion" preference
 *
 * Animations:
 * - Emergency pulse (slow)
 * - Bluetooth color transition
 * - Click feedback (150-250ms)
 * - Notification badge scale
 *
 * @param isBluetoothEnabled Current Bluetooth state
 * @param isDarkTheme Whether dark theme is active (accepted but ignored — app is light-only)
 * @param onSettingsClick Callback for settings action (when showing settings icon)
 * @param onBackClick Callback for back action (when showing back icon)
 * @param onEmergencyClick Callback for emergency action
 * @param onBluetoothClick Callback for bluetooth toggle
 * @param onHelpClick Callback for the Help (?) action — opens Voice Guide screen
 * @param showBackIcon When true, shows a back arrow instead of settings icon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isBluetoothEnabled: Boolean,
    bluetoothState: com.example.lakbaylaya.bluetooth.BluetoothState = com.example.lakbaylaya.bluetooth.BluetoothState.DISCONNECTED,
    isDarkTheme: Boolean,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onHelpClick: () -> Unit,
    showBackIcon: Boolean = false,
    showActions: Boolean = true,
    title: String? = null,
    // Kept for binary-compatibility with call-sites that still pass these;
    // they are intentionally ignored.
    @Suppress("UNUSED_PARAMETER") notificationCount: Int = 0,
    @Suppress("UNUSED_PARAMETER") onNotificationsClick: () -> Unit = {}
) {
    // Precompute string resources in composable scope to avoid using Context inside semantics
    val settingsDesc = stringResource(R.string.content_desc_settings)
    val emergencyDesc = stringResource(R.string.content_desc_emergency)
    val helpDesc = "Open voice guide and help"
    val bluetoothOnDesc = stringResource(R.string.content_desc_bluetooth_on)
    val bluetoothOffDesc = stringResource(R.string.content_desc_bluetooth_off)
    val backDesc = stringResource(R.string.action_back)

    // Emergency pulse animation
    val emergencyTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val emergencyAlpha by emergencyTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_alpha"
    )

    // Bluetooth color animation (based on state)
    val bluetoothColor by animateColorAsState(
        targetValue = when (bluetoothState) {
            com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTED -> if (isDarkTheme) DarkBluetoothOn else BluetoothOn
            com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTING -> if (isDarkTheme) DarkBluetoothOn else BluetoothOn
            com.example.lakbaylaya.bluetooth.BluetoothState.SCANNING -> if (isDarkTheme) DarkBluetoothOn else BluetoothOn
            else -> if (isDarkTheme) DarkBluetoothOff else BluetoothOff
        },
        animationSpec = tween(300),
        label = "bluetooth_color"
    )

    // Pulsing animation for scanning/connecting
    val btTransition = rememberInfiniteTransition(label = "bt_pulse")
    val btPulseAlpha by btTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_pulse_alpha"
    )

    val emergencyColor = if (isDarkTheme) DarkEmergency else Emergency

    // Wrap the TopAppBar and divider with a container that applies status bar insets
    SystemTopBarContainer {
        Column {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title
                                ?: stringResource(R.string.app_name), // Use custom title if provided
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    if (showBackIcon) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = backDesc
                                }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backDesc,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = settingsDesc
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (showActions) { // Only show actions if true
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emergency button with pulse
                            IconButton(
                                onClick = onEmergencyClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .alpha(emergencyAlpha)
                                    .semantics {
                                        contentDescription = emergencyDesc
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.action_emergency),
                                    tint = emergencyColor
                                )
                            }

                            // Bluetooth with state-based visual indicator
                            IconButton(
                                onClick = onBluetoothClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = when (bluetoothState) {
                                            com.example.lakbaylaya.bluetooth.BluetoothState.SCANNING -> bluetoothOnDesc
                                            com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTING -> bluetoothOnDesc
                                            com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTED -> bluetoothOnDesc
                                            else -> bluetoothOffDesc
                                        }
                                    }
                            ) {
                                // Icon-only: show Bluetooth icon and pulsing dot when active
                                Box(contentAlignment = Alignment.Center) {
                                    val iconVector = when (bluetoothState) {
                                        com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTED -> Icons.Default.Bluetooth
                                        com.example.lakbaylaya.bluetooth.BluetoothState.SCANNING -> Icons.Default.Bluetooth
                                        com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTING -> Icons.Default.Bluetooth
                                        else -> Icons.Default.BluetoothDisabled
                                    }

                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = stringResource(R.string.action_bluetooth),
                                        tint = bluetoothColor
                                    )

                                    // Small pulsing dot when scanning or connecting
                                    if (bluetoothState == com.example.lakbaylaya.bluetooth.BluetoothState.SCANNING ||
                                        bluetoothState == com.example.lakbaylaya.bluetooth.BluetoothState.CONNECTING
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(8.dp)
                                                .padding(end = 6.dp, top = 6.dp)
                                                .alpha(btPulseAlpha)
                                                .background(
                                                    color = bluetoothColor,
                                                    shape = CircleShape
                                                )
                                        ) {}
                                    }
                                }
                            }

                            // Help icon — opens Voice Guide screen
                            IconButton(
                                onClick = onHelpClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = helpDesc
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = helpDesc,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Bottom divider to separate TopBar from content
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )
        }
    }
}

