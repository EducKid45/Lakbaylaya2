package com.example.lakbaylaya.ui.screens.map.components.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.example.lakbaylaya.ui.screens.map.models.BottomSheetState
import kotlin.math.abs

/**
 * Creates a nested scroll connection for handling drag gestures on bottom sheet
 * Integrates with BottomSheetController for smooth physics
 */
@Composable
fun rememberBottomSheetNestedScrollConnection(
    sheetState: BottomSheetState,
    controller: BottomSheetController,
    onStateChange: (BottomSheetState) -> Unit
): NestedScrollConnection {
    return remember(sheetState, controller) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isFullState = sheetState is BottomSheetState.Full
                val isHiddenState = sheetState is BottomSheetState.Hidden
                val isInitialState = sheetState is BottomSheetState.Initial
                val isScrollingDown = available.y > 0
                val isScrollingUp = available.y < 0
                val isHorizontalScroll = abs(available.x) > abs(available.y)

                return when {
                    // Hidden state: Allow drag up to expand
                    isHiddenState && isScrollingUp -> {
                        controller.dragBy(available.y)
                        available
                    }
                    // Initial state: Only intercept vertical drags, allow horizontal scrolling
                    isInitialState && !isHorizontalScroll -> {
                        controller.dragBy(available.y)
                        available
                    }
                    // Full state: Only intercept downward scroll (collapse gesture)
                    // Let LazyColumn handle upward scroll for content scrolling
                    isFullState && isScrollingDown -> {
                        controller.dragBy(available.y)
                        available
                    }
                    // Half state: intercept all scrolling for state transitions
                    !isFullState && !isHiddenState && !isInitialState -> {
                        controller.dragBy(available.y)
                        available
                    }
                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val isFullState = sheetState is BottomSheetState.Full
                val hasUnconsumedDownwardScroll = available.y > 0

                // In Full state: only collapse when scrolled to top and dragging down
                // This allows content to scroll freely
                return if (isFullState && hasUnconsumedDownwardScroll) {
                    controller.dragBy(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Handle fling with velocity
                controller.onFling(available.y, onStateChange)
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return Velocity.Zero
            }
        }
    }
}


