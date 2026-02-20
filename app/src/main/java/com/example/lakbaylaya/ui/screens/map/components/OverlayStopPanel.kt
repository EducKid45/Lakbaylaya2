package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.snapshotFlow
import com.example.lakbaylaya.ui.screens.map.models.DirectionData
import com.example.lakbaylaya.ui.screens.map.models.RoutePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Overlay panel that shows origin, stops and destination and allows reordering via long-press drag.
 *
 * REDESIGNED unified drag-and-swap logic:
 * - A stable `localKeys` list represents ALL draggable items: [stop0, stop1, ..., destination].
 * - Destination is treated as a normal item at the last index in localKeys.
 * - All drag computations use indices into `localKeys` (local indices).
 * - Placeholder offsets are managed per-key to enable smooth animations.
 * - Only affected items have their offsets updated during drag.
 * - Drag state is cleared only when drag ends, not mid-animation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayStopPanel(
    directionData: DirectionData,
    isEditingStops: Boolean,
    onBack: () -> Unit,
    onAddStop: () -> Unit,
    onRemoveStop: (Int) -> Unit,
    onSwapOriginDestination: () -> Unit,
    onSwapStops: (Int, Int) -> Unit,
    onSwapStopWithDestination: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onExpandedChange: ((Boolean) -> Unit)? = null
) {

    var isExpanded by remember { mutableStateOf(false) }

    data class SwapSelection(val isDestination: Boolean, val localIndex: Int)
    var swapSelection by remember { mutableStateOf<SwapSelection?>(null) }
    fun clearSwapSelection() { swapSelection = null }

    LaunchedEffect(isEditingStops) { isExpanded = isEditingStops }
    LaunchedEffect(directionData.stops.size, isEditingStops) {
        if (isEditingStops && directionData.stops.isNotEmpty()) isExpanded = true
    }
    LaunchedEffect(isExpanded) { onExpandedChange?.invoke(isExpanded) }

    Surface(
        // Tap only expands. Collapse is handled by the header back button when expanded.
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (!isEditingStops && !isExpanded) { isExpanded = true; onExpandedChange?.invoke(true) } }
            .semantics { contentDescription = "Direction stop panel" },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show collapse/back icon only when expanded.
                // If user is editing stops, use the back button to finish editing (onDone).
                if (isExpanded) {
                    IconButton(onClick = {
                        if (isEditingStops) {
                            onDone()
                        } else {
                            isExpanded = false; onExpandedChange?.invoke(false)
                        }
                    }, modifier = Modifier.semantics { contentDescription = "Collapse direction panel" }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = "Directions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                swapSelection?.let { sel ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = when {
                                sel.isDestination -> "Selected: Destination"
                                sel.localIndex >= 0 -> "Selected: Stop ${sel.localIndex + 1}"
                                else -> "Selected"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                // Header controls: show Add when expanded. Done button removed; back will finish editing.
                if (isExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onAddStop, modifier = Modifier.size(40.dp)) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add stop", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isOriginUserLocation = directionData.origin.name.contains("your", ignoreCase = true)
                    || directionData.origin.name.contains("my", ignoreCase = true)
                    || directionData.origin.name.contains("current", ignoreCase = true)

            // Whether any stops exist — used to determine whether swap affordances should be visible
            val hasStops = directionData.stops.isNotEmpty()

            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            val itemHeightDp = 72.dp
            val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }

            // Build unified list (stops + destination) in the composable scope so its size
            // can be used consistently for clamping and destination detection.
            val unifiedList = remember(directionData.stops, directionData.destination) {
                val list = mutableListOf<RoutePoint>()
                list.addAll(directionData.stops)
                list.add(directionData.destination)
                list
            }

             // === REDESIGNED UNIFIED DRAG STATE ===
            // Track which item key is being dragged
            var draggingKey by remember { mutableStateOf<String?>(null) }

            // draggedOffset: cumulative pixel offset for the currently dragged item only
            var draggedOffset by remember { mutableStateOf(0f) }

            // placeholderOffsets: visual shift for OTHER items to preview insertion position
            // This map persists across frames to enable smooth animation via animateFloatAsState
            val placeholderOffsets = remember { mutableStateMapOf<String, Float>() }

            // dragStartIndex: where in localKeys the drag began
            var dragStartIndex by remember { mutableStateOf<Int?>(null) }

            // dragTargetIndex: computed insertion position in localKeys during drag
            var dragTargetIndex by remember { mutableStateOf<Int?>(null) }
            // lastAppliedTarget: the last integer target we used to update placeholderOffsets.
            // Debouncing updates to placeholder offsets avoids rapid flip-flopping / jiggle.
            var lastAppliedTarget by remember { mutableStateOf<Int?>(null) }

            // Helper: clear all drag-related transient state consistently
            fun clearDragState() {
                draggingKey = null
                draggedOffset = 0f
                placeholderOffsets.clear()
                swapSelection = null
                dragStartIndex = null
                dragTargetIndex = null
                lastAppliedTarget = null
            }

            // localKeys: stable list of [stop_key_0, stop_key_1, ..., dest_key]
            val localKeys = remember { mutableStateListOf<String>() }

            // build a stable destination key (will be unique)
            val destinationKey = remember(directionData.destination.latitude, directionData.destination.longitude, directionData.destination.name) {
                "dest_${directionData.destination.latitude}_${directionData.destination.longitude}_${directionData.destination.name}"
            }

            var swapInProgress by remember { mutableStateOf(false) }

            // Utility mapping functions between local keys index and stops index
            fun localKeyIndexToStopIndex(localIndex: Int): Int? {
                return if (localIndex in 0 until directionData.stops.size) localIndex else null
            }
            fun stopIndexToLocalKeyIndex(stopIndex: Int): Int? {
                return if (stopIndex in directionData.stops.indices) stopIndex else null
            }

            // Rebuild localKeys whenever stops or destination change
            LaunchedEffect(directionData.stops, directionData.destination.latitude, directionData.destination.longitude, directionData.destination.name) {
                localKeys.clear()
                // use index-augmented keys to ensure uniqueness even for identical RoutePoint values
                directionData.stops.forEachIndexed { idx, stop ->
                    localKeys.add("${stop.latitude}_${stop.longitude}_${stop.name}_$idx")
                }
                localKeys.add(destinationKey)

                // Remove stale placeholder offsets for keys that no longer exist
                val stalePlaceholderKeys = placeholderOffsets.keys.filter { it !in localKeys }
                stalePlaceholderKeys.forEach { placeholderOffsets.remove(it) }

                // Clear drag state to avoid stale indices when underlying data changed
                clearDragState()
                swapInProgress = false
            }

            // Cancel active drag whenever external data changes mid-drag
            LaunchedEffect(directionData.stops.size, directionData.destination.latitude, directionData.destination.longitude, directionData.destination.name) {
                if (draggingKey != null) {
                    // external update happened while dragging -> cancel
                    clearDragState()
                }
            }

            /**
             * Optimized placeholder offset update: only update keys that are affected by the
             * insertion preview. Never clear the entire map mid-animation to avoid visual jumps.
             *
             * @param dragKey The key of the item being dragged
             * @param startIndex Original position in localKeys where drag started
             * @param computedTarget Current computed insertion position
             */
            fun updatePlaceholderOffsets(dragKey: String, startIndex: Int, computedTarget: Int) {
                if (startIndex < 0 || startIndex >= unifiedList.size) return
                if (computedTarget < 0 || computedTarget >= unifiedList.size) return

                // Always ensure dragged item has no placeholder offset
                placeholderOffsets.remove(dragKey)

                // If target hasn't changed from start, clear all placeholder offsets
                if (startIndex == computedTarget) {
                    // Clear all offsets since no items need to shift
                    placeholderOffsets.clear()
                    return
                }

                val movingDown = computedTarget > startIndex
                val affectedRange = if (movingDown) (startIndex + 1)..computedTarget else computedTarget until startIndex
                // Map indices to keys using localKeys (which mirrors unifiedList)
                val affectedKeys = affectedRange.mapNotNull { localKeys.getOrNull(it) }.filter { it != dragKey }

                // Compute desired offset: items shift opposite to drag direction
                val desiredOffset = if (movingDown) -itemHeightPx else itemHeightPx

                // Remove offsets for keys no longer affected (not in affected range and not being dragged)
                val toRemove = placeholderOffsets.keys.filter { it !in affectedKeys && it != dragKey }
                toRemove.forEach { placeholderOffsets.remove(it) }

                // Update or insert offsets for affected keys
                affectedKeys.forEach { key ->
                    if (placeholderOffsets[key] != desiredOffset) {
                        placeholderOffsets[key] = desiredOffset
                    }
                }
            }

            // Throttle/coordinate scrolling: only scroll when dragTargetIndex changes meaningfully
            // This prevents jitter from animateScrollToItem being called every drag update
            LaunchedEffect(listState) {
                snapshotFlow { dragTargetIndex }
                    .distinctUntilChanged()
                    .collectLatest { target ->
                        val t = target ?: return@collectLatest
                        if (t < 0 || t >= unifiedList.size) return@collectLatest

                        // Check if target is outside visible window
                        val visible = listState.layoutInfo.visibleItemsInfo
                        if (visible.isEmpty()) {
                            delay(50)
                            if (dragTargetIndex == t) {
                                listState.animateScrollToItem(t)
                            }
                            return@collectLatest
                        }

                        val firstVisible = visible.first().index
                        val lastVisible = visible.last().index

                        val shouldScroll = t < firstVisible || t > lastVisible

                        if (shouldScroll) {
                            // Debounce to avoid rapid scrolling on small movements
                            delay(50)
                            // Verify drag state hasn't changed during delay
                            if (dragTargetIndex == t) {
                                listState.animateScrollToItem(t)
                            }
                        }
                    }
            }

             LazyColumn(state = listState, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 // Origin
                 item {
                     // Origin row — show location icon in collapse (use LocationOn); don't auto-highlight.
                     StopPointRow(
                         point = directionData.origin,
                         label = "From",
                         showFullDetails = isExpanded,
                         // Only show swap affordance when expanded AND there are stops
                         showSwap = isExpanded && hasStops && !isOriginUserLocation,
                         onSwapHandleLongPress = {
                             if (swapInProgress) return@StopPointRow
                             swapInProgress = true
                             onSwapOriginDestination(); clearSwapSelection()
                         },
                         onRowClick = {},
                         isHighlighted = false,
                         // Force a location icon for the origin row (use the location pin icon)
                         isUserLocation = true
                     )
                 }

                 if (unifiedList.isNotEmpty()) {
                     if (!isExpanded) {
                         // Collapsed: show origin above (already shown), then either single stop inline or a small pill, then destination compact
                         item {
                             Column(modifier = Modifier.fillMaxWidth()) {
                                 when (directionData.stops.size) {
                                     0 -> { /* nothing between origin and destination */ }
                                     1 -> {
                                         val onlyStop = directionData.stops[0]
                                         StopPointRow(
                                             point = onlyStop,
                                             label = "Stop",
                                             showFullDetails = false,
                                             showSwap = false,
                                             onSwapHandleLongPress = {},
                                             onRowClick = {},
                                             isHighlighted = false,
                                             onRemove = null,
                                             isUserLocation = false,
                                             stopLetter = null,
                                             badgeOnly = true // collapsed: render colored badge with no number
                                         )
                                     }
                                     else -> {
                                         // Reuse StopPointRow to show a compact 'N stops' row with colored badge (no number)
                                         val count = directionData.stops.size
                                         val groupPoint = RoutePoint(0.0, 0.0, "${count} stops")
                                         StopPointRow(
                                             point = groupPoint,
                                             label = "Stops",
                                             showFullDetails = false,
                                             showSwap = false,
                                             onSwapHandleLongPress = {},
                                             onRowClick = {},
                                             isHighlighted = false,
                                             onRemove = null,
                                             isUserLocation = false,
                                             stopLetter = null,
                                             badgeOnly = true
                                         )
                                     }
                                 }
                                 Spacer(modifier = Modifier.height(8.dp))
                                 // Destination compact row
                                 StopPointRow(
                                     point = directionData.destination,
                                     label = "To",
                                     showFullDetails = false,
                                     showSwap = false,
                                     onSwapHandleLongPress = {},
                                     onRowClick = {},
                                     isHighlighted = false,
                                     onRemove = null
                                 )
                             }
                         }
                     } else {
                         itemsIndexed(unifiedList, key = { idx, p -> if (idx < directionData.stops.size) "${p.latitude}_${p.longitude}_${p.name}_$idx" else destinationKey }) { localIdx, point ->

                            // Use the itemsIndexed index directly as the canonical local index
                            val isDestination = localIdx == unifiedList.lastIndex
                            val itemKey = if (isDestination) destinationKey else "${point.latitude}_${point.longitude}_${point.name}_$localIdx"
                            val localIndex = localIdx

                             val isDragging = draggingKey == itemKey
                             val epsilon = 0.01f
                             val visualOffsetBase = (placeholderOffsets[itemKey] ?: 0f)
                             val visualOffsetPx = if (isDragging) draggedOffset else (visualOffsetBase + epsilon)

                             // If this is a stop (not destination), compute its letter badge (A, B, C...)
                            val stopLetter = if (localIndex < directionData.stops.size) {
                                (('A'.code + localIndex).toChar()).toString()
                            } else null

                            // Determine selection/highlight state: only highlight the dragged item and
                            // the current drag target. When not actively dragging, fall back to tap selection behavior.
                            val sel = swapSelection
                            val isSelected = isDragging || (sel != null && sel.localIndex == localIndex && sel.isDestination == isDestination)
                            val isCandidate = when {
                                // During active drag, highlight only the current drag target index
                                draggingKey != null -> (dragTargetIndex != null && dragTargetIndex == localIndex)
                                // When not dragging, retain previous tap-selection semantics (single candidate)
                                sel != null && !sel.isDestination && isDestination -> true
                                sel != null && sel.isDestination && !isDestination && sel.localIndex == localIndex -> false
                                sel != null && !sel.isDestination && !isDestination -> sel.localIndex != localIndex
                                else -> false
                            }

                             StopPointRow(
                                  point = point,
                                  label = if (isDestination) "To" else "Stop ${localIdx + 1}",
                                  showFullDetails = isExpanded,
                                  // only show swap affordance when there are stops to swap with
                                  showSwap = isExpanded && hasStops,
                                  isUserLocation = false,
                                  stopLetter = stopLetter,
                                  onSwapHandleLongPress = {
                                      if (swapInProgress) return@StopPointRow
                                      // Start dragging this item (stop or destination)
                                      draggingKey = itemKey
                                      swapSelection = SwapSelection(isDestination = isDestination, localIndex = localIndex)
                                      dragStartIndex = localIndex
                                      dragTargetIndex = localIndex
                                      lastAppliedTarget = localIndex
                                      draggedOffset = 0f
                                      placeholderOffsets.remove(itemKey)
                                  },
                                 onRowClick = {
                                     val sel = swapSelection
                                     if (sel != null) {
                                         when {
                                             sel.isDestination && !isDestination -> {
                                                 // Destination was previously selected -> swap dest with this stop
                                                 val targetStopLocal = localIndex
                                                 val targetStopIndex = localKeyIndexToStopIndex(targetStopLocal)
                                                 if (targetStopIndex != null && !swapInProgress) {
                                                     swapInProgress = true
                                                     onSwapStopWithDestination(targetStopIndex)
                                                 }
                                             }
                                             !sel.isDestination && isDestination -> {
                                                 // A stop was selected earlier -> swap that stop with destination
                                                 val fromLocal = sel.localIndex
                                                 val fromStopIndex = localKeyIndexToStopIndex(fromLocal)
                                                 if (fromStopIndex != null && !swapInProgress) {
                                                     swapInProgress = true
                                                     onSwapStopWithDestination(fromStopIndex)
                                                 }
                                             }
                                             !sel.isDestination && !isDestination -> {
                                                 // stop-stop swap
                                                 val fromLocal = sel.localIndex
                                                 val toLocal = localIndex
                                                 val fromStopIndex = localKeyIndexToStopIndex(fromLocal)
                                                 val toStopIndex = localKeyIndexToStopIndex(toLocal)
                                                 if (fromStopIndex != null && toStopIndex != null && !swapInProgress) {
                                                     swapInProgress = true
                                                     onSwapStops(fromStopIndex, toStopIndex)
                                                 }
                                             }
                                             else -> { /* sel.isDestination && isDestination -> no-op */ }
                                         }
                                         coroutineScope.launch { listState.animateScrollToItem(localIndex.coerceAtLeast(0)) }
                                         clearSwapSelection()
                                     }
                                 },
                                 onRemove = if (!isDestination) {
                                     {
                                         val stopIdx = localKeyIndexToStopIndex(localIndex) ?: localIdx
                                         onRemoveStop(stopIdx)
                                     }
                                 } else null,
                                 isHighlighted = isSelected || isCandidate,
                                 modifier = Modifier,
                                 offsetPx = visualOffsetPx,
                                 isDragging = isDragging,
                                 onDragDelta = { _, dyPx, dragState ->
                                     val key = itemKey
                                     if (dragState == 0) {
                                         // accumulating drag offset
                                         val newCum = draggedOffset + dyPx
                                         draggedOffset = newCum

                                        val startIdx = dragStartIndex ?: localIndex
                                        if (startIdx < 0) return@StopPointRow

                                        // Compute target based on cumulative offset
                                        val floatTarget = startIdx + (newCum / itemHeightPx)
                                        val computedTarget = floatTarget.roundToInt().coerceIn(0, unifiedList.lastIndex)

                                        // Only update placeholder offsets when the integer target changes.
                                        if (computedTarget != lastAppliedTarget) {
                                            dragTargetIndex = computedTarget
                                            lastAppliedTarget = computedTarget
                                            updatePlaceholderOffsets(key, startIdx, computedTarget)
                                        }

                                     } else {
                                         // drag ended
                                        val startIdx = dragStartIndex ?: localIndex
                                        val finalTarget = dragTargetIndex ?: startIdx

                                        if (startIdx >= 0 && finalTarget >= 0 && finalTarget != startIdx) {
                                            // Cases to handle:
                                            // - stop dragged to destination (finalTarget == lastIndex && startIdx is stop)
                                            // - destination dragged to stop (startIdx == lastIndex && finalTarget is stop)
                                            when {
                                                // stop -> destination
                                                finalTarget == unifiedList.lastIndex && startIdx < directionData.stops.size -> {
                                                    val startStopIndex = localKeyIndexToStopIndex(startIdx)
                                                    if (startStopIndex != null && !swapInProgress) {
                                                        swapInProgress = true
                                                        onSwapStopWithDestination(startStopIndex)
                                                    }
                                                }
                                                // destination -> stop
                                                startIdx == unifiedList.lastIndex && finalTarget < directionData.stops.size -> {
                                                    // swap destination with the stop at finalTarget
                                                    val targetStopIndex = localKeyIndexToStopIndex(finalTarget)
                                                    if (targetStopIndex != null && !swapInProgress) {
                                                        swapInProgress = true
                                                        onSwapStopWithDestination(targetStopIndex)
                                                    }
                                                }
                                                // stop <-> stop
                                                startIdx < directionData.stops.size && finalTarget < directionData.stops.size -> {
                                                    val fromStopIndex = localKeyIndexToStopIndex(startIdx)
                                                    val toStopIndex = localKeyIndexToStopIndex(finalTarget)
                                                    if (fromStopIndex != null && toStopIndex != null && !swapInProgress) {
                                                        swapInProgress = true
                                                        onSwapStops(fromStopIndex, toStopIndex)
                                                    }
                                                }
                                                else -> {
                                                    // no-op for other cases (e.g., origin or invalid indices)
                                                }
                                            }
                                        }

                                        // Clear drag state
                                        clearDragState()
                                     }
                                 }
                             )
                         }
                     }
                 } else if (isEditingStops) {
                     item { AddStopPlaceholder(onClick = onAddStop) }
                 }

                // Removed duplicate AddStopPlaceholder at the bottom — header Add remains sufficient.
             }
         }
     }
 }

/**
 * A row representing an origin/stop/destination entry. The offset is animated so that placeholder
 * offsets (set while another item is dragged) produce a smooth visual swap preview.
 */
@Composable
private fun StopPointRow(
    point: RoutePoint,
    label: String,
    showFullDetails: Boolean,
    showSwap: Boolean,
    onSwapHandleLongPress: () -> Unit,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onDragDelta: ((Float, Float, Int) -> Unit)? = null,
    offsetPx: Float = 0f,
    isDragging: Boolean = false,
    isUserLocation: Boolean = false,
    stopLetter: String? = null,
    badgeOnly: Boolean = false // new flag for collapsed badge-only rendering
) {
    // For dragged items: use offset directly (immediate response)
    // For other items: animate the offset smoothly
    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) 0f else offsetPx,
        label = "stopRowOffset"
    )

    val actualOffset = if (isDragging) offsetPx else animatedOffset

    // Row container - drag gestures are attached only to the swap icon below
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, actualOffset.roundToInt()) }
            .then(if (isDragging) Modifier.scale(1.03f).zIndex(6f) else Modifier)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).animateContentSize().then(if (showSwap) Modifier.clickable { onRowClick() } else Modifier),
            tonalElevation = when { isDragging -> 12.dp; isHighlighted -> 8.dp; else -> 0.dp },
            shadowElevation = when { isDragging -> 8.dp; isHighlighted -> 4.dp; else -> 0.dp }
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (badgeOnly) {
                    // Collapsed: always render a filled primary-colored circular badge (no number)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                 } else if (stopLetter != null) {
                    // Minimal circular badge with letter (A, B, C...) — use primary for badge and onPrimary for text for stronger contrast
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stopLetter, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                 } else if (isUserLocation) {
                     // Use MyLocation (crosshair) icon for the user's origin to visually distinguish it
                     Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Your location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                 } else {
                     Icon(imageVector = Icons.Default.Place, contentDescription = label, tint = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                 }
                Column(modifier = Modifier.weight(1f)) {
                    if (showFullDetails) Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = point.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (showFullDetails) FontWeight.SemiBold else FontWeight.Medium, color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = if (showFullDetails) 2 else 1, overflow = TextOverflow.Ellipsis)
                    if (showFullDetails && point.address.isNotEmpty()) Text(text = point.address, style = MaterialTheme.typography.bodySmall, color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (showSwap) {
                    // Swap affordance: attach drag gesture here so dragging only starts when interacting with this icon
                    val swapIconModifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)

                    val swapWithDragModifier = if (onDragDelta != null) {
                        swapIconModifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    onSwapHandleLongPress(); onDragDelta.invoke(0f, 0f, 0)
                                },
                                onDrag = { change, dragAmount -> onDragDelta.invoke(dragAmount.x, dragAmount.y, 0); change.consume() },
                                onDragEnd = { onDragDelta.invoke(0f, 0f, 1) },
                                onDragCancel = { onDragDelta.invoke(0f, 0f, 1) }
                            )
                        }
                    } else swapIconModifier

                    Box(modifier = swapWithDragModifier, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Long press & drag to swap", tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
                onRemove?.let { IconButton(onClick = it, modifier = Modifier.size(36.dp)) { Icon(imageVector = Icons.Default.Close, contentDescription = "Remove stop", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) } }
            }
        }
    }


}

@Composable
private fun AddStopPlaceholder(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add stop", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add stop")
    }
}
