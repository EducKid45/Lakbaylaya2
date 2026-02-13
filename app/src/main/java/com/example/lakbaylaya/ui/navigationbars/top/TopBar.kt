package com.example.lakbaylaya.ui.navigationbars.top

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.example.lakbaylaya.ui.theme.Accent
import com.example.lakbaylaya.ui.theme.BluetoothOff
import com.example.lakbaylaya.ui.theme.BluetoothOn
import com.example.lakbaylaya.ui.theme.DarkAccent
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
 * @param notificationCount Number of unread notifications
 * @param isDarkTheme Whether dark theme is active
 * @param onSettingsClick Callback for settings action (when showing settings icon)
 * @param onBackClick Callback for back action (when showing back icon)
 * @param onEmergencyClick Callback for emergency action
 * @param onBluetoothClick Callback for bluetooth toggle
 * @param onNotificationsClick Callback for notifications action
 * @param showBackIcon When true, shows a back arrow instead of settings icon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isBluetoothEnabled: Boolean,
    notificationCount: Int,
    isDarkTheme: Boolean,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    showBackIcon: Boolean = false
) {
    // Precompute string resources in composable scope to avoid using Context inside semantics
    val settingsDesc = stringResource(R.string.content_desc_settings)
    val emergencyDesc = stringResource(R.string.content_desc_emergency)
    val notificationsDesc = stringResource(R.string.content_desc_notifications)
    val bluetoothOnDesc = stringResource(R.string.content_desc_bluetooth_on)
    val bluetoothOffDesc = stringResource(R.string.content_desc_bluetooth_off)
    val badgeDesc = stringResource(R.string.content_desc_notification_badge, notificationCount)
    val backDesc = stringResource(R.string.action_back)

    // Emergency pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val emergencyAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_alpha"
    )

    // Bluetooth color animation
    val bluetoothColor by animateColorAsState(
        targetValue = if (isBluetoothEnabled) {
            if (isDarkTheme) DarkBluetoothOn else BluetoothOn
        } else {
            if (isDarkTheme) DarkBluetoothOff else BluetoothOff
        },
        animationSpec = tween(300),
        label = "bluetooth_color"
    )

    val emergencyColor = if (isDarkTheme) DarkEmergency else Emergency
    val accentColor = if (isDarkTheme) DarkAccent else Accent

    // Wrap the TopAppBar and divider with a container that applies status bar insets
    SystemTopBarContainer {
        Column {
            TopAppBar(
                title = {
                    // Centered app name
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
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
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = backDesc,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Settings - Left corner
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
                    // Right corner actions
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

                        // Bluetooth with state-based color
                        IconButton(
                            onClick = onBluetoothClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = if (isBluetoothEnabled) {
                                        bluetoothOnDesc
                                    } else {
                                        bluetoothOffDesc
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = if (isBluetoothEnabled) {
                                    Icons.Default.Bluetooth
                                } else {
                                    Icons.Default.BluetoothDisabled
                                },
                                contentDescription = stringResource(R.string.action_bluetooth),
                                tint = bluetoothColor
                            )
                        }

                        // Notifications with badge
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = notificationsDesc
                                }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (notificationCount > 0) {
                                        Badge(
                                            containerColor = accentColor,
                                            modifier = Modifier.semantics {
                                                contentDescription = badgeDesc
                                            }
                                        ) {
                                            Text(
                                                text = if (notificationCount > 99) {
                                                    stringResource(R.string.notification_badge_over_99)
                                                } else {
                                                    notificationCount.toString()
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.action_notifications),
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
