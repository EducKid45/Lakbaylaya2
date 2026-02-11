# Bug Fix: Second Destination Drag Animation Not Moving

## Problem Description
When dragging the destination for the first time, the animation worked correctly. However, on the second attempt to drag the destination, it would not animate smoothly even though the swap logic still functioned correctly.

## Root Cause
The issue was in the `StopPointRow` composable's animation logic. The problem had two parts:

### Part 1: Animation State Persistence
The `animateFloatAsState` was animating ALL offsets, including the dragged item's offset. When a drag operation completed and the destination animated back to position 0, if a second drag started before the animation finished, the animation state would still contain the old value.

### Part 2: Animation Target Confusion
The original code had:
```kotlin
val animatedOffset by animateFloatAsState(targetValue = offsetPx)

Box(modifier = modifier.offset { IntOffset(0, animatedOffset.roundToInt()) })
```

This meant that even when `isDragging = true`, the dragged item was still using the animated value, which would lag behind the actual touch position and cause visual stuttering on the second drag.

## Solution

### Change 1: Separate Dragged vs Placeholder Animation Logic
Modified the `StopPointRow` to treat dragged items and placeholder items differently:

```kotlin
// For dragged items: use offset directly (immediate response)
// For other items: animate the offset smoothly
val animatedOffset by animateFloatAsState(
    targetValue = if (isDragging) 0f else offsetPx,
    label = "stopRowOffset"
)

val actualOffset = if (isDragging) offsetPx else animatedOffset
```

**Key insight:** When an item is being dragged, we want immediate visual feedback following the finger. Only non-dragged items should have smooth animations for the placeholder preview effect.

### Change 2: Use Actual Offset for Rendering
```kotlin
Box(modifier = modifier.offset { IntOffset(0, actualOffset.roundToInt()) })
```

Now:
- **Dragged item**: Uses `offsetPx` directly → no animation lag, immediate response
- **Other items**: Use `animatedOffset` → smooth sliding animation for placeholder preview

### Change 3: Clear Placeholder Offsets on Drag Start
Updated `updatePlaceholderOffsets` to always clear all offsets when `startIndex == computedTarget`:
```kotlin
if (startIndex == computedTarget) {
    // Clear all offsets since no items need to shift
    placeholderOffsets.clear()
    return
}
```

This ensures that when a new drag starts (target equals start initially), any stale offsets from previous operations are removed.

### Change 4: Clean Stale Offsets on Data Update
Added cleanup in the `LaunchedEffect` that rebuilds `localKeys`:
```kotlin
// Remove stale placeholder offsets for keys that no longer exist
val stalePlaceholderKeys = placeholderOffsets.keys.filter { it !in localKeys }
stalePlaceholderKeys.forEach { placeholderOffsets.remove(it) }
```

This ensures that after a swap operation, when keys get regenerated, any offsets for old keys are removed.

## How It Works Now

### First Drag (Stop to Destination)
1. User starts drag on Stop → `isDragging = true`, `offsetPx` starts updating
2. Stop follows finger immediately (no animation)
3. Destination gets placeholder offset → animates smoothly upward
4. On drag end → swap occurs, data updates, all offsets cleared

### Second Drag (Destination)
1. Data has updated, destination is now in new position
2. All stale offsets were cleared by `clearDragState()` and stale key cleanup
3. User starts drag on Destination → `isDragging = true`, `offsetPx = 0f`
4. `animateFloatAsState` targets `0f` (because `isDragging = true`)
5. `actualOffset = offsetPx` (dragged item uses direct offset)
6. Destination follows finger immediately ✓
7. Stops get placeholder offsets → animate smoothly ✓

## Key Differences: Before vs After

### Before (Buggy)
```kotlin
// Everything animated
val animatedOffset by animateFloatAsState(targetValue = offsetPx)
// Dragged item lagged behind finger on second drag
```

### After (Fixed)
```kotlin
// Only non-dragged items animated
val animatedOffset by animateFloatAsState(
    targetValue = if (isDragging) 0f else offsetPx
)
val actualOffset = if (isDragging) offsetPx else animatedOffset
// Dragged item always immediate, others smooth
```

## Testing Results

### Expected Behavior (Now Working)
- ✅ First drag of destination: smooth animation
- ✅ Second drag of destination: smooth animation
- ✅ Third+ drags: smooth animation
- ✅ Rapid consecutive drags: no stuttering
- ✅ Drag any stop multiple times: smooth
- ✅ Placeholder animations: smooth for all non-dragged items

### Previously Broken
- ❌ Second drag of destination: item didn't move smoothly
- ❌ Animation state carried over from first drag

## Code Changes Summary

### File: `OverlayStopPanel.kt`

**Lines 550-575** (StopPointRow animation logic):
- Split animation path for dragged vs non-dragged items
- Dragged item uses direct offset for immediate response
- Non-dragged items use animated offset for smooth preview

**Lines 218-230** (updatePlaceholderOffsets):
- Simplified logic to always clear all offsets when no items need shifting
- Ensures no stale offsets remain from previous drag operations

**Lines 192-207** (localKeys rebuild LaunchedEffect):
- Added cleanup for stale placeholder offsets after data updates
- Ensures offsets for removed/changed keys are cleared

## Why This Fix Works

The fundamental principle is:

> **Dragged items must respond immediately to touch input. Only non-dragged items should animate.**

By separating these two concerns:
1. The dragged item (whether stop or destination) always follows the finger precisely
2. Other items smoothly animate their placeholder positions
3. No animation state persists between drag operations
4. Each drag starts with a clean slate

This ensures that regardless of how many times you drag the destination (or any stop), the behavior is consistent and smooth.

## Prevention of Future Issues

The fix includes several defensive measures:
1. **Stale offset cleanup** when localKeys rebuilds
2. **Clear all offsets** when drag starts (target == start)
3. **Separate code paths** for dragged vs animated items
4. **Explicit offset removal** for dragged item in updatePlaceholderOffsets

These measures ensure that even if some edge case causes an offset to persist, it will be cleaned up before the next drag operation.

