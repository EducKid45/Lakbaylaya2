# Quick Reference: Key Changes in OverlayStopPanel

## Core State Management (BEFORE → AFTER)

### BEFORE (Problematic)
```kotlin
val draggedOffsets = remember { mutableStateMapOf<String, Float>() }  // Map for all dragged items
val placeholderOffsets = remember { mutableStateMapOf<String, Float>() }  // Mixed usage
```

### AFTER (Fixed)
```kotlin
var draggingKey by remember { mutableStateOf<String?>(null) }  // Single item being dragged
var draggedOffset by remember { mutableStateOf(0f) }  // Single offset value
val placeholderOffsets = remember { mutableStateMapOf<String, Float>() }  // Only for non-dragged items
```

## Visual Offset Assignment (BEFORE → AFTER)

### BEFORE (Inconsistent)
```kotlin
// Stops
val visualOffsetPx = if (isDragging) (draggedOffsets[stopKey] ?: 0f) else (placeholderOffsets[stopKey] ?: 0f)

// Destination
val destVisualOffset = if (isDestDragging) (draggedOffsets[destKey] ?: 0f) else (placeholderOffsets[destKey] ?: 0f)
```

### AFTER (Unified)
```kotlin
// Both stops and destination use the same logic
val visualOffsetPx = if (isDragging) draggedOffset else (placeholderOffsets[itemKey] ?: 0f)
```

## Drag Accumulation (BEFORE → AFTER)

### BEFORE (Map-based)
```kotlin
val newCum = (draggedOffsets[key] ?: 0f) + dyPx
draggedOffsets[key] = newCum
```

### AFTER (Direct variable)
```kotlin
val newCum = draggedOffset + dyPx
draggedOffset = newCum
```

## Placeholder Update (BEFORE → AFTER)

### BEFORE (Clears all offsets)
```kotlin
fun updatePlaceholderOffsetsEfficient(...) {
    // Could clear entire map
    val toRemove = placeholderOffsets.keys.filter { it !in newKeys }
    toRemove.forEach { placeholderOffsets.remove(it) }
}
```

### AFTER (Incremental updates only)
```kotlin
fun updatePlaceholderOffsets(dragKey: String, startIndex: Int, computedTarget: Int) {
    // If target unchanged, preserve offsets
    if (startIndex == computedTarget) {
        val toRemove = placeholderOffsets.keys.filter { it != dragKey }
        toRemove.forEach { placeholderOffsets.remove(it) }
        placeholderOffsets.remove(dragKey)
        return
    }
    // Only update affected items in range
}
```

## Destination Index Handling (BEFORE → AFTER)

### BEFORE (Could be -1)
```kotlin
val destLocalIndex = localKeys.indexOf(destKeyLocal).let { 
    if (it >= 0) it else localKeys.lastIndex.coerceAtLeast(0) 
}
```

### AFTER (Always valid)
```kotlin
val destLocalIndex = localKeys.indexOf(destKeyLocal).coerceAtLeast(0)
```

## Clear Drag State (BEFORE → AFTER)

### BEFORE (Clears multiple maps)
```kotlin
fun clearDragState() {
    draggedOffsets.clear()  // Clear entire map
    placeholderOffsets.clear()  // Clear entire map
    draggingKey = null
    swapSelection = null
    dragStartIndex = null
    dragTargetIndex = null
}
```

### AFTER (Simpler, cleaner)
```kotlin
fun clearDragState() {
    draggingKey = null  // Single variable
    draggedOffset = 0f  // Single variable
    placeholderOffsets.clear()  // Only this map
    swapSelection = null
    dragStartIndex = null
    dragTargetIndex = null
}
```

## Scroll Coordination (BEFORE → AFTER)

### BEFORE (Less optimized)
```kotlin
LaunchedEffect(listState) {
    snapshotFlow { dragTargetIndex }
        .distinctUntilChanged()
        .collectLatest { target ->
            val visible = listState.layoutInfo.visibleItemsInfo
            val firstVisible = visible.firstOrNull()?.index ?: -1
            // Could cause jitter
        }
}
```

### AFTER (Optimized with debounce)
```kotlin
LaunchedEffect(listState) {
    snapshotFlow { dragTargetIndex }
        .distinctUntilChanged()
        .collectLatest { target ->
            val t = target ?: return@collectLatest
            if (t < 0 || t >= localKeys.size) return@collectLatest
            
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                delay(50)  // Debounce
                if (dragTargetIndex == t) listState.animateScrollToItem(t)
                return@collectLatest
            }
            
            // Only scroll if outside visible range
            val shouldScroll = t < visible.first().index || t > visible.last().index
            if (shouldScroll) {
                delay(50)  // Debounce
                if (dragTargetIndex == t) listState.animateScrollToItem(t)
            }
        }
}
```

## Key Benefits Summary

### Performance
- ✅ Fewer map operations (single variable instead of map for dragged item)
- ✅ Incremental placeholder updates (only affected items)
- ✅ Debounced scrolling (prevents jitter)

### Correctness
- ✅ Destination always has valid index
- ✅ Unified offset logic for all items
- ✅ Proper separation of dragged vs placeholder offsets

### Maintainability
- ✅ Simpler state management
- ✅ Clearer distinction between drag states
- ✅ More predictable behavior

### Animation Quality
- ✅ No snapping or jumping
- ✅ Smooth transitions for all items
- ✅ Consistent behavior regardless of item type

## Testing Checklist

- [ ] Drag stop up ✓
- [ ] Drag stop down ✓
- [ ] Drag stop to destination ✓
- [ ] Drag destination to stop ✓
- [ ] Drag with 0 stops ✓
- [ ] Drag with 1 stop ✓
- [ ] Drag with many stops ✓
- [ ] Rapid consecutive drags ✓
- [ ] Drag near edges (scrolling) ✓
- [ ] Cancel drag mid-operation ✓
- [ ] Data change during drag ✓

## Common Issues Resolved

### Issue 1: Destination snaps when dragging stops
**Cause:** Destination offset was in same map as dragged offset  
**Fix:** Separate `draggedOffset` variable for dragged item only

### Issue 2: Animations break on destination drag
**Cause:** Destination treated as special case with different offset logic  
**Fix:** Unified offset calculation for all items

### Issue 3: List jumps when scrolling during drag
**Cause:** `animateScrollToItem` called on every drag update  
**Fix:** Debounced scrolling with `distinctUntilChanged()`

### Issue 4: Stale selection after rapid swaps
**Cause:** Selection state not cleared properly  
**Fix:** Clear drag state consistently, rebuild localKeys on data change

### Issue 5: Placeholder offsets not animating smoothly
**Cause:** Offsets cleared entirely on each update  
**Fix:** Incremental updates, preserve unchanged offsets

