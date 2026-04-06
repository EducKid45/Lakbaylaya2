package com.example.lakbaylaya.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lakbaylaya.data.api.GeoapifyApiImpl
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Which location type we are editing.
 */
enum class LocationMarkerType { HOME, WORK }

/**
 * Overlay dialog that allows the user to confirm / edit a lat-lon pair for
 * their home or work location.  The address is reverse-geocoded via Geoapify
 * and then saved immediately to Room through [ProfileViewModel].
 *
 * Usage: show this dialog on top of the map screen when the user taps
 * "Set home location" or "Set work location".  Pass the initially selected
 * lat/lon (e.g. from a map tap or the user's current position).
 *
 * @param type          HOME or WORK
 * @param initialLat    Latitude of the tapped/dropped marker
 * @param initialLon    Longitude of the tapped/dropped marker
 * @param onDismiss     Close without saving
 * @param onSaved       Invoked after the location is persisted (address, lat, lon)
 */
@Composable
fun LocationMarkerEditorDialog(
    type: LocationMarkerType,
    initialLat: Double,
    initialLon: Double,
    viewModel: ProfileViewModel = viewModel(),
    onDismiss: () -> Unit,
    onSaved: (address: String, lat: Double, lon: Double) -> Unit = { _, _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val api = remember { GeoapifyApiImpl() }

    var lat by remember { mutableDoubleStateOf(initialLat) }
    var lon by remember { mutableDoubleStateOf(initialLon) }
    var resolvedAddress by remember { mutableStateOf("Fetching address…") }
    var isFetching by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }

    // Reverse-geocode whenever lat/lon change
    LaunchedEffect(lat, lon) {
        if (lat == 0.0 && lon == 0.0) {
            resolvedAddress = "Invalid location"
            isFetching = false
            fetchError = true
            return@LaunchedEffect
        }
        isFetching = true
        fetchError = false
        val result = api.reverseGeocode(lat, lon)
        result.fold(
            onSuccess = { geocode ->
                resolvedAddress = when {
                    geocode.name.isNotBlank() -> geocode.name
                    geocode.address.isNotBlank() -> geocode.address
                    else -> "Unknown location"
                }
                isFetching = false
            },
            onFailure = {
                resolvedAddress = "Could not resolve address"
                isFetching = false
                fetchError = true
            }
        )
    }

    val icon = if (type == LocationMarkerType.HOME) Icons.Default.Home else Icons.Default.Work
    val iconColor = if (type == LocationMarkerType.HOME) Color(0xFF4CAF50) else Color(0xFFFF9800)
    val typeLabel = if (type == LocationMarkerType.HOME) "Home" else "Work / School"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                }
            }
        },
        title = {
            Text(
                "Set $typeLabel Location",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.semantics { contentDescription = "Set $typeLabel Location dialog" }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Address display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = iconColor)
                        } else {
                            Icon(
                                if (fetchError) Icons.Default.Warning else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (fetchError) Color(0xFFE53935) else iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = resolvedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (fetchError) Color(0xFFE53935) else Color(0xFF1A1A1A),
                            modifier = Modifier.semantics { contentDescription = "Resolved address: $resolvedAddress" }
                        )
                    }
                }

                // Coordinate display (read-only)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordChip(label = "Lat", value = String.format(Locale.US, "%.5f", lat), modifier = Modifier.weight(1f))
                    CoordChip(label = "Lon", value = String.format(Locale.US, "%.5f", lon), modifier = Modifier.weight(1f))
                }

                Text(
                    text = "Coordinates are set from the map marker. Tap OK to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addr = resolvedAddress
                    scope.launch {
                        if (type == LocationMarkerType.HOME) {
                            viewModel.updateHomeLocation(addr, lat, lon)
                        } else {
                            viewModel.updateWorkLocation(addr, lat, lon)
                        }
                        onSaved(addr, lat, lon)
                        onDismiss()
                    }
                },
                enabled = !isFetching,
                colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.semantics { contentDescription = "Confirm and save $typeLabel location" }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun CoordChip(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF888888))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Color(0xFF1A1A1A))
    }
}



