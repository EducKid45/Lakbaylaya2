package com.example.lakbaylaya.ui.screens.map.components.markerEdit

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.maplibre.manager.MapLibreManager

/**
 * Map overlay for selecting a new marker location.
 * Shows an interactive map with a green marker fixed at the center while the map pans/zooms.
 * Users confirm the location and the map center coordinates are captured as the new marker location.
 * @param mapContent Composable lambda that renders the actual MapLibre map view
 * @param onLocationSelected Callback with latitude, longitude, and address when user confirms
 * @param onCancel Callback when user cancels the location selection
 */
@Composable
fun MarkerMapEditorOverlay(
    mapManager: MapLibreManager?,
    mapContent: @Composable () -> Unit,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit,
    onCancel: () -> Unit
) {
    // Map state - placeholder for actual map center coordinates
    var mapCenterLatitude by remember { mutableStateOf(40.7128) }
    var mapCenterLongitude by remember { mutableStateOf(-74.0060) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Render the actual MapLibre map view
        mapContent()

        // Map area - overlay for gesture handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
                // Consume drag gestures to pan map and prevent dialog dismissal
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        // Consume the gesture
                        change.consume()

                        // Clamp per-event pixel delta to avoid huge jumps if the gesture reports a large delta
                        val maxDelta = 300f
                        val dx = (-dragAmount.x).coerceIn(-maxDelta, maxDelta)
                        val dy = (-dragAmount.y).coerceIn(-maxDelta, maxDelta)

                        // Pan the underlying map by screen pixels (no degree conversion here)
                        try {
                            mapManager?.panBy(dx, dy, 0)
                        } catch (e: Exception) {
                            // Fallback: if panBy fails, try animateTo with tiny delta
                            val current = mapManager?.getCenter()
                            if (current != null) {
                                val newLat = current.first - (dy / 1000f)
                                val newLng = current.second + (dx / 1000f)
                                mapManager?.animateTo(newLat, newLng, zoom = 15.0, duration = 100)
                            }
                        }

                        // Read the actual map center after the pan and update displayed coords
                        mapManager?.getCenter()?.let { center ->
                            mapCenterLatitude = center.first
                            mapCenterLongitude = center.second
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // TODO: Integrate MapLibre view pan/zoom handling here

            // Center marker indicator - stays fixed at center while map pans
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                // Green marker icon
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "New marker location",
                    modifier = Modifier.size(56.dp),
                    tint = Color(0xFF4CAF50) // Green color for new marker
                )
            }

            // Location info card - shows current map center coordinates
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .widthIn(max = 300.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Lat: ${String.format(java.util.Locale.US, "%.4f", mapCenterLatitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Lng: ${String.format(java.util.Locale.US, "%.4f", mapCenterLongitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Bottom controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Get final map center coordinates
                        val finalCenter = mapManager?.getCenter()
                        val finalLat = finalCenter?.first ?: mapCenterLatitude
                        val finalLng = finalCenter?.second ?: mapCenterLongitude

                        onLocationSelected(
                            finalLat,
                            finalLng,
                            "Selected Location"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

















