package com.example.lakbaylaya.ui.screens.route

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel = viewModel(),
    onPreviewRoute: (SavedRoute) -> Unit = {},
    onStartNavigation: (SavedRoute) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Parent `LakbayLayaApp` provides the TopBar. Treat this screen as scaffold content.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        if (uiState.savedRoutes.isEmpty()) {
            // Empty state
            EmptyRoutesState(
                onSaveCurrentRoute = { viewModel.saveCurrentRoute() }
            )
        } else {
            // Routes list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header
                item {
                    RoutesHeader(routeCount = uiState.savedRoutes.size)
                }

                // Save current route button
                item {
                    SaveCurrentRouteButton(
                        onClick = { viewModel.saveCurrentRoute() }
                    )
                }

                // Route items
                items(
                    items = uiState.savedRoutes,
                    key = { it.id }
                ) { route ->
                    RouteItem(
                        route = route,
                        onClick = { viewModel.selectRoute(route) }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Route detail bottom sheet
        if (uiState.isDetailPanelVisible && uiState.selectedRoute != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeDetailPanel() },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                RouteDetailPanel(
                    route = uiState.selectedRoute!!,
                    onClose = { viewModel.closeDetailPanel() },
                    onPreview = { onPreviewRoute(uiState.selectedRoute!!) },
                    onStartNavigation = { onStartNavigation(uiState.selectedRoute!!) },
                    onRename = { viewModel.showRenameDialog() },
                    onDelete = { viewModel.showDeleteDialog() },
                    onPlayVoiceNote = { viewModel.playVoiceNote(uiState.selectedRoute!!.id) },
                    onPlayDifficultyWarning = { viewModel.playDifficultyWarning(uiState.selectedRoute!!.id) }
                )
            }
        }
    }

    // Rename dialog
    if (uiState.isRenameDialogVisible && uiState.selectedRoute != null) {
        RenameRouteDialog(
            currentName = uiState.selectedRoute!!.name,
            onDismiss = { viewModel.hideRenameDialog() },
            onConfirm = { newName -> viewModel.renameRoute(newName) }
        )
    }

    // Delete confirmation dialog
    if (uiState.isDeleteDialogVisible && uiState.selectedRoute != null) {
        DeleteRouteDialog(
            routeName = uiState.selectedRoute!!.name,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteRoute() }
        )
    }
}

@Composable
private fun RoutesHeader(routeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
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
private fun SaveCurrentRouteButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                contentDescription =
                    "Save current route. Tap to save your current navigation route."
            },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
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
private fun RouteItem(
    route: SavedRoute,
    onClick: () -> Unit
) {
    // Build accessibility description
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
            // Route name and icon
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

            // Start → Destination
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

            // Distance and time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
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

                // Time
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

            // Indicators (voice notes, difficult segments)
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
private fun RouteIndicatorChip(
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
private fun RouteDetailPanel(
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
                    .semantics { heading() }
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
                modifier = Modifier.semantics { heading() }
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
                modifier = Modifier.semantics { heading() }
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
            OutlinedButton(
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

            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Start navigation. Begin navigating this route."
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
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
            OutlinedButton(
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

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Delete route. Remove this route from saved routes."
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
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
private fun InfoCard(
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
private fun MemoryInfoRow(
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
        TextButton(
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

@Composable
private fun EmptyRoutesState(
    onSaveCurrentRoute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Route,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Saved Routes",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF424242),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                contentDescription = "No saved routes. You haven't saved any familiar routes yet."
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Save your frequently used routes for quick access and voice navigation.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSaveCurrentRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription =
                        "Save current route. Start navigating and save your first route."
                },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Save Current Route",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun RenameRouteDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Rename Route",
                modifier = Modifier.semantics {
                    contentDescription = "Rename route dialog"
                }
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Route Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Route name input field. Current value: $newName"
                    }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank(),
                modifier = Modifier.semantics {
                    contentDescription = "Confirm rename"
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel rename"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteRouteDialog(
    routeName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete Route?",
                modifier = Modifier.semantics {
                    contentDescription = "Delete route confirmation dialog"
                }
            )
        },
        text = {
            Text(
                "Are you sure you want to delete \"$routeName\"? This action cannot be undone.",
                modifier = Modifier.semantics {
                    contentDescription =
                        "Are you sure you want to delete $routeName? This action cannot be undone."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Confirm delete"
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel delete"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen()
}
