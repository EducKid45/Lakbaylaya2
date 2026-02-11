package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.components.bottomsheet.BottomSheetConstants
import com.example.lakbaylaya.ui.screens.map.models.Location
import com.example.lakbaylaya.ui.screens.map.models.ReverseGeocodedLocation
import java.util.Locale
import kotlin.math.abs

/**
 * User location bottom sheet for displaying current location
 *
 * Features:
 * - Two states: Hidden (handle only) and Initial (location info)
 * - Shows when no search is active
 * - Displays current coordinates and location name
 * - Reverse geocoded location with closest amenity
 * - Smooth Google Maps-like animation
 * - Drag/click to toggle between states
 * - Accessibility support
 * - Material 3 theming
 *
 * @param isVisible Whether the sheet should be visible (hidden when searching)
 * @param currentLocation User's current location
 * @param reverseGeocodedLocation Reverse geocoded location name
 * @param isExpanded Whether the sheet is expanded (Initial) or collapsed (Hidden)
 * @param onToggle Callback when sheet is toggled
 * @param modifier Modifier for customization
 * @param bottomNavigationHeight Height of bottom navigation bar to avoid overlap
 */
@Composable
fun UserLocationBottomSheet(
    isVisible: Boolean,
    currentLocation: Location?,
    reverseGeocodedLocation: ReverseGeocodedLocation?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavigationHeight: Dp = 80.dp
) {
    // Only show if visible and has location
    if (!isVisible || currentLocation == null) return

    // Calculate sheet height based on expanded state
    val targetHeight by remember(isExpanded) {
        derivedStateOf {
            if (isExpanded) {
                BottomSheetConstants.INITIAL_STATE_HEIGHT
            } else {
                BottomSheetConstants.HANDLE_ONLY_HEIGHT
            }
        }
    }

    // Google Maps-like smooth animation
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(
            durationMillis = BottomSheetConstants.ANIMATION_DURATION_MS
        ),
        label = "user_location_sheet_height"
    )

    // Track drag offset for toggling
    var cumulativeDragOffset by remember { mutableStateOf(0f) }

    // Reset drag offset when state changes
    LaunchedEffect(isExpanded) {
        cumulativeDragOffset = 0f
    }

    // Accessibility description
    val stateDescription = remember(isExpanded, reverseGeocodedLocation) {
        if (isExpanded) {
            "Current location: ${reverseGeocodedLocation?.getDisplayLabel() ?: "Loading..."}"
        } else {
            "Current location hidden, tap to show"
        }
    }

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (abs(cumulativeDragOffset) > BottomSheetConstants.FLING_THRESHOLD_PX) {
                            onToggle()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        cumulativeDragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeDragOffset += dragAmount
                    }
                )
            }
            .semantics(mergeDescendants = false) {
                contentDescription = stateDescription
            },
        shape = RoundedCornerShape(
            topStart = BottomSheetConstants.CORNER_RADIUS,
            topEnd = BottomSheetConstants.CORNER_RADIUS
        ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = BottomSheetConstants.TONAL_ELEVATION,
        shadowElevation = BottomSheetConstants.SHADOW_ELEVATION
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = targetHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = BottomSheetConstants.CORNER_RADIUS,
                        topEnd = BottomSheetConstants.CORNER_RADIUS
                    )
                )
        ) {
            // Drag handle
            LocationDragHandle(
                isExpanded = isExpanded,
                onClick = {
                    onToggle()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            // Content (only when expanded)
            if (isExpanded) {
                LocationContent(
                    currentLocation = currentLocation,
                    reverseGeocodedLocation = reverseGeocodedLocation
                )
            }
        }
    }
}

/**
 * Drag handle for user location sheet
 */
@Composable
private fun LocationDragHandle(
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val stateLabel = remember(isExpanded) {
        if (isExpanded) {
            "Tap to hide current location"
        } else {
            "Tap to show current location"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = stateLabel
            }
            .padding(vertical = BottomSheetConstants.DRAG_HANDLE_VERTICAL_PADDING),
        contentAlignment = Alignment.Center
    ) {
        // Visual drag indicator
        Box(
            modifier = Modifier
                .width(BottomSheetConstants.DRAG_HANDLE_WIDTH)
                .height(BottomSheetConstants.DRAG_HANDLE_HEIGHT)
                .clip(RoundedCornerShape(BottomSheetConstants.DRAG_HANDLE_CORNER_RADIUS))
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = BottomSheetConstants.DRAG_HANDLE_ALPHA
                    )
                )
        )
    }
}

/**
 * Location content showing current coordinates and location name
 */
@Composable
private fun LocationContent(
    currentLocation: Location,
    reverseGeocodedLocation: ReverseGeocodedLocation?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header with icon and location name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reverseGeocodedLocation?.getDisplayLabel() ?: "Current Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show secondary info if available
                reverseGeocodedLocation?.getSecondaryInfo()?.let { secondaryInfo ->
                    if (secondaryInfo.isNotBlank()) {
                        Text(
                            text = secondaryInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coordinates - one line with label
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Coordinates",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.6f", currentLocation.latitude)}, ${String.format(Locale.US, "%.6f", currentLocation.longitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Coordinates: Latitude ${String.format(Locale.US, "%.6f", currentLocation.latitude)}, Longitude ${String.format(Locale.US, "%.6f", currentLocation.longitude)}"
                }
            )
        }
    }
}





