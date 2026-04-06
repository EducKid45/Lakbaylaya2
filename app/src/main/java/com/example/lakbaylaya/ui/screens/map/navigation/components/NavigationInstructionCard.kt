package com.example.lakbaylaya.ui.screens.map.navigation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.example.lakbaylaya.utils.DirectionIconMapper

/**
 * Navigation instruction overlay card displayed at the top of the screen
 *
 * This is the primary navigation UI element that shows the current instruction.
 */
@Composable
fun NavigationInstructionCard(
    step: DirectionStep,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArrival: Boolean = false
) {
    // Prefer spokenInstruction if available for display and accessibility
    val displayText = step.spokenInstruction.takeIf { it.isNotBlank() } ?: step.instruction

    // Use special styling for arrival
    val containerColor = if (isArrival) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (isArrival) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (isArrival) 12.dp else 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                contentDescription = if (isArrival) {
                    "Navigation completed: $displayText. Tap to repeat announcement."
                } else {
                    "Current instruction: $displayText. Tap to zoom and repeat instruction."
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = if (isArrival) 12.dp else 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Maneuver icon (large and prominent)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isArrival) {
                    Color(0xFF4CAF50) // Green for arrival
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isArrival) {
                            Icons.Default.CheckCircle
                        } else {
                            DirectionIconMapper.getIconForManeuver(step.maneuver)
                        },
                        contentDescription = if (isArrival) {
                            "Arrived at destination"
                        } else {
                            DirectionIconMapper.getContentDescription(step.maneuver)
                        },
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Instruction text (dominant element)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isArrival) 24.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isArrival) 30.sp else 28.sp
                    ),
                    color = textColor,
                    maxLines = 3
                )

                // Distance for this step (hide for arrival)
                if (!isArrival && step.distanceMeters > 0) {
                    Text(
                        text = "in ${step.getFormattedDistance()}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.8f)
                    )
                } else if (isArrival) {
                    Text(
                        text = "Tap to hear announcement again",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactNavigationInstructionCard(
    step: DirectionStep,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArrival: Boolean = false
) {
    val displayText = step.spokenInstruction.takeIf { it.isNotBlank() } ?: step.instruction

    val containerColor = if (isArrival) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (isArrival) 8.dp else 6.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = if (isArrival) {
                    "Navigation completed: $displayText"
                } else {
                    "Current instruction: $displayText"
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (isArrival) 8.dp else 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Smaller icon
            Icon(
                imageVector = if (isArrival) {
                    Icons.Default.CheckCircle
                } else {
                    DirectionIconMapper.getIconForManeuver(step.maneuver)
                },
                contentDescription = null,
                tint = if (isArrival) {
                    Color(0xFF4CAF50) // Green for arrival
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(36.dp)
            )

            // Compact instruction
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2
                )

                if (!isArrival && step.distanceMeters > 0) {
                    Text(
                        text = step.getFormattedDistance(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
