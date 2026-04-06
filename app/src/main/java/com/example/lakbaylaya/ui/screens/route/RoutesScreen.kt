package com.example.lakbaylaya.ui.screens.route

import com.example.lakbaylaya.ui.screens.route.dialogs.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val previewRoutesViewModel = RoutesViewModel(null, null, null)

/**
 * Routes screen — unified "Saved Locations" list.
 *
 * Both Route and Place cards share the SAME visual design:
 *  - Tap header → expand / collapse
 *  - Expanded: Navigate, Preview, Rename, Delete
 *
 * TTS reading of the full list is handled via GlobalVoiceViewModel (voice command
 * "show routes") — no mic FAB here, no TTS in this file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onPreviewRoute: (SavedRoute) -> Unit = {},
    onStartNavigation: (SavedRoute) -> Unit = {},
    onNavigateToMap: (SavedPlace) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    openRouteId: String? = null
) {
    val uiState          by viewModel.uiState.collectAsState()
    val placeInteraction by viewModel.placeInteraction.collectAsState()

    var expandedItemId  by remember { mutableStateOf<String?>(null) }
    var renamingPlaceId by remember { mutableStateOf<String?>(null) }
    var renamingRouteId by remember { mutableStateOf<String?>(null) }

    // Auto-open route from Save flow
    LaunchedEffect(openRouteId, uiState.savedRoutes) {
        openRouteId?.let { id ->
            if (uiState.savedRoutes.any { it.id == id }) expandedItemId = id
        }
    }

    // Duplicate overwrite dialog
    val dupCandidate = placeInteraction.duplicateCandidate
    if (dupCandidate != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelOverwritePlace() },
            icon  = { Icon(Icons.Default.Place, null, tint = Color(0xFFE53935)) },
            title = { Text("Place Already Exists", fontWeight = FontWeight.Bold) },
            text  = { Text("\"${dupCandidate.placeName}\" already exists. Overwrite it?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmOverwritePlace() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelOverwritePlace() }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        if (uiState.savedLocations.isEmpty() && !uiState.isLoading) {
            EmptySavedLocationsState()
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                item { SavedLocationsHeader(count = uiState.savedLocations.size) }

                items(items = uiState.savedLocations, key = { it.id }) { locItem ->
                    val isExpanded = expandedItemId == locItem.id

                    when (locItem.type) {
                        LocationItemType.ROUTE -> {
                            val route = uiState.savedRoutes.find { it.id == locItem.id }
                            if (route != null) {
                                SavedItemCard(
                                    id          = route.id,
                                    title       = route.name,
                                    subTitle    = buildString {
                                        append(route.startLocation)
                                        if (route.endLocation.isNotBlank()) append(" → ${route.endLocation}")
                                    },
                                    typeLabel   = "Route",
                                    icon        = Icons.Default.Explore,
                                    accentColor = Color(0xFF1565C0),
                                    isExpanded  = isExpanded,
                                    isRenaming  = renamingRouteId == route.id,
                                    currentName = route.name,
                                    onToggle    = {
                                        expandedItemId = if (isExpanded) null else route.id
                                        renamingRouteId = null
                                    },
                                    onNavigate  = { onStartNavigation(route) },
                                    onPreview   = { onPreviewRoute(route) },
                                    onStartEdit = {
                                        renamingRouteId = route.id
                                        expandedItemId  = route.id
                                    },
                                    onConfirmRename = { newName ->
                                        viewModel.selectRoute(route)
                                        viewModel.renameRoute(newName)
                                        renamingRouteId = null
                                    },
                                    onCancelRename = { renamingRouteId = null },
                                    onDelete = {
                                        viewModel.selectRoute(route)
                                        viewModel.showDeleteDialog()
                                        if (expandedItemId == route.id) expandedItemId = null
                                    }
                                )
                            }
                        }

                        LocationItemType.PLACE -> {
                            val place = uiState.savedPlaces.find { it.id == locItem.id }
                            if (place != null) {
                                SavedItemCard(
                                    id          = place.id,
                                    title       = place.placeName,
                                    subTitle    = place.address.ifBlank {
                                        "Lat %.5f  Lon %.5f".format(place.latitude, place.longitude)
                                    },
                                    typeLabel   = place.label.ifBlank { "Place" },
                                    icon        = Icons.Default.Place,
                                    accentColor = Color(0xFF006C4C),
                                    isExpanded  = isExpanded,
                                    isRenaming  = renamingPlaceId == place.id,
                                    currentName = place.placeName,
                                    onToggle    = {
                                        expandedItemId = if (isExpanded) null else place.id
                                        renamingPlaceId = null
                                    },
                                    onNavigate  = { onNavigateToMap(place) },
                                    onPreview   = { onNavigateToMap(place) },
                                    onStartEdit = {
                                        renamingPlaceId = place.id
                                        expandedItemId  = place.id
                                    },
                                    onConfirmRename = { newName ->
                                        viewModel.confirmRenamePlace(place.id, newName, place.label)
                                        renamingPlaceId = null
                                    },
                                    onCancelRename = { renamingPlaceId = null },
                                    onDelete = {
                                        viewModel.deletePlace(place.id)
                                        if (expandedItemId == place.id) expandedItemId = null
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Route delete confirmation dialog
    if (uiState.isDeleteDialogVisible && uiState.selectedRoute != null) {
        DeleteRouteDialog(
            routeName = uiState.selectedRoute!!.name,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteRoute() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavedLocationsHeader(count: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { heading() }.padding(bottom = 4.dp)
    ) {
        Text(
            text  = "Saved Locations",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold, fontSize = 24.sp
            ),
            color    = Color(0xFF1A1A1A),
            modifier = Modifier.semantics {
                contentDescription = "Saved Locations, $count ${if (count == 1) "item" else "items"}"
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = "$count saved ${if (count == 1) "location" else "locations"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unified card — same design for ROUTE and PLACE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavedItemCard(
    id: String,
    title: String,
    subTitle: String,
    typeLabel: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    isRenaming: Boolean,
    currentName: String,
    onToggle: () -> Unit,
    onNavigate: () -> Unit,
    onPreview: () -> Unit,
    onStartEdit: () -> Unit,
    onConfirmRename: (String) -> Unit,
    onCancelRename: () -> Unit,
    onDelete: () -> Unit
) {
    var editName by remember(isRenaming, id) { mutableStateOf(currentName) }
    val nameBlank = editName.isBlank()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$typeLabel: $title. " +
                    if (isExpanded) "Expanded. Tap to collapse." else "Tap to expand actions."
            }
            .clickable(onClick = onToggle),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header row ─────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = accentColor.copy(alpha = 0.10f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector      = icon,
                            contentDescription = null,
                            tint             = accentColor,
                            modifier         = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = accentColor.copy(alpha = 0.13f)) {
                        Text(
                            text     = typeLabel,
                            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color    = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    if (subTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text     = subTitle,
                            style    = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color    = Color(0xFF666666),
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector        = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint               = Color(0xFF9E9E9E),
                    modifier           = Modifier.size(24.dp)
                )
            }

            // ── Expanded section ───────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isRenaming) {
                        Text(
                            "Rename",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value          = editName,
                            onValueChange  = { editName = it },
                            label          = { Text("Name *") },
                            singleLine     = true,
                            isError        = nameBlank,
                            supportingText = {
                                if (nameBlank) Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick  = { onConfirmRename(editName) },
                                enabled  = !nameBlank,
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) { Text("Save", color = Color.White) }
                            OutlinedButton(
                                onClick  = onCancelRename,
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    } else {
                        // Row 1: Navigate + Preview
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick  = onNavigate,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.Navigation, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Navigate", color = Color.White, fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick  = onPreview,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Preview", fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 2: Rename + Delete
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick  = onStartEdit,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rename", fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick  = onDelete,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                border   = BorderStroke(1.dp, Color(0xFFE53935))
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color(0xFFE53935))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", fontSize = 14.sp, color = Color(0xFFE53935))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptySavedLocationsState() {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "No saved locations yet." },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector      = Icons.Default.Bookmark,
                contentDescription = null,
                tint             = Color(0xFFBDBDBD),
                modifier         = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text      = "No saved locations yet",
                style     = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color     = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "Save routes and places from the map\nto see them here.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color(0xFFBDBDBD),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(viewModel = previewRoutesViewModel)
}
