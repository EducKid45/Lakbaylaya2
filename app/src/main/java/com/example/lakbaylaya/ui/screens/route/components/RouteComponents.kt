package com.example.lakbaylaya.ui.screens.route.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.screens.route.SavedRoute

@Composable
fun RoutesHeader(routeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { /* heading semantics are applied by caller if needed */ }
    ) {
        Text(
            text = "Saved Routes",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics {
                contentDescription = "Saved Routes, $routeCount routes available"
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$routeCount familiar routes",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            modifier = Modifier.semantics {
                contentDescription = "$routeCount familiar routes saved"
            }
        )
    }
}

@Composable
fun SaveCurrentRouteButton(onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                contentDescription =
                    "Save current route. Tap to save your current navigation route."
            },
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF1976D2)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Save Current Route",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun RouteItem(
    route: SavedRoute,
    onClick: () -> Unit
) {
    val accessibilityDescription = buildString {
        append("Route: ${route.name}. ")
        append("From ${route.startLocation} to ${route.endLocation}. ")
        append("${route.distanceKm} kilometers. ")
        append("${route.estimatedMinutes} minutes walking. ")
        if (route.hasVoiceNotes) {
            append("Voice notes available. ")
        }
        if (route.hasDifficultSegments) {
            append("Difficult section ahead. ")
        }
        append("Tap to view details.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = accessibilityDescription
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.startLocation,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color(0xFF424242)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "to",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                )
                Text(
                    text = route.endLocation,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color(0xFF424242)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${route.distanceKm} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${route.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            if (route.hasVoiceNotes || route.hasDifficultSegments) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (route.hasVoiceNotes) {
                        RouteIndicatorChip(
                            icon = Icons.Default.Mic,
                            text = "Voice notes",
                            backgroundColor = Color(0xFFE3F2FD),
                            contentColor = Color(0xFF1976D2)
                        )
                    }
                    if (route.hasDifficultSegments) {
                        RouteIndicatorChip(
                            icon = Icons.Default.Warning,
                            text = "Difficult section",
                            backgroundColor = Color(0xFFFFF3E0),
                            contentColor = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteIndicatorChip(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun RouteDetailPanel(
    route: SavedRoute,
    onClose: () -> Unit,
    onPreview: () -> Unit,
    onStartNavigation: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPlayVoiceNote: () -> Unit,
    onPlayDifficultyWarning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics {
                contentDescription = "Route details panel for ${route.name}"
            }
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = route.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .weight(1f)
                    .semantics { /* heading handled by caller if needed */ }
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.semantics {
                    contentDescription = "Close route details"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route info
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = route.startLocation,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = Color(0xFF424242)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "to",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp)
            )
            Text(
                text = route.endLocation,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = Color(0xFF424242)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Distance and time cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "Distance",
                value = "${route.distanceKm} km",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Default.AccessTime,
                label = "Est. Time",
                value = "${route.estimatedMinutes} min",
                modifier = Modifier.weight(1f)
            )
        }

        // Landmarks section
        if (route.landmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Known Landmarks",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.semantics { /* heading handled by caller */ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            route.landmarks.forEach { landmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = landmark,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242),
                        modifier = Modifier.semantics {
                            contentDescription = "Landmark: $landmark"
                        }
                    )
                }
            }
        }

        // Voice notes and difficulty sections
        if (route.hasVoiceNotes || route.hasDifficultSegments) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Route Memory",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.semantics { /* heading handled by caller */ }
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (route.hasVoiceNotes) {
                MemoryInfoRow(
                    icon = Icons.Default.Mic,
                    iconColor = Color(0xFF1976D2),
                    text = "${route.voiceNoteCount} GPS-tagged voice notes",
                    actionText = "Play sample",
                    onAction = onPlayVoiceNote
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (route.hasDifficultSegments) {
                MemoryInfoRow(
                    icon = Icons.Default.Vibration,
                    iconColor = Color(0xFFE65100),
                    text = "${route.difficultSegmentCount} difficult areas marked",
                    actionText = "Hear warning",
                    onAction = onPlayDifficultyWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onPreview,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription =
                            "Preview on map. Shows route line only without navigation."
                    },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview", fontWeight = FontWeight.SemiBold)
            }

            androidx.compose.material3.Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Start navigation. Begin navigating this route."
                    },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navigate", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Management buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onRename,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Rename route. Change the name of this route."
                    },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rename", fontSize = 14.sp)
            }

            androidx.compose.material3.OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Delete route. Remove this route from saved routes."
                    },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE53935)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            },
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

@Composable
fun MemoryInfoRow(
    icon: ImageVector,
    iconColor: Color,
    text: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$text. Tap $actionText button to hear audio."
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
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.TextButton(
            onClick = onAction,
            modifier = Modifier.semantics {
                contentDescription = actionText
            }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(actionText, fontSize = 14.sp)
        }
    }
}
