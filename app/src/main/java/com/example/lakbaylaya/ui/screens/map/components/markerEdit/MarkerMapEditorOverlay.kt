package com.example.lakbaylaya.ui.screens.map.components.markerEdit

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import com.example.lakbaylaya.utils.LocationProvider
import com.example.lakbaylaya.utils.rememberLocationPermissionState
import kotlinx.coroutines.launch
import com.example.lakbaylaya.data.repository.MapRepositoryImpl
import com.example.lakbaylaya.data.api.GeoapifyApiImpl

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
    onCancel: () -> Unit,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialLabel: String? = null
) {
    // Map state - default to 0.0 until we read a real value
    var mapCenterLatitude by remember { mutableStateOf(0.0) }
    var mapCenterLongitude by remember { mutableStateOf(0.0) }
    var initialLabelState by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val locationProvider = remember { LocationProvider(context) }

    // Permission helper
    val locationPermission = rememberLocationPermissionState()

    // Repository for reverse geocoding
    val repository = remember { MapRepositoryImpl(GeoapifyApiImpl()) }

    // Display label (place name or "Current Location")
    var locationLabel by remember { mutableStateOf("Current Location") }

    val scope = rememberCoroutineScope()

    // Try to initialize the starting center: prefer an explicit initial latitude/longitude, otherwise map manager center, otherwise last known device location
    LaunchedEffect(mapManager) {
        // If caller provided initial coordinates (editor opened from search result), use them
        val providedLat = initialLatitude
        val providedLng = initialLongitude
        if (providedLat != null && providedLng != null) {
            mapCenterLatitude = providedLat
            mapCenterLongitude = providedLng
            initialLabelState = initialLabel

            // Move map to provided location
            try {
                mapManager?.animateTo(
                    mapCenterLatitude,
                    mapCenterLongitude,
                    zoom = 17.0,
                    duration = 200
                )
            } catch (_: Exception) {
                // ignore
            }

            // Attempt to refresh display label using reverse geocode (best effort)
            scope.launch {
                repository.getPlaceFromCoordinates(mapCenterLatitude, mapCenterLongitude)
                    .onSuccess { name ->
                        if (name.isNotBlank()) locationLabel = name
                    }
            }

            return@LaunchedEffect
        }

        // Otherwise fallback to map manager center then last known device location
        val center = mapManager?.getCenter()
        if (center != null) {
            mapCenterLatitude = center.first
            mapCenterLongitude = center.second

            // Try to reverse-geocode initial center for display
            scope.launch {
                repository.getPlaceFromCoordinates(mapCenterLatitude, mapCenterLongitude)
                    .onSuccess { name ->
                        locationLabel = if (name.isNotBlank()) name else "Current Location"
                    }
            }
        } else {
            // Ask LocationProvider for last known location as a fallback
            locationProvider.getLastLocation { location ->
                location?.let {
                    mapCenterLatitude = it.latitude
                    mapCenterLongitude = it.longitude

                    // Update label by reverse-geocoding
                    scope.launch {
                        repository.getPlaceFromCoordinates(mapCenterLatitude, mapCenterLongitude)
                            .onSuccess { name ->
                                locationLabel = if (name.isNotBlank()) name else "Current Location"
                            }
                    }

                    // Try to move the map to this location if map manager becomes available
                    try {
                        mapManager?.animateTo(
                            mapCenterLatitude,
                            mapCenterLongitude,
                            zoom = 15.0,
                            duration = 300
                        )
                    } catch (_: Exception) {
                        // ignore animation failures
                    }
                }
            }
        }
    }

    // Continuously update map center coordinates as user pans/zooms with MapLibre controls
    LaunchedEffect(mapManager) {
        if (mapManager != null) {
            while (true) {
                kotlinx.coroutines.delay(100) // Update every 100ms
                mapManager.getCenter()?.let { center ->
                    if (mapCenterLatitude != center.first || mapCenterLongitude != center.second) {
                        mapCenterLatitude = center.first
                        mapCenterLongitude = center.second

                        // Update location label when coordinates change significantly
                        scope.launch {
                            repository.getPlaceFromCoordinates(
                                mapCenterLatitude,
                                mapCenterLongitude
                            )
                                .onSuccess { name ->
                                    locationLabel =
                                        if (name.isNotBlank()) name else "Current Location"
                                }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Render the actual MapLibre map view
        mapContent()

        // Map area - allow MapLibre's native camera controls (pan, zoom, rotate)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            // MapLibre's native camera controls handle pan, zoom, and rotation gestures
            // These gestures are automatically enabled in MapLibreManager.configureMap()
            // Users can:
            // - Pan: Drag with one finger
            // - Zoom: Pinch with two fingers or double-tap
            // - Rotate: Rotate with two fingers
            // - Tilt: Drag up/down with two fingers

            // Camera control buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In button
                FloatingActionButton(
                    onClick = { mapManager?.zoomIn() },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Zoom Out button
                FloatingActionButton(
                    onClick = { mapManager?.zoomOut() },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center on user location button
                FloatingActionButton(
                    onClick = {
                        if (locationPermission.hasPermission) {
                            mapManager?.centerOnUserLocation()
                        } else {
                            locationPermission.requestPermission()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on My Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center marker indicator - stays fixed at center while map pans
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                // Green marker icon - tappable to fetch current device location + reverse geocode
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "New marker location",
                    modifier = Modifier
                        .size(56.dp)
                        .clickable {
                            // If permission not granted, request it and return; user must tap again after granting
                            if (!locationPermission.hasPermission) {
                                locationPermission.requestPermission()
                                return@clickable
                            }

                            // Try last known location first
                            locationProvider.getLastLocation { location ->
                                if (location != null) {
                                    mapCenterLatitude = location.latitude
                                    mapCenterLongitude = location.longitude

                                    // Update label by reverse-geocoding
                                    scope.launch {
                                        repository.getPlaceFromCoordinates(
                                            mapCenterLatitude,
                                            mapCenterLongitude
                                        )
                                            .onSuccess { name ->
                                                locationLabel =
                                                    if (name.isNotBlank()) name else "Current Location"
                                            }
                                    }

                                    try {
                                        mapManager?.animateTo(
                                            mapCenterLatitude,
                                            mapCenterLongitude,
                                            zoom = 15.0,
                                            duration = 300
                                        )
                                    } catch (_: Exception) {
                                        // ignore
                                    }
                                } else {
                                    // If last known is null, start a short location update and stop after first result
                                    locationProvider.startLocationUpdates { loc ->
                                        mapCenterLatitude = loc.latitude
                                        mapCenterLongitude = loc.longitude

                                        // Update label by reverse-geocoding
                                        scope.launch {
                                            repository.getPlaceFromCoordinates(
                                                mapCenterLatitude,
                                                mapCenterLongitude
                                            )
                                                .onSuccess { name ->
                                                    locationLabel =
                                                        if (name.isNotBlank()) name else "Current Location"
                                                }
                                        }

                                        try {
                                            mapManager?.animateTo(
                                                mapCenterLatitude,
                                                mapCenterLongitude,
                                                zoom = 15.0,
                                                duration = 300
                                            )
                                        } catch (_: Exception) {
                                            // ignore
                                        }

                                        // Stop updates after first fix
                                        locationProvider.stopLocationUpdates()
                                    }
                                }
                            }
                        },
                    tint = Color(0xFF4CAF50) // Green color for new marker
                )
            }

            // Location info card - shows name and current map center coordinates
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
                        // Prefer initialLabelState (open-from-search) when present, otherwise the live locationLabel
                        initialLabelState ?: locationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            locationLabel
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
