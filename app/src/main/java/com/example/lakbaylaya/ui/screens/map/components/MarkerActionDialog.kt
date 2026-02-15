package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata
import com.example.lakbaylaya.ui.screens.map.components.markerEdit.MarkerLocationEditorDialog
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.lakbaylaya.data.repository.MapRepositoryImpl
import com.example.lakbaylaya.data.api.GeoapifyApiImpl

/**
 * Modal dialog for managing voice notes and location metadata.
 * Full-screen centered popup with semi-transparent scrim overlay.
 * @param isVisible Controls dialog visibility
 * @param onDismiss Callback when user dismisses the dialog
 * @param onSaveNotes Callback when user saves marker notes
 * @param modifier Modifier for customization
 * @param mapManager MapLibreManager for controlling map during location editing
 */
@Composable
fun MarkerActionDialog(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    mapManager: MapLibreManager? = null,
    onDismiss: () -> Unit,
    onSaveNotes: (MarkerMetadata) -> Unit
) {
    if (!isVisible) return

    val scope = rememberCoroutineScope()

    // Initialize repository for reverse geocoding
    val repository = remember<MapRepositoryImpl> {
        MapRepositoryImpl(GeoapifyApiImpl())
    }

    var metadata by remember { mutableStateOf(MarkerMetadata()) }
    var showMapEditor by remember { mutableStateOf(false) }

    // If map editor is active, show it instead of the dialog
    if (showMapEditor) {
        MarkerLocationEditorDialog(
            isVisible = true,
            mapManager = mapManager,
            onLocationSelected = { lat, lng, _ -> // address parameter unused
                scope.launch {
                    val locationName = repository.getPlaceFromCoordinates(lat, lng)
                        .getOrNull() ?: "Selected Location"

                    metadata = metadata.copy(
                        landmarkName = locationName,
                        latitude = lat,
                        longitude = lng
                    )
                }
                // Do NOT create preview marker - we'll create green marker on save only
                showMapEditor = false
            },
            onCancel = {
                showMapEditor = false
            }
        )
        return
    }

    // Use a Dialog so the overlay is rendered in a top-level window above other Compose content
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full screen overlay with semi-transparent scrim - appears ABOVE bottom sheet and any other UI
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Consume all taps on scrim to prevent clicks on elements below
                        onDismiss()
                    }
                }
                .semantics(mergeDescendants = false) {
                    contentDescription = "Marker action dialog overlay"
                },
            contentAlignment = Alignment.Center
        ) {
            // Centered card popup with marker actions - in FRONT of sheet
            // Using wrapContentSize to ensure dialog doesn't get clipped
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                MarkerActionDialogContent(
                    metadata = metadata,
                    onMetadataChange = { metadata = it },
                    onDismiss = onDismiss,
                    onSaveNotes = onSaveNotes,
                    onShowMapEditor = { showMapEditor = true },
                    // make dialog content fill the available width to match requested design
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.95f)
                )
            }
        }
    }
}

/**
 * Preview for the Marker Action Dialog.
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MarkerActionDialogPreview() {
    MaterialTheme {
        MarkerActionDialog(
            isVisible = true,
            onDismiss = {},
            onSaveNotes = { }
        )
    }
}
