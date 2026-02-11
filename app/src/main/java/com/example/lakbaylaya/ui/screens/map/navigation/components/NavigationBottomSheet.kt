package com.example.lakbaylaya.ui.screens.map.navigation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.screens.map.models.NavigationState
import java.util.Locale

/**
 * Compact navigation bottom sheet showing navigation progress and controls
 *
 * This sheet is designed to be small and unobtrusive, showing key
 * navigation metrics without blocking the map view.
 *
 * Features:
 * - Estimated time for current step
 * - Step progress indicator (e.g., "Step 2 of 5")
 * - Distance covered in current step
 * - Close button to stop navigation
 * - Recenter camera button to view full route
 *
 * @param navigationState Current active navigation state
 * @param onClose Callback to stop navigation
 * @param onRecenter Callback to recenter camera on full route
 * @param onDone Callback to finish navigation and return to normal mode
 * @param modifier Modifier for customization
 */
@Composable
fun NavigationBottomSheet(
    navigationState: NavigationState.Active,
    onClose: () -> Unit,
    onRecenter: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = navigationState.getCurrentStep()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            )
            .semantics {
                contentDescription = "Navigation controls: Step ${navigationState.currentStepIndex + 1} of ${navigationState.getTotalSteps()}"
            },
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: Close button and recenter button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button (circular)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Stop navigation"
                        },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Recenter button
                OutlinedButton(
                    onClick = onRecenter,
                    modifier = Modifier.semantics {
                        contentDescription = "Show full route on map"
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Route")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Show Done button only when navigation is completed
                if (navigationState.isCompleted) {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.semantics {
                            contentDescription = "Finish navigation"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Done")
                    }
                }
            }

            HorizontalDivider()

            // Center content: Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estimated time for current step
                NavigationStatItem(
                    label = "Est. Time",
                    value = currentStep?.let { formatDuration(it.durationMinutes) } ?: "--",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                )

                // Pedometer step count (cumulative since navigation started)
                NavigationStatItem(
                    label = "Steps Taken",
                    value = "${navigationState.stepCount} steps",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                )

                // Total distance covered since navigation started
                NavigationStatItem(
                    label = "Distance",
                    value = formatDistance(navigationState.totalDistanceCovered),
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress / completion area
            // If navigation is completed, show a done message and a full progress bar
            if (navigationState.isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Navigation complete",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else if (currentStep != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Progress text moved above the progress bar
                    Text(
                        text = "${formatDistance(navigationState.getRemainingDistance())} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LinearProgressIndicator(
                        progress = { navigationState.getStepProgress() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual stat item in navigation bottom sheet
 */
@Composable
private fun NavigationStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Format duration in minutes to readable string
 */
private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
        }
    }
}

/**
 * Format distance in meters to readable string
 */
private fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "${meters.toInt()} m"
        else -> String.format(Locale.getDefault(), "%.1f km", meters / 1000)
    }
}
