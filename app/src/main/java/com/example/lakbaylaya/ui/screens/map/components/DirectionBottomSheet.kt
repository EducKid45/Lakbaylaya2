package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.lakbaylaya.ui.screens.map.components.bottomsheet.BottomSheetConstants
import com.example.lakbaylaya.ui.screens.map.models.BottomSheetState
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.example.lakbaylaya.ui.screens.map.models.DirectionData
import com.example.lakbaylaya.ui.screens.map.models.RouteOption
import com.example.lakbaylaya.utils.DirectionIconMapper
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Direction bottom sheet - Shows turn-by-turn steps, route alternatives, and actions
 *
 * Route selection is done by tapping polylines on the map or choosing from alternatives.
 * Stop editing is done in the Overlay Stop Panel.
 *
 * Features:
 * - Two states: DirectionInitial (collapsed) and DirectionFullExpand (full steps)
 * - Action buttons: Start, Add stops, Save
 * - Address display for origin and destination
 * - Route alternatives for walking (for accessibility)
 * - Route comparison (time, distance, etc.)
 * - Scrollable step list with independent scrolling
 * - Route summary (distance, time, steps count)
 * - Smooth animations and gestures
 * - Scroll-to-bottom detection for auto-expand
 *
 * @param sheetState Current direction sheet state
 * @param onStateChange Callback when sheet state changes
 * @param onRouteSelected Callback when a route is selected
 * @param modifier Modifier for customization
 * @param bottomNavigationHeight Height of bottom navigation bar
 */
@Composable
fun DirectionBottomSheet(
    sheetState: BottomSheetState,
    onStateChange: (BottomSheetState) -> Unit,
    onRouteSelected: (Int) -> Unit = {},
    onAddStopsClick: () -> Unit = {},
    isEditingStops: Boolean = false,
    topInsetAdjustment: Dp = 100.dp,
    modifier: Modifier = Modifier,
    bottomNavigationHeight: Dp = 80.dp,
    onClose: () -> Unit = {}, // Called when user presses the close X to exit directions mode
    onStartNavigation: () -> Unit = {}, // Called when user presses the Start button to begin navigation
    onSaveRoute: (DirectionData) -> Unit = {}, // Provide the full DirectionData for saving
    /** Called when user taps the speaker icon on an individual direction step. */
    onSpeakStep: ((String) -> Unit)? = null
) {
    // Only render for Direction states
    val directionData = when (sheetState) {
        is BottomSheetState.DirectionInitial -> sheetState.directionData
        is BottomSheetState.DirectionFullExpand -> sheetState.directionData
        is BottomSheetState.Hidden -> null
        else -> return
    }

    // If Hidden state, don't render
    if (sheetState is BottomSheetState.Hidden || directionData == null) {
        return
    }

    val selectedRoute = directionData.getSelectedRoute() // Can be null during loading

    // Loading state: only show when route is being added/removed/updated
    // If routes exist and are up-to-date, don't show loading
    val isLoadingRoute = remember(directionData.routes.size, directionData.routes.firstOrNull()) {
        directionData.routes.isEmpty()
    }

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val haptics = LocalHapticFeedback.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTopPadding = (statusBarPadding - topInsetAdjustment).coerceAtLeast(0.dp)

    // Simple two-state toggle: Collapsed or Expanded
    // LOCKED to collapsed during loading
    var isExpanded by remember(sheetState) {
        mutableStateOf(sheetState is BottomSheetState.DirectionFullExpand && !isLoadingRoute)
    }

    // Track drag offset for toggling (like Location sheet)
    var cumulativeDragOffset by remember { mutableStateOf(0f) }

    // Reset drag offset when expansion changes
    LaunchedEffect(isExpanded) {
        cumulativeDragOffset = 0f
    }

    // Lock to collapsed when loading
    LaunchedEffect(isLoadingRoute) {
        if (isLoadingRoute) {
            isExpanded = false
        }
    }

    // Calculate sheet height based on state (unified approach)
    val targetHeight = remember(isExpanded, screenHeightDp) {
        if (isExpanded) {
            (screenHeightDp - effectiveTopPadding).coerceAtLeast(0.dp)
        } else {
            screenHeightDp * BottomSheetConstants.DIRECTION_INITIAL_SCREEN_RATIO
        }
    }

    // Unified smooth animation (same as Location and Place sheets)
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = BottomSheetConstants.ANIMATION_DURATION_MS),
        label = "direction_sheet_height"
    )

    // Accessibility description
    val stateDescription = remember(isExpanded) {
        if (isExpanded) {
            "Turn-by-turn directions, swipe down to collapse"
        } else {
            "Route overview, swipe up for turn-by-turn directions"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .offset(y = if (isExpanded) effectiveTopPadding else bottomNavigationHeight)
            .pointerInput(isExpanded, isLoadingRoute) {
                // Unified drag handling (same as Location sheet)
                // DISABLED during loading
                if (!isLoadingRoute) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(cumulativeDragOffset) > BottomSheetConstants.FLING_THRESHOLD_PX) {
                                isExpanded = !isExpanded
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // Update parent state
                                val newState = if (isExpanded) {
                                    BottomSheetState.DirectionFullExpand(directionData)
                                } else {
                                    BottomSheetState.DirectionInitial(directionData)
                                }
                                onStateChange(newState)
                            }
                            cumulativeDragOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            cumulativeDragOffset += dragAmount
                        }
                    )
                }
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle - unified with Location sheet pattern
                // DISABLED during loading
                DragHandle(
                    isExpanded = isExpanded,
                    enabled = !isLoadingRoute,
                    onClick = {
                        if (!isLoadingRoute) {
                            isExpanded = !isExpanded
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newState = if (isExpanded) {
                                BottomSheetState.DirectionFullExpand(directionData)
                            } else {
                                BottomSheetState.DirectionInitial(directionData)
                            }
                            onStateChange(newState)
                        }
                    }
                )

                // Show appropriate content based on expansion state
                if (isExpanded) {
                    // Only show expanded content if we have a route
                    if (selectedRoute != null) {
                        DirectionFullContent(
                            route = selectedRoute,
                            directionData = directionData,
                            onAddStopsClick = onAddStopsClick,
                            onStartNavigation = onStartNavigation,
                            onSaveRoute = { onSaveRoute(directionData) },
                            onSpeakStep = onSpeakStep
                        )
                    }
                } else {
                    DirectionInitialContent(
                        directionData = directionData,
                        route = selectedRoute, // Can be null during loading
                        onRouteSelected = onRouteSelected,
                        onStateChange = onStateChange,
                        onAddStopsClick = onAddStopsClick,
                        isEditingStops = isEditingStops,
                        onStartNavigation = onStartNavigation,
                        onSaveRoute = { onSaveRoute(directionData) }
                    )
                }
            }

            // Overlay close button so it always stays above content and can't be occluded
            IconButton(
                onClick = { onStateChange(BottomSheetState.Hidden); onClose() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 8.dp)
                    .size(40.dp)
                    .zIndex(4f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close directions",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Drag handle for Direction sheet (unified with Location sheet pattern)
 */
@Composable
private fun DragHandle(
    isExpanded: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val stateLabel = remember(isExpanded, enabled) {
        if (!enabled) {
            "Loading route"
        } else if (isExpanded) {
            "Tap to collapse directions"
        } else {
            "Tap to expand directions"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = stateLabel
                role = Role.Button
            }
            .padding(vertical = BottomSheetConstants.DRAG_HANDLE_VERTICAL_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(BottomSheetConstants.DRAG_HANDLE_WIDTH)
                .height(BottomSheetConstants.DRAG_HANDLE_HEIGHT)
                .clip(RoundedCornerShape(BottomSheetConstants.DRAG_HANDLE_CORNER_RADIUS))
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) BottomSheetConstants.DRAG_HANDLE_ALPHA else 0.2f
                    )
                )
        )
    }
}

/**
 * Initial state content - route summary, addresses, actions, and route alternatives
 */
@Composable
private fun DirectionInitialContent(
    directionData: DirectionData,
    route: RouteOption?, // Nullable to handle loading state
    onRouteSelected: (Int) -> Unit,
    onStateChange: (BottomSheetState) -> Unit,
    onAddStopsClick: () -> Unit,
    isEditingStops: Boolean,
    onStartNavigation: () -> Unit,
    onSaveRoute: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // NestedScrollConnection to detect upward scroll attempts when already at bottom
    val nestedScrollConnection = remember(directionData) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y < 0 means user is scrolling up (trying to move content up / pull sheet)
                // Only trigger expansion when scrolling up from bottom
                if (available.y < 0f && !listState.canScrollForward) {
                    val layoutInfo = listState.layoutInfo
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    val isAtBottom = lastVisibleItem?.index == layoutInfo.totalItemsCount - 1 &&
                            lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset

                    if (isAtBottom) {
                        // Only trigger full expand if we have a valid route (not loading)
                        if (directionData.routes.isNotEmpty()) {
                            coroutineScope.launch {
                                onStateChange(BottomSheetState.DirectionFullExpand(directionData))
                            }
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Detect scroll to bottom and expand sheet (kept as fallback)
    LaunchedEffect(listState, directionData.routes.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val isAtBottom = lastVisibleItem?.index == layoutInfo.totalItemsCount - 1 &&
                    lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset
            Pair(isAtBottom, listState.isScrollInProgress)
        }.collect { (isAtBottom, isScrolling) ->
            if (isAtBottom && isScrolling && !listState.canScrollForward) {
                // Only expand if we have routes (not in loading state)
                if (directionData.routes.isNotEmpty()) {
                    onStateChange(BottomSheetState.DirectionFullExpand(directionData))
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
    ) {
        item {
            // Header: Route name + Destination address only (compact) with Close button
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), // fixed header height to center content vertically
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = route?.name ?: "Route",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Destination address only (hide place name)
                        if (directionData.destination.address.isNotEmpty()) {
                            Text(
                                text = directionData.destination.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // (Close button removed from header - overlay IconButton is used instead)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            // Route summary or loading state
            if (directionData.routes.isEmpty() || route == null) {
                // Loading state with shimmer effect
                LoadingRouteCard()
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                RouteSummaryCard(route = route)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            // Action buttons: Start, Add stops, Save (horizontally scrollable fixed-width buttons)
            ActionButtonsRow(
                modifier = Modifier.fillMaxWidth(),
                onAddStopsClick = onAddStopsClick,
                onStartNavigation = onStartNavigation,
                onSaveRoute = onSaveRoute
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Only show Route Options if there are alternatives (more than 1 route)
        if (directionData.routes.size > 1) {
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Route Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                // Route alternatives for walking (for blind users)
                RouteAlternativesSection(
                    directionData = directionData,
                    onRouteSelected = onRouteSelected
                )
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

/**
 * Loading state card for route computation - Visually appealing design
 */
@Composable
private fun LoadingRouteCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prominent text message
            Text(
                text = "Getting route…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Please wait while we calculate the best path",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Shimmer placeholder boxes with better design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Icon placeholder
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.15f - (index * 0.03f)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Text placeholder
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f - (index * 0.02f)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Subtitle placeholder
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.08f - (index * 0.01f)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full expand state content - complete turn-by-turn directions with sticky buttons
 */
@Composable
private fun DirectionFullContent(
    route: RouteOption,
    directionData: DirectionData,
    onAddStopsClick: () -> Unit = {},
    onStartNavigation: () -> Unit = {},
    onSaveRoute: () -> Unit = {},
    onSpeakStep: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header (non-scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Route ${route.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // (Close button removed from header - overlay IconButton is used instead)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route summary
                RouteSummaryCard(route = route)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scrollable turn-by-turn steps with bottom padding for sticky buttons
            // Build route segments (origin -> stop1 -> stop2 -> destination)
            val segments = buildRouteSegments(directionData, route)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
            ) {
                segments.forEachIndexed { segmentIndex, segment ->
                    item(key = "segment_header_$segmentIndex") {
                        RouteSegmentHeader(
                            fromName = segment.fromName,
                            toName = segment.toName,
                            isExpanded = segment.isExpanded,
                            onToggle = { segment.isExpanded = !segment.isExpanded }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (segment.isExpanded) {
                        items(
                            items = segment.steps,
                            key = { step -> "segment_${segmentIndex}_step_${step.instruction}_${step.distanceMeters}" }
                        ) { step ->
                            DirectionStepItem(step = step, onSpeakStep = onSpeakStep)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Divider between segments
                    if (segmentIndex < segments.size - 1) {
                        item(key = "segment_divider_$segmentIndex") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }

        // Sticky action buttons at bottom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            ActionButtonsRow(
                modifier = Modifier.padding(16.dp),
                onAddStopsClick = onAddStopsClick,
                onStartNavigation = onStartNavigation,
                onSaveRoute = onSaveRoute
            )
        }
    }
}

/**
 * Data class representing a route segment (from one point to another)
 */
@Stable
private class RouteSegment(
    val fromName: String,
    val toName: String,
    val steps: List<DirectionStep>,
    initialExpanded: Boolean = true
) {
    var isExpanded by mutableStateOf(initialExpanded)
}

/**
 * Build route segments from DirectionData
 * Splits steps based on stops
 */
@Composable
private fun buildRouteSegments(directionData: DirectionData, route: RouteOption): List<RouteSegment> {
    val segments = remember(directionData, route) {
        mutableStateListOf<RouteSegment>()
    }

    // Initialize segments if empty
    LaunchedEffect(directionData, route) {
        segments.clear()

        if (directionData.stops.isEmpty()) {
            // Single segment: origin -> destination
            segments.add(
                RouteSegment(
                    fromName = directionData.origin.name,
                    toName = directionData.destination.name,
                    steps = route.steps,
                    initialExpanded = true
                )
            )
        } else {
            // Multiple segments with stops
            val points = listOf(directionData.origin) + directionData.stops + listOf(directionData.destination)

            // Create segments between consecutive points
            // Note: This is simplified - in production you'd split the actual steps
            val stepsPerSegment = if (points.size > 1) route.steps.size / (points.size - 1) else route.steps.size

            for (i in 0 until points.size - 1) {
                val startIdx = i * stepsPerSegment
                val endIdx = if (i == points.size - 2) route.steps.size else (i + 1) * stepsPerSegment
                val segmentSteps = if (startIdx < route.steps.size) {
                    route.steps.subList(
                        startIdx.coerceIn(0, route.steps.size),
                        endIdx.coerceIn(startIdx, route.steps.size)
                    )
                } else {
                    emptyList()
                }

                segments.add(
                    RouteSegment(
                        fromName = points[i].name,
                        toName = points[i + 1].name,
                        steps = segmentSteps,
                        initialExpanded = i == 0 // Only expand first segment by default
                    )
                )
            }
        }
    }

    return segments
}

/**
 * Route segment header with expand/collapse functionality
 */
@Composable
private fun RouteSegmentHeader(
    fromName: String,
    toName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "From: $fromName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "To: $toName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Route summary card showing time, distance, steps
 */
@Composable
private fun RouteSummaryCard(
    route: RouteOption
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeSummaryItem(
                totalMinutes = route.totalDurationMinutes,
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = Color(0xFFEEEEEE)
            )

            SummaryItem(
                icon = Icons.Default.Straighten,
                value = route.getFormattedDistance(),
                label = "Distance",
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = Color(0xFFEEEEEE)
            )

            SummaryItem(
                icon = Icons.Default.Hiking,
                value = route.getEstimatedSteps(),
                label = "Steps",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Time summary item (hours on first line, minutes on second line per spec)
 */
@Composable
private fun TimeSummaryItem(
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Icon for the time section
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Time",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (hours > 0) {
            Text(
                text = "${hours} hr",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (minutes > 0) {
                Text(
                    text = "${minutes} min",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = "${minutes} min",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "Time",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Summary item (time/distance/steps)
 */
@Composable
private fun SummaryItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Action buttons row - Start, Add stops, Save
 */
@Composable
private fun ActionButtonsRow(
    modifier: Modifier = Modifier,
    onAddStopsClick: () -> Unit = {},
    onStartNavigation: () -> Unit = {},
    onSaveRoute: () -> Unit = {}
) {
    // Fixed button width to avoid compressing text on small devices; container scrolls horizontally
    val buttonWidth = 160.dp
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Start button (primary)
        Button(
            onClick = onStartNavigation,
            modifier = Modifier.width(buttonWidth),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Add stops button
        OutlinedButton(
            onClick = onAddStopsClick,
            modifier = Modifier.width(buttonWidth)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add stops",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Save button
        OutlinedButton(
            onClick = onSaveRoute,
            modifier = Modifier.width(buttonWidth)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Route alternatives section showing different route options for walking
 * Designed for accessibility (especially for blind users)
 * Maximum of 3 routes displayed
 */
@Composable
private fun RouteAlternativesSection(
    directionData: DirectionData,
    onRouteSelected: (Int) -> Unit
) {
    // Use real routes from DirectionData, limited to 3
    val routes = directionData.routes.take(3)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        routes.forEachIndexed { index, route ->
            RouteAlternativeItem(
                route = route,
                isSelected = index == directionData.selectedRouteIndex,
                onClick = { onRouteSelected(index) }
            )
            if (index < routes.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Get route color - use single primary color for consistency
 */
@Composable
private fun getRouteColor(): Color {
    return MaterialTheme.colorScheme.primary
}

/**
 * Get route container color based on selection
 */
@Composable
private fun getRouteContainerColor(isSelected: Boolean): Color {
    return if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
}

/**
 * Individual route alternative item
 */
@Composable
private fun RouteAlternativeItem(
    route: RouteOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val routeColor = getRouteColor()
    val containerColor = getRouteContainerColor(isSelected)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                routeColor
            )
        } else androidx.compose.foundation.BorderStroke(
            1.dp,
            routeColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Colored route indicator badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(routeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = route.name.takeLast(1), // Shows "A", "B", or "C"
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (route.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = routeColor
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    contentDescription = if (isSelected) "Selected" else null,
                    tint = if (isSelected) routeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RouteStatItem(
                    icon = Icons.Default.Schedule,
                    value = route.getFormattedDuration(),
                    modifier = Modifier.weight(1f)
                )
                RouteStatItem(
                    icon = Icons.Default.Straighten,
                    value = route.getFormattedDistance(),
                    modifier = Modifier.weight(1f)
                )
                RouteStatItem(
                    icon = Icons.Default.Hiking,
                    value = route.getEstimatedSteps(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Route stat item for alternatives
 */
@Composable
private fun RouteStatItem(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/**
 * Individual direction step item — shows maneuver icon, instruction text,
 * distance/steps, and a TTS spoken-instruction chip when available.
 */
@Composable
private fun DirectionStepItem(
    step: DirectionStep,
    onSpeakStep: ((String) -> Unit)? = null
) {
    // Keep the raw instruction (from route) visible and only show the parsed/spoken
    // instruction when it's non-empty and different from the raw instruction.
    val rawInstruction = step.instruction.trim()
    val parsedInstruction = step.spokenInstruction.trim()
    val showParsed = parsedInstruction.isNotBlank() && !parsedInstruction.equals(rawInstruction, ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Maneuver icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = DirectionIconMapper.getIconForManeuver(step.maneuver),
                        contentDescription = DirectionIconMapper.getContentDescription(step.maneuver),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Step details: show raw instruction as primary text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rawInstruction,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = step.getFormattedDistance(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${step.getEstimatedSteps()} steps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Speaker button — play parsed instruction when available, otherwise raw instruction
            val speakText = if (parsedInstruction.isNotBlank()) parsedInstruction else rawInstruction
            if (speakText.isNotBlank() && onSpeakStep != null) {
                IconButton(
                    onClick = { onSpeakStep(speakText) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Speak step",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Only show the parsed/spoken instruction chip when it's different from the raw instruction
        if (showParsed) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp) // align under the text column
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = parsedInstruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Individual address item
 */
@Composable
private fun AddressItem(
    label: String,
    name: String,
    address: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        if (address.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
