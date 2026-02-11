# Visual Flow Diagram: Drag-and-Drop Logic

## State Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DRAG STATE MANAGEMENT                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  draggingKey: String?  ──────────► "Which item is moving"   │
│       │                                                       │
│       ├─ null: No drag in progress                          │
│       └─ "lat_lon_name_idx": Item being dragged             │
│                                                               │
│  draggedOffset: Float  ──────────► "How far dragged"        │
│       │                                                       │
│       ├─ Updated on each onDrag event                       │
│       └─ Reset to 0f on drag start                          │
│                                                               │
│  placeholderOffsets: Map<String, Float> ─► "Preview shifts" │
│       │                                                       │
│       ├─ Key: item key                                       │
│       ├─ Value: +/- itemHeightPx                            │
│       └─ Items NOT being dragged                            │
│                                                               │
│  dragStartIndex: Int? ────────────► "Original position"     │
│  dragTargetIndex: Int? ───────────► "Computed position"     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Visual Offset Assignment

```
For EACH item in LazyColumn:
┌─────────────────────────────────────────────┐
│ Is this item being dragged?                  │
└──────────────┬──────────────┬────────────────┘
               │              │
             YES             NO
               │              │
               ▼              ▼
    ┌──────────────┐   ┌──────────────────────┐
    │ Use:         │   │ Use:                 │
    │ draggedOffset│   │ placeholderOffsets[] │
    │ (immediate)  │   │ (animated)           │
    └──────┬───────┘   └──────┬───────────────┘
           │                  │
           └────────┬─────────┘
                    ▼
         ┌─────────────────────────┐
         │ animateFloatAsState()   │
         │ ▼                       │
         │ IntOffset(0, y)         │
         └─────────────────────────┘
```

## Drag Lifecycle

### Phase 1: Drag Start (onSwapHandleLongPress)
```
User long-presses swap icon
         │
         ▼
┌─────────────────────────┐
│ Set draggingKey         │
│ Set swapSelection       │
│ Set dragStartIndex      │
│ Set dragTargetIndex     │
│ Set draggedOffset = 0f  │
└─────────────────────────┘
         │
         ▼
┌─────────────────────────┐
│ Item scales to 1.03x    │
│ zIndex set to 6f        │
│ Shadow elevation 8.dp   │
└─────────────────────────┘
```

### Phase 2: During Drag (onDrag)
```
User drags finger (dy pixels)
         │
         ▼
┌──────────────────────────────┐
│ draggedOffset += dy          │
└──────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ floatTarget = startIdx +     │
│   (draggedOffset / itemH)    │
│ computedTarget =             │
│   floatTarget.roundToInt()   │
│   .coerceIn(0, lastIndex)    │
└──────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ dragTargetIndex =            │
│   computedTarget             │
└──────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ updatePlaceholderOffsets()   │
│   - Compute affected range   │
│   - Set +/- itemHeightPx     │
│   - Remove unaffected items  │
│   - Never clear entire map   │
└──────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ snapshotFlow monitors        │
│ dragTargetIndex changes      │
│   ▼                          │
│ Debounce 50ms                │
│   ▼                          │
│ animateScrollToItem(target)  │
└──────────────────────────────┘
```

### Phase 3: Drag End (onDragEnd / onDragCancel)
```
User releases finger
         │
         ▼
┌─────────────────────────────────┐
│ finalTarget = dragTargetIndex   │
│ startIdx = dragStartIndex       │
└─────────────────────────────────┘
         │
         ▼
     ┌───┴───┐
     │ Same? │
     └───┬───┘
       NO│   │YES
         │   └────────► clearDragState()
         ▼
┌──────────────────────────────────┐
│ Is finalTarget == lastIndex?     │
│ (destination position)           │
└──────────────────────────────────┘
       YES│   │NO
         │   │
         │   └────► Both are stop positions
         │          ▼
         │   ┌──────────────────────────┐
         │   │ onSwapStops(from, to)    │
         │   └──────────────────────────┘
         │
         └────► Stop ↔ Destination swap
                ▼
         ┌──────────────────────────────┐
         │ onSwapStopWithDestination()  │
         └──────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────┐
         │ swapInProgress = true        │
         └──────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────┐
         │ clearDragState()             │
         │   - draggingKey = null       │
         │   - draggedOffset = 0f       │
         │   - placeholderOffsets.clear │
         │   - swapSelection = null     │
         │   - dragStartIndex = null    │
         │   - dragTargetIndex = null   │
         └──────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────┐
         │ Data update triggers         │
         │ LaunchedEffect               │
         └──────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────┐
         │ localKeys rebuilt            │
         │ swapInProgress = false       │
         └──────────────────────────────┘
```

## Placeholder Offset Logic

### Moving DOWN (target > start)
```
Before:  [A] [B] [C] [D] [E]
                  ^dragging
         
         startIndex = 2
         targetIndex = 4
         
Affected: items 3, 4 (between start+1 and target)

Offsets:  [A]   [B]   [C]   [D]     [E]
          0px   0px   +dy   -1h     -1h
                            ▲       ▲
                            shift up

After:   [A] [B] [D] [E] [C]
```

### Moving UP (target < start)
```
Before:  [A] [B] [C] [D] [E]
                        ^dragging
         
         startIndex = 4
         targetIndex = 1
         
Affected: items 1, 2, 3 (between target and start-1)

Offsets:  [A]   [B]     [C]     [D]     [E]
          0px   +1h     +1h     +1h     +dy
                ▼       ▼       ▼
                shift down

After:   [A] [E] [B] [C] [D]
```

## localKeys Structure

```
directionData.stops = [Stop1, Stop2, Stop3]
directionData.destination = Dest

                    ▼

localKeys = [
  "lat1_lon1_Stop1_0",    ◄── stopIndex 0
  "lat2_lon2_Stop2_1",    ◄── stopIndex 1
  "lat3_lon3_Stop3_2",    ◄── stopIndex 2
  "dest_latD_lonD_Dest"   ◄── destination (lastIndex)
]

Mapping:
  localIndex 0 → stopIndex 0
  localIndex 1 → stopIndex 1
  localIndex 2 → stopIndex 2
  localIndex 3 → destination (not a stop)
```

## Animation Timeline

```
Frame 1: Drag starts
┌────────────────────────────────────────┐
│ Item A: offsetPx = 0     (dragged)     │
│ Item B: offsetPx = 0     (not affected)│
│ Item C: offsetPx = 0     (not affected)│
└────────────────────────────────────────┘

Frame 10: Dragging down +100px
┌────────────────────────────────────────┐
│ Item A: offsetPx = +100  (dragged)     │
│ Item B: offsetPx → -72   (animating)   │
│ Item C: offsetPx = 0     (not affected)│
└────────────────────────────────────────┘
         ▲
         animateFloatAsState smoothly
         transitions from 0 to -72

Frame 20: Continue dragging +150px
┌────────────────────────────────────────┐
│ Item A: offsetPx = +150  (dragged)     │
│ Item B: offsetPx → -72   (stable)      │
│ Item C: offsetPx → -72   (animating)   │
└────────────────────────────────────────┘
         ▲
         B finished animating
         C now animating

Frame 30: Drag end
┌────────────────────────────────────────┐
│ All offsets → 0 (animating)            │
│ Data updates (swap occurred)           │
│ Items render in new order              │
└────────────────────────────────────────┘
```

## Key Invariants

### ✅ Always True
1. `draggingKey` is null OR matches exactly one item key
2. `draggedOffset` is only used by the item with `key == draggingKey`
3. `placeholderOffsets[key]` is never set for the dragged item
4. `localKeys.lastIndex` always corresponds to destination
5. `dragTargetIndex` is always clamped to `[0, localKeys.lastIndex]`

### ✅ Never True
1. Multiple items have `isDragging = true`
2. Dragged item has a placeholder offset
3. `dragTargetIndex` is negative or > `localKeys.size`
4. `placeholderOffsets` is cleared mid-animation (only at drag end)
5. Destination has index -1

## Error Prevention

### Problem: Destination snaps during stop drag
**Root Cause:** Destination key was in placeholderOffsets map  
**Prevention:** `updatePlaceholderOffsets()` explicitly removes dragKey  
**Verification:** `placeholderOffsets.remove(dragKey)`

### Problem: Animations break on data change
**Root Cause:** Stale keys after stops list updates  
**Prevention:** `LaunchedEffect` rebuilds localKeys and clears drag state  
**Verification:** Check `directionData.stops` dependencies

### Problem: List jumps during scroll
**Root Cause:** `animateScrollToItem` called every frame  
**Prevention:** `snapshotFlow` with `distinctUntilChanged()` and debounce  
**Verification:** Scroll only when target changes and is out of view

### Problem: Rapid swaps cause index mismatch
**Root Cause:** Multiple swaps trigger before data updates  
**Prevention:** `swapInProgress` flag blocks new swaps  
**Verification:** Reset flag when `localKeys` rebuilds

