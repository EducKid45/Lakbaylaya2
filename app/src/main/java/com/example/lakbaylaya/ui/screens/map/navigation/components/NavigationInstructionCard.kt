package com.example.lakbaylaya.ui.screens.map.navigation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.example.lakbaylaya.ui.screens.map.utils.DirectionIconMapper

/**
 * Navigation instruction overlay card displayed at the top of the screen
 *
 * This is the primary navigation UI element that shows the current instruction.
 * It's designed to be highly visible and readable while walking.
 *
 * Features:
 * - Large, readable instruction text
 * - Maneuver icon for visual guidance
 * - Clickable to zoom to step location and repeat instruction via TTS
 * - Material 3 design with proper elevation and colors
 *
 * @param step Current navigation step to display
 * @param onClick Callback when card is clicked (zoom + TTS)
 * @param modifier Modifier for customization
 */
@Composable
fun NavigationInstructionCard(
    step: DirectionStep,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                contentDescription = "Current instruction: ${step.instruction}. Tap to zoom and repeat instruction."
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = DirectionIconMapper.getIconForManeuver(step.maneuver),
                        contentDescription = DirectionIconMapper.getContentDescription(step.maneuver),
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                    text = step.instruction,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 3
                )

                // Distance for this step
                Text(
                    text = "in ${step.getFormattedDistance()}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Compact version of navigation instruction card for smaller screens
 * or when more map visibility is needed
 */
@Composable
fun CompactNavigationInstructionCard(
    step: DirectionStep,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = "Current instruction: ${step.instruction}"
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp
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
                imageVector = DirectionIconMapper.getIconForManeuver(step.maneuver),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            // Compact instruction
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2
                )

                Text(
                    text = step.getFormattedDistance(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}


