# Routing & UI Fixes Summary

## Date: February 11, 2026

## Issues Fixed

### 1. ✅ Dotted Lines for All Route Segments
**Problem:** When using multiple stops, some polylines were rendering as solid lines instead of dotted.

**Fix Applied:**
- Modified `MapScreen.kt` line ~201: Changed `isPrimary = polylineData.isPrimary` to `isPrimary = false`
- This ensures ALL route segments always render as dotted lines regardless of stop count
- The `isPrimary = false` parameter forces dotted line style in `MapLibreManager.drawPolyline()`

**Location:** `app/src/main/java/com/example/lakbaylaya/ui/screens/map/MapScreen.kt`

---

### 2. ✅ X-in-Circle Button for Collapsed Direction Sheet
**Problem:** Collapsed direction bottom sheet was missing the X button with circular background.

**Fix Applied:**
- Modified `DirectionBottomSheet.kt` line ~356-368
- Replaced animated transparent background with always-visible `MaterialTheme.colorScheme.surfaceVariant` background
- X button now displays inside a visible circle in both collapsed and expanded states

**Location:** `app/src/main/java/com/example/lakbaylaya/ui/screens/map/components/DirectionBottomSheet.kt`

---

### 3. ✅ Stop Marker Labels Not Rendering
**Problem:** Stop markers (A, B, C...) were not showing text inside the circles on the map.

**Fix Applied:**
- Modified `MapLibreManager.kt` line ~188-203
- Added `PropertyFactory.textAnchor("center")` to center text
- Added `PropertyFactory.textOffset(arrayOf(0f, 0f))` for precise positioning
- Increased `textSize` from 12f to 14f for better visibility
- These properties ensure the SymbolLayer text renders centered inside the circular stop markers

**Location:** `app/src/main/java/com/example/lakbaylaya/ui/screens/map/maplibre/MapLibreManager.kt`

---

### 4. ✅ Current Route Segment Highlighting
**Problem:** Need to visually distinguish which route segment is currently active (the next leg to travel).

**Fix Applied:**
- Modified `MapScreen.kt` line ~193-203
- First segment (index 0) when stops exist: renders in green (#4CAF50) - current route to travel
- Remaining segments when stops exist: render in gray (#9E9E9E) - future legs
- Single route without stops: uses default color
- All segments always render as dotted lines

**Location:** `app/src/main/java/com/example/lakbaylaya/ui/screens/map/MapScreen.kt`

---

### 5. ✅ Destination Marker Rendering
**Problem:** Destination was being rendered as a stop circle marker instead of a standard pin.

**Fix Applied:**
- Modified `MapScreen.kt` line ~218-235
- Origin + stops: rendered as white circles with A/B/C labels using `drawStopMarkers()`
- Destination: rendered separately as a standard marker pin using `addMarker()`
- This creates clear visual distinction between waypoints and final destination

**Location:** `app/src/main/java/com/example/lakbaylaya/ui/screens/map/MapScreen.kt`

---

## Technical Details

### Color Scheme for Route Segments
- **Current/First Segment:** `#4CAF50` (Green) - Indicates the active route leg user should follow
- **Future Segments:** `#9E9E9E` (Gray) - Indicates upcoming route legs after reaching stops
- **Single Route:** Default color (Blue `#2196F3`)

### Stop Marker Configuration
- **Circle Radius:** 10dp
- **Circle Color:** White (#FFFFFF)
- **Stroke Width:** 2dp
- **Stroke Color:** Blue (#2196F3)
- **Label Text Size:** 14f (increased from 12f)
- **Label Position:** Centered with textAnchor("center") and textOffset([0,0])

### Polyline Configuration
- **All segments:** Dotted style (lineDasharray with [2f, 2f] or [1f, 2f])
- **Line Width:** 5f for primary, 4f for alternatives
- **Line Caps:** Round
- **Line Joins:** Round

---

## Testing Instructions

1. **Test Dotted Lines:**
   - Create a route with 2+ stops
   - Verify ALL polyline segments appear dotted (no solid lines)
   - Verify first segment is green, remaining are gray

2. **Test Stop Labels:**
   - Add multiple stops to a route
   - Verify white circles appear at each stop location
   - Verify A, B, C, etc. letters appear centered inside each circle
   - Verify destination shows a pin marker (not a circle)

3. **Test Direction Sheet:**
   - Start directions in collapsed state
   - Verify X button appears inside a visible circle at top-right
   - Click X to close directions
   - Expand sheet - verify X button still visible

4. **Test Route Highlighting:**
   - Create route: Origin → Stop 1 → Stop 2 → Destination
   - Verify first polyline segment (Origin → Stop 1) is green and dotted
   - Verify remaining segments (Stop 1 → Stop 2, Stop 2 → Destination) are gray and dotted

---

## Compilation Status

✅ **No Compile Errors**
⚠️ **Warnings Only** (non-blocking style suggestions):
- KTX color extension suggestions
- Unused helper functions (kept for future features)
- Parameter ordering conventions

---

## Files Modified

1. `MapLibreManager.kt` - Stop label rendering configuration
2. `MapScreen.kt` - Polyline drawing and color logic
3. `DirectionBottomSheet.kt` - X button styling

---

## Related Documentation

- See `REAL_ROUTING_API_IMPLEMENTATION.md` for routing logic overview
- See `DRAG_REDESIGN_SUMMARY.md` for UI component architecture
- See `ROUTING_API_INTEGRATION.md` for API integration details

