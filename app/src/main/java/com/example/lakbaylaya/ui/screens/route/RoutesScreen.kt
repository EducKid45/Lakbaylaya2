package com.example.lakbaylaya.ui.screens.route

import com.example.lakbaylaya.ui.screens.route.components.*
import com.example.lakbaylaya.ui.screens.route.dialogs.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Preview helper: lightweight ViewModel instance used only by @Preview
private val previewRoutesViewModel = RoutesViewModel(null)

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

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(viewModel = previewRoutesViewModel)
}
