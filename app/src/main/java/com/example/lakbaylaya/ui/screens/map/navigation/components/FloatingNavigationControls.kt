package com.example.lakbaylaya.ui.screens.map.navigation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Floating navigation controls positioned above the bottom sheet
 *
 * Provides quick access to important navigation settings without
 * requiring the user to open menus.
 *
 * Features:
 * - Mute/Unmute toggle for voice guidance
 * - Center-on-location toggle (replaces 2D/3D toggle and custom compass button)
 *
 * Note: The built-in MapLibre compass UI is used for device orientation
 * and there is no separate compass button in this control anymore.
 */
@Composable
fun FloatingNavigationControls(
    isMuted: Boolean,
    isCentered: Boolean,
    onMuteToggle: () -> Unit,
    onCenterToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Mute/Unmute toggle
        FloatingActionButton(
            onClick = onMuteToggle,
            containerColor = if (isMuted) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isMuted) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape)
                .semantics {
                    contentDescription = if (isMuted) {
                        "Voice guidance muted. Tap to unmute."
                    } else {
                        "Voice guidance active. Tap to mute."
                    }
                }
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }

        // Center-on-location toggle (replaces custom compass & 2D/3D)
        FloatingActionButton(
            onClick = onCenterToggle,
            containerColor = if (isCentered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isCentered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape)
                .semantics {
                    contentDescription = if (isCentered) "Following your location" else "Center on my location"
                }
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Compact version with smaller buttons for constrained layouts
 */
@Composable
fun CompactFloatingNavigationControls(
    isMuted: Boolean,
    isCentered: Boolean,
    onMuteToggle: () -> Unit,
    onCenterToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Smaller FABs
        SmallFloatingActionButton(
            onClick = onMuteToggle,
            containerColor = if (isMuted) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isMuted) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .shadow(3.dp, CircleShape)
                .semantics {
                    contentDescription = if (isMuted) "Unmute" else "Mute"
                }
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onCenterToggle,
            containerColor = if (isCentered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isCentered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .shadow(3.dp, CircleShape)
                .semantics {
                    contentDescription = if (isCentered) "Following your location" else "Center on my location"
                }
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
