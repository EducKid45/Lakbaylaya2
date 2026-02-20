package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.components.bottomsheet.BottomSheetConstants
import com.example.lakbaylaya.ui.screens.map.components.bottomsheet.rememberBottomSheetController
import com.example.lakbaylaya.ui.screens.map.components.bottomsheet.rememberBottomSheetNestedScrollConnection
import com.example.lakbaylaya.ui.screens.map.models.BottomSheetState
import com.example.lakbaylaya.ui.screens.map.models.PlaceAction
import com.example.lakbaylaya.ui.screens.map.models.SearchResult
import com.example.lakbaylaya.utils.DistanceUtils
import java.util.Locale

/**
 * Modern bottom sheet for displaying place information and actions
 *
 * Features:
 * - Four distinct states: Hidden, Initial, Half, Full
 * - Google Maps-like smooth animations (no bounce)
 * - Swipe gestures for state transitions
 * - Drag handle for visual affordance (clickable to cycle states)
 * - Accessibility announcements for state changes
 * - Action buttons with icons
 * - Horizontal scrollable buttons in Initial state
 * - Scrollable content in Full state
 * - Close button in Half and Full states
 * - Accessibility support (TalkBack with state descriptions)
 * - Material 3 theming
 * - Respects safe area insets
 *
 * @param sheetState Current bottom sheet state
 * @param onStateChange Callback when sheet state changes
 * @param onActionClick Callback when action button is clicked
 * @param onDismiss Callback when close button is clicked
 * @param bottomNavigationHeight Height of bottom navigation bar to avoid overlap
 * @param modifier Modifier for customization
 */
@Composable
fun PlaceBottomSheet(
    sheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    onStateChange: (BottomSheetState) -> Unit,
    onActionClick: (PlaceAction, SearchResult) -> Unit,
    onDismiss: () -> Unit = {},
    bottomNavigationHeight: Dp = 80.dp,
    // Marker dialog state - passed from parent for higher z-index rendering
    showMarkerDialog: Boolean = false,
    onShowMarkerDialog: (Boolean, SearchResult?) -> Unit = { _, _ -> }
) {

    // Get screen height for calculating proportional heights
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    // Coroutine scope for animations
    val scope = rememberCoroutineScope()

    // Controller for physics and motion
    val controller = rememberBottomSheetController(
        scope = scope,
        screenHeightDp = screenHeightDp,
        bottomNavigationHeight = bottomNavigationHeight
    )

    // Update controller when state changes externally
    LaunchedEffect(sheetState) {
        controller.setState(sheetState)
    }

    // Calculate target height based on state
    val targetHeight = remember(sheetState, screenHeightDp) {
        when (sheetState) {
            is BottomSheetState.Hidden -> BottomSheetConstants.HANDLE_ONLY_HEIGHT
            is BottomSheetState.Initial -> BottomSheetConstants.INITIAL_STATE_HEIGHT
            is BottomSheetState.Half -> screenHeightDp * BottomSheetConstants.HALF_STATE_SCREEN_RATIO
            is BottomSheetState.Full -> screenHeightDp * BottomSheetConstants.FULL_STATE_SCREEN_RATIO
            is BottomSheetState.DirectionInitial -> screenHeightDp * BottomSheetConstants.DIRECTION_INITIAL_SCREEN_RATIO
            is BottomSheetState.DirectionFullExpand -> screenHeightDp * BottomSheetConstants.DIRECTION_FULL_SCREEN_RATIO
        }
    }

    // Smooth animation for height transitions
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = BottomSheetConstants.ANIMATION_DURATION_MS),
        label = "place_sheet_height"
    )

    val nestedScrollConnection = rememberBottomSheetNestedScrollConnection(
        sheetState = sheetState,
        controller = controller,
        onStateChange = onStateChange
    )

    // Accessibility announcement for state changes
    val stateDescription = remember(sheetState) {
        when (sheetState) {
            is BottomSheetState.Hidden -> "Bottom sheet hidden, showing handle only"
            is BottomSheetState.Initial -> "Bottom sheet collapsed, showing place preview"
            is BottomSheetState.Half -> "Bottom sheet half expanded, showing place details"
            is BottomSheetState.Full -> "Bottom sheet fully expanded, showing all information"
            is BottomSheetState.DirectionInitial -> "Direction sheet showing route overview"
            is BottomSheetState.DirectionFullExpand -> "Direction sheet showing turn-by-turn directions"
        }
    }

    val haptic = LocalHapticFeedback.current

    // Use both manual drag and nestedScroll
    // Manual drag for white space areas, nestedScroll for scrollable content
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .nestedScroll(nestedScrollConnection)
            .pointerInput(sheetState) {
                // Detect drags on non-scrollable areas (white space)
                detectVerticalDragGestures(
                    onDragEnd = {
                        controller.settleNearest(onStateChange)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = {
                        controller.settleNearest(onStateChange)
                    },
                    onVerticalDrag = { _, dragAmount ->
                        controller.dragBy(dragAmount)
                    }
                )
            }
            .semantics(mergeDescendants = false) {
                contentDescription = stateDescription
                role = Role.DropdownList
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
                .heightIn(min = animatedHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = BottomSheetConstants.CORNER_RADIUS,
                        topEnd = BottomSheetConstants.CORNER_RADIUS
                    )
                )
        ) {
            // Drag handle with state indicator
            DragHandle(
                state = sheetState,
                onClick = {
                    controller.cycleState(onStateChange)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            // Content based on state
            when (sheetState) {
                is BottomSheetState.Initial -> {
                    InitialContent(
                        place = sheetState.place,
                        onActionClick = { action ->
                            if (action == PlaceAction.MARK_LOCATION) {
                                onShowMarkerDialog(true, sheetState.place)
                            } else {
                                onActionClick(action, sheetState.place)
                            }
                        }
                    )
                }
                is BottomSheetState.Half -> {
                    HalfContent(
                        place = sheetState.place,
                        onActionClick = { action ->
                            if (action == PlaceAction.MARK_LOCATION) {
                                onShowMarkerDialog(true, sheetState.place)
                            } else {
                                onActionClick(action, sheetState.place)
                            }
                        },
                        onDismiss = onDismiss
                    )
                }
                is BottomSheetState.Full -> {
                    FullContent(
                        place = sheetState.place,
                        onActionClick = { action ->
                            if (action == PlaceAction.MARK_LOCATION) {
                                onShowMarkerDialog(true, sheetState.place)
                            } else {
                                onActionClick(action, sheetState.place)
                            }
                        },
                        onDismiss = onDismiss
                    )
                }
                is BottomSheetState.Hidden,
                is BottomSheetState.DirectionInitial,
                is BottomSheetState.DirectionFullExpand -> {
                    // Only handle visible Place states
                    // Direction states are handled by DirectionBottomSheet
                }
            }
        }
    }
}

/**
 * Drag handle component with state indicator for accessibility
 * Clickable to cycle through states
 */
@Composable
private fun DragHandle(
    state: BottomSheetState,
    onClick: () -> Unit
) {
    val stateLabel = remember(state) {
        when (state) {
            is BottomSheetState.Hidden -> "Tap to expand place details"
            is BottomSheetState.Initial -> "Tap to show more details"
            is BottomSheetState.Half -> "Tap to expand fully"
            is BottomSheetState.Full -> "Tap to collapse"
            is BottomSheetState.DirectionInitial -> "Direction sheet - tap to expand"
            is BottomSheetState.DirectionFullExpand -> "Direction sheet - tap to collapse"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = stateLabel
                role = Role.Button
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
 * Initial state content - place name and horizontal scrollable action buttons only
 */
@Composable
private fun InitialContent(
    place: SearchResult,
    onActionClick: (PlaceAction) -> Unit
) {
    val isTooFarForWalking = remember(place.distanceMeters) {
        place.distanceMeters > 50000 // 50km limit
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Reduced from 8dp
    ) {
        // Place name
        Text(
            text = place.placeName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = "Place name: ${place.placeName}"
            }
        )

        Spacer(modifier = Modifier.height(8.dp)) // Reduced from 12dp

        // Show compact error if too far
        if (isTooFarForWalking) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Too far for walking (${DistanceUtils.formatDistance(place.distanceMeters)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Horizontal scrollable action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics {
                    contentDescription = "Action buttons, swipe horizontally to view all options"
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PlaceAction.START_NAVIGATION,
                PlaceAction.DIRECTIONS,
                PlaceAction.MARK_LOCATION,
                PlaceAction.SAVE_PLACE,
            ).forEach { action ->
                val (icon, label) = getActionIconAndLabel(action)
                val isDisabled = isTooFarForWalking &&
                    (action == PlaceAction.START_NAVIGATION || action == PlaceAction.DIRECTIONS)

                CompactActionButton(
                    icon = icon,
                    label = label,
                    onClick = { onActionClick(action) },
                    enabled = !isDisabled
                )
            }
        }
    }
}

/**
 * Half state content - detailed information with close button
 */
@Composable
private fun HalfContent(
    place: SearchResult,
    onActionClick: (PlaceAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header with place name and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = place.placeName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Close place details and remove marker"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Address
        Text(
            text = place.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Distance, time, and steps - 2 in first line, 1 in second centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: Distance and Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min), // ensure children can match height when one wraps
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    text = DistanceUtils.formatDistance(place.distanceMeters),
                    contentDescription = "Distance: ${DistanceUtils.formatDistance(place.distanceMeters)}",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                TimeInfoChip(
                    timeText = DistanceUtils.estimateWalkingTime(place.distanceMeters),
                    contentDescription = "Walking time: ${DistanceUtils.estimateWalkingTime(place.distanceMeters)}",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Second row: Steps (centered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                InfoChip(
                    icon = Icons.Default.Hiking,
                    text = DistanceUtils.estimateSteps(place.distanceMeters),
                    contentDescription = "Estimated steps: ${DistanceUtils.estimateSteps(place.distanceMeters)}",
                    modifier = Modifier.widthIn(max = 160.dp) // Allow up to two lines if needed
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coordinates with label
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Coordinates",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.6f", place.latitude)}, ${String.format(Locale.US, "%.6f", place.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Coordinates: Latitude ${String.format(Locale.US, "%.6f", place.latitude)}, Longitude ${String.format(Locale.US, "%.6f", place.longitude)}"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show distance warning if too far for walking
        val isTooFarForWalking = remember(place.distanceMeters) {
            place.distanceMeters > 50000 // 50km limit
        }

        if (isTooFarForWalking) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distance Too Far",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Walking navigation unavailable. Distance (${DistanceUtils.formatDistance(place.distanceMeters)}) exceeds 50 km limit. Please choose a closer destination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action buttons
        ActionButtons(
            actions = listOf(
                PlaceAction.START_NAVIGATION,
                PlaceAction.DIRECTIONS,
                PlaceAction.MARK_LOCATION,
                PlaceAction.SAVE_PLACE,
            ),
            onActionClick = onActionClick,
            disabledActions = if (isTooFarForWalking) {
                setOf(PlaceAction.START_NAVIGATION, PlaceAction.DIRECTIONS)
            } else {
                emptySet()
            }
        )
    }
}

/**
 * Full state content - complete details with scrolling and close button
 */
@Composable
private fun FullContent(
    place: SearchResult,
    onActionClick: (PlaceAction) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // Header with place name and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = place.placeName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Close place details and remove marker"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Address
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Distance, time, and steps - 2 in first line, 1 in second centered
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: Distance and Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min), // ensure children can match height when one wraps
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        text = DistanceUtils.formatDistance(place.distanceMeters),
                        contentDescription = "Distance: ${DistanceUtils.formatDistance(place.distanceMeters)}",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    TimeInfoChip(
                        timeText = DistanceUtils.estimateWalkingTime(place.distanceMeters),
                        contentDescription = "Walking time: ${DistanceUtils.estimateWalkingTime(place.distanceMeters)}",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Second row: Steps (centered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    InfoChip(
                        icon = Icons.Default.Hiking,
                        text = DistanceUtils.estimateSteps(place.distanceMeters),
                        contentDescription = "Estimated steps: ${DistanceUtils.estimateSteps(place.distanceMeters)}",
                        modifier = Modifier.widthIn(max = 160.dp) // Allow up to two lines if needed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Coordinates with label
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Coordinates",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format(Locale.US, "%.6f", place.latitude)}, ${String.format(Locale.US, "%.6f", place.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Coordinates: Latitude ${String.format(Locale.US, "%.6f", place.latitude)}, Longitude ${String.format(Locale.US, "%.6f", place.longitude)}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Category
            if (place.category.isNotEmpty()) {
                Text(
                    text = "Category: ${place.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            // Show distance warning if too far for walking
            val isTooFarForWalking = place.distanceMeters > 50000 // 50km limit

            if (isTooFarForWalking) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Distance Too Far",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Walking navigation unavailable. Distance (${DistanceUtils.formatDistance(place.distanceMeters)}) exceeds 50 km limit. Please choose a closer destination.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // All action buttons
            ActionButtons(
                actions = listOf(
                    PlaceAction.START_NAVIGATION,
                    PlaceAction.DIRECTIONS,
                    PlaceAction.MARK_LOCATION,
                    PlaceAction.SAVE_PLACE,
                ),
                onActionClick = onActionClick,
                disabledActions = if (isTooFarForWalking) {
                    setOf(PlaceAction.START_NAVIGATION, PlaceAction.DIRECTIONS)
                } else {
                    emptySet()
                }
            )
        }
    }
}

/**
 * Info chip component for displaying distance/time/steps with icon
 * Bigger size for readability
 */
@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String = text
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Time chip that splits walking time into two lines: minutes on the first line and "X hr walk" on the second.
 * This helps keep the chip readable when the full label would otherwise wrap awkwardly.
 */
@Composable
private fun TimeInfoChip(
    timeText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Schedule,
    contentDescription: String = timeText
) {
    // Parse standard DistanceUtils.estimateWalkingTime outputs like:
    // "< 1 min walk", "5 min walk", "1 hr 15 min walk", "2 hrs walk", "1 hr walk"
    // Display hours first (if present) then minutes; both lines use the same typography size for visual parity.
    val (firstLine, secondLine) = remember(timeText) {
        val trimmed = timeText.trim()
        val hrRegex = "(\\d+)\\s*hr".toRegex()
        val minRegex = "(\\d+)\\s*min".toRegex()

        val hrMatch = hrRegex.find(trimmed)
        val minMatch = minRegex.find(trimmed)

        val hrs = hrMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        when {
            hrs > 0 && mins > 0 -> Pair("${hrs} hr", "${mins} min walk")
            hrs > 0 -> Pair("${hrs} hr", "walk")
            trimmed.contains("< 1") -> Pair("< 1 min", "walk")
            mins > 0 -> Pair("${mins} min", "walk")
            else -> Pair(trimmed, "walk")
        }
    }

    val textStyle = MaterialTheme.typography.bodyMedium

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = firstLine,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = secondLine,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact action button for horizontal scrolling in Initial state
 * Fixed width for consistent sizing
 */
@Composable
private fun CompactActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(90.dp) // Fixed width for consistency
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = if (enabled) {
                    "$label button"
                } else {
                    "$label button, disabled - distance too far"
                }
                role = Role.Button
            },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

/**
 * Action buttons grid
 */
@Composable
private fun ActionButtons(
    actions: List<PlaceAction>,
    onActionClick: (PlaceAction) -> Unit,
    disabledActions: Set<PlaceAction> = emptySet()
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    ActionButton(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f),
                        enabled = action !in disabledActions
                    )
                }
                // Fill remaining space if odd number
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Individual action button
 */
@Composable
private fun ActionButton(
    action: PlaceAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val (icon, label) = getActionIconAndLabel(action)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = if (enabled) {
                    "$label button"
                } else {
                    "$label button, disabled - distance too far"
                }
                role = Role.Button
            },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Get icon and label for action
 */
private fun getActionIconAndLabel(action: PlaceAction): Pair<ImageVector, String> {
    return when (action) {
        PlaceAction.START_NAVIGATION -> Icons.Default.Navigation to "Navigate"
        PlaceAction.SAVE_PLACE -> Icons.Default.Bookmark to "Save"
        PlaceAction.MARK_LOCATION -> Icons.Default.PushPin to "Mark"
        PlaceAction.SHARE -> Icons.Default.Share to "Share"
        PlaceAction.CALL -> Icons.Default.Call to "Call"
        PlaceAction.DIRECTIONS -> Icons.Default.Directions to "Directions"
    }
}
