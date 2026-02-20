package com.example.lakbaylaya.ui.screens.route

import com.example.lakbaylaya.ui.screens.route.components.*
import com.example.lakbaylaya.ui.screens.route.dialogs.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Preview helper: lightweight ViewModel instance used only by @Preview
private val previewRoutesViewModel = RoutesViewModel(null, null, null)

/** Renders the Routes screen UI from the provided ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onPreviewRoute: (SavedRoute) -> Unit = {},
    onStartNavigation: (SavedRoute) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Routes", "Places", "Markers")

    // Parent `LakbayLayaApp` provides the TopBar. Treat this screen as scaffold content.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> RoutesTab(viewModel, uiState)
                1 -> PlacesTab(viewModel, uiState)
                2 -> MarkersTab(viewModel, uiState)
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

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(viewModel = previewRoutesViewModel)
}

@Composable
private fun RoutesTab(viewModel: RoutesViewModel, uiState: RoutesUiState) {
    if (uiState.savedRoutes.isEmpty()) {
        EmptyRoutesState(
            onSaveCurrentRoute = { viewModel.saveCurrentRoute() }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                RoutesHeader(routeCount = uiState.savedRoutes.size)
            }

            item {
                SaveCurrentRouteButton(
                    onClick = { viewModel.saveCurrentRoute() }
                )
            }

            items(
                items = uiState.savedRoutes,
                key = { it.id }
            ) { route ->
                RouteItem(
                    route = route,
                    onClick = { viewModel.selectRoute(route) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PlacesTab(viewModel: RoutesViewModel, uiState: RoutesUiState) {
    if (uiState.savedPlaces.isEmpty()) {
        EmptyPlacesState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Saved Places",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${uiState.savedPlaces.size} places saved",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            items(
                items = uiState.savedPlaces,
                key = { it.id }
            ) { place ->
                PlaceItem(
                    place = place,
                    onDelete = { viewModel.deletePlace(place.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MarkersTab(viewModel: RoutesViewModel, uiState: RoutesUiState) {
    if (uiState.customMarkers.isEmpty()) {
        EmptyMarkersState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Custom Markers",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${uiState.customMarkers.size} markers saved",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            items(
                items = uiState.customMarkers,
                key = { it.id }
            ) { marker ->
                MarkerItem(
                    marker = marker,
                    onDelete = { viewModel.deleteMarker(marker.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyPlacesState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFBDBDBD)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Saved Places",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save places from the map using the Place Bottom Sheet",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyMarkersState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFBDBDBD)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Custom Markers",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add markers from your current location using the Navigation Bottom Sheet",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

