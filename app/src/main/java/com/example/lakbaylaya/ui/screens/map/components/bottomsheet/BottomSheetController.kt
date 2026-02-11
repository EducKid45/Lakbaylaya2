package com.example.lakbaylaya.ui.screens.map.components.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.lakbaylaya.ui.screens.map.models.BottomSheetState
import com.example.lakbaylaya.ui.screens.map.models.DirectionData
import com.example.lakbaylaya.ui.screens.map.models.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Unified controller for all bottom sheet states and transitions
 *
 * Handles:
 * - Place sheets (Hidden, Initial, Half, Full)
 * - Direction sheets (DirectionInitial, DirectionFullExpand)
 * - User Location sheet
 *
 * Features:
 * - Smooth spring/velocity-based animations
 * - Safe state transitions (prevents illegal transitions)
 * - State restoration (can return to previous state)
 * - Edge case handling (rapid taps, ongoing animations)
 * - Gesture coordination with scrolling content
 */
@Stable
class BottomSheetController(
    private val scope: CoroutineScope,
    private val expandedTop: Float,
    private val collapsedTop: Float,
    private val halfTop: Float,
    private val hiddenTop: Float,
    private val directionInitialTop: Float,
    private val directionFullTop: Float
) {
    // Single source of truth for sheet position (in pixels from top)
    private val offset = Animatable(hiddenTop)

    // Current logical state
    private var currentState: BottomSheetState = BottomSheetState.Hidden

    // Previous state for restoration (used for back navigation)
    private var previousState: BottomSheetState? = null

    // Stored data for state restoration
    private var lastPlace: SearchResult? = null
    private var lastDirectionData: DirectionData? = null

    // Current animation job (for cancellation on rapid gestures)
    private var currentAnimationJob: Job? = null

    /**
     * Get current offset value
     */
    fun getOffset(): Float = offset.value

    /**
     * Get current state
     */
    fun getCurrentState(): BottomSheetState = currentState

    /**
     * Get previous state (for restoration)
     */
    fun getPreviousState(): BottomSheetState? = previousState

    /**
     * Update logical state and animate to target position
     * Handles edge case: ongoing animation + new state request
     */
    fun setState(newState: BottomSheetState, onComplete: () -> Unit = {}) {
        if (newState == currentState) return

        // Cancel any ongoing animation
        currentAnimationJob?.cancel()

        // Store previous state for restoration
        previousState = currentState

        // Update current state
        currentState = newState

        // Store data for restoration
        when (newState) {
            is BottomSheetState.Initial -> lastPlace = newState.place
            is BottomSheetState.Half -> lastPlace = newState.place
            is BottomSheetState.Full -> lastPlace = newState.place
            is BottomSheetState.DirectionInitial -> lastDirectionData = newState.directionData
            is BottomSheetState.DirectionFullExpand -> lastDirectionData = newState.directionData
            is BottomSheetState.Hidden -> {} // Keep stored data
        }

        val targetOffset = getTargetForState(newState)
        currentAnimationJob = scope.launch {
            offset.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(
                    stiffness = 800f,
                    dampingRatio = 0.8f
                )
            )
            onComplete()
        }
    }

    /**
     * Restore previous state (for back navigation)
     */
    fun restorePreviousState(onComplete: () -> Unit = {}) {
        previousState?.let { prevState ->
            setState(prevState, onComplete)
        }
    }

    /**
     * Drag by delta (during finger movement)
     * Updates position immediately without animation
     */
    fun dragBy(deltaY: Float) {
        scope.launch {
            val minOffset = when (currentState) {
                is BottomSheetState.DirectionFullExpand -> directionFullTop
                is BottomSheetState.Full -> expandedTop
                else -> expandedTop
            }
            val newOffset = (offset.value + deltaY).coerceIn(minOffset, hiddenTop)
            offset.snapTo(newOffset)
        }
    }

    /**
     * Settle to nearest state based on current offset
     * Considers current state type (Place vs Direction)
     */
    fun settleNearest(onStateChange: (BottomSheetState) -> Unit) {
        val nearestState = nearestStateFor(offset.value)
        if (nearestState != currentState) {
            onStateChange(nearestState)
        } else {
            // If same state, just animate back to position
            val targetOffset = getTargetForState(nearestState)
            currentAnimationJob?.cancel()
            currentAnimationJob = scope.launch {
                offset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        stiffness = 800f,
                        dampingRatio = 0.8f
                    )
                )
            }
        }
    }

    /**
     * Animate to specific state
     */
    fun animateToState(state: BottomSheetState, onStateChange: (BottomSheetState) -> Unit) {
        if (state != currentState) {
            onStateChange(state)
        }
    }

    /**
     * Determine nearest state for given offset
     * Respects current state type (Place vs Direction)
     */
    fun nearestStateFor(offsetValue: Float): BottomSheetState {
        return when (currentState) {
            // Direction states can only settle to Direction states
            is BottomSheetState.DirectionInitial, is BottomSheetState.DirectionFullExpand -> {
                val directionData = lastDirectionData ?: return BottomSheetState.Hidden
                val distances = listOf(
                    directionFullTop to BottomSheetState.DirectionFullExpand(directionData),
                    directionInitialTop to BottomSheetState.DirectionInitial(directionData),
                    hiddenTop to BottomSheetState.Hidden
                )
                distances.minByOrNull { (target, _) ->
                    abs(offsetValue - target)
                }?.second ?: BottomSheetState.Hidden
            }
            // Place states can only settle to Place states
            else -> {
                val place = lastPlace ?: return BottomSheetState.Hidden
                val distances = listOf(
                    expandedTop to BottomSheetState.Full(place),
                    halfTop to BottomSheetState.Half(place),
                    collapsedTop to BottomSheetState.Initial(place),
                    hiddenTop to BottomSheetState.Hidden
                )
                distances.minByOrNull { (target, _) ->
                    abs(offsetValue - target)
                }?.second ?: BottomSheetState.Hidden
            }
        }
    }

    /**
     * Handle fling with velocity
     * Supports both Place and Direction state transitions
     */
    fun onFling(velocityY: Float, onStateChange: (BottomSheetState) -> Unit) {
        // Velocity threshold for fling detection (pixels per second)
        val flingVelocityThreshold = 500f

        if (abs(velocityY) < flingVelocityThreshold) {
            // Not fast enough, settle to nearest
            settleNearest(onStateChange)
            return
        }

        val targetState = when (currentState) {
            // Direction state fling transitions
            is BottomSheetState.DirectionInitial -> {
                if (velocityY < 0) { // Flinging up
                    (currentState as? BottomSheetState.DirectionInitial)?.let {
                        BottomSheetState.DirectionFullExpand(it.directionData)
                    } ?: currentState
                } else { // Flinging down
                    BottomSheetState.Hidden
                }
            }
            is BottomSheetState.DirectionFullExpand -> {
                if (velocityY > 0) { // Flinging down
                    (currentState as? BottomSheetState.DirectionFullExpand)?.let {
                        BottomSheetState.DirectionInitial(it.directionData)
                    } ?: currentState
                } else {
                    currentState
                }
            }
            // Place state fling transitions
            is BottomSheetState.Hidden -> {
                if (velocityY < 0) {
                    lastPlace?.let { BottomSheetState.Initial(it) } ?: currentState
                } else {
                    currentState
                }
            }
            is BottomSheetState.Initial -> {
                if (velocityY < 0) {
                    (currentState as? BottomSheetState.Initial)?.let {
                        BottomSheetState.Half(it.place)
                    } ?: currentState
                } else {
                    BottomSheetState.Hidden
                }
            }
            is BottomSheetState.Half -> {
                if (velocityY < 0) {
                    (currentState as? BottomSheetState.Half)?.let {
                        BottomSheetState.Full(it.place)
                    } ?: currentState
                } else {
                    (currentState as? BottomSheetState.Half)?.let {
                        BottomSheetState.Initial(it.place)
                    } ?: currentState
                }
            }
            is BottomSheetState.Full -> {
                if (velocityY > 0) {
                    (currentState as? BottomSheetState.Full)?.let {
                        BottomSheetState.Half(it.place)
                    } ?: currentState
                } else {
                    currentState
                }
            }
        }

        if (targetState != currentState) {
            onStateChange(targetState)
        }
    }

    /**
     * Cycle to next state (for click/tap on handle)
     * Place states cycle through Place hierarchy
     * Direction states cycle through Direction hierarchy
     */
    fun cycleState(onStateChange: (BottomSheetState) -> Unit) {
        val nextState = when (currentState) {
            // Direction state cycling
            is BottomSheetState.DirectionInitial -> {
                (currentState as? BottomSheetState.DirectionInitial)?.let {
                    BottomSheetState.DirectionFullExpand(it.directionData)
                } ?: currentState
            }
            is BottomSheetState.DirectionFullExpand -> {
                (currentState as? BottomSheetState.DirectionFullExpand)?.let {
                    BottomSheetState.DirectionInitial(it.directionData)
                } ?: currentState
            }
            // Place state cycling
            is BottomSheetState.Hidden -> {
                lastPlace?.let { BottomSheetState.Initial(it) } ?: currentState
            }
            is BottomSheetState.Initial -> {
                (currentState as? BottomSheetState.Initial)?.let {
                    BottomSheetState.Half(it.place)
                } ?: currentState
            }
            is BottomSheetState.Half -> {
                (currentState as? BottomSheetState.Half)?.let {
                    BottomSheetState.Full(it.place)
                } ?: currentState
            }
            is BottomSheetState.Full -> {
                (currentState as? BottomSheetState.Full)?.let {
                    BottomSheetState.Initial(it.place)
                } ?: currentState
            }
        }

        if (nextState != currentState) {
            onStateChange(nextState)
        }
    }

    /**
     * Get target offset for state
     */
    private fun getTargetForState(state: BottomSheetState): Float {
        return when (state) {
            is BottomSheetState.Hidden -> hiddenTop
            is BottomSheetState.Initial -> collapsedTop
            is BottomSheetState.Half -> halfTop
            is BottomSheetState.Full -> expandedTop
            is BottomSheetState.DirectionInitial -> directionInitialTop
            is BottomSheetState.DirectionFullExpand -> directionFullTop
        }
    }

    /**
     * Check if current state is a Direction state
     */
    fun isDirectionMode(): Boolean {
        return currentState is BottomSheetState.DirectionInitial ||
               currentState is BottomSheetState.DirectionFullExpand
    }

    /**
     * Check if current state is a Place state
     */
    fun isPlaceMode(): Boolean {
        return currentState is BottomSheetState.Initial ||
               currentState is BottomSheetState.Half ||
               currentState is BottomSheetState.Full
    }
}

/**
 * Remember bottom sheet controller with proper dependencies
 */
@Composable
fun rememberBottomSheetController(
    scope: CoroutineScope,
    screenHeightDp: Dp,
    bottomNavigationHeight: Dp
): BottomSheetController {
    val density = LocalDensity.current

    return remember(screenHeightDp) {
        with(density) {
            val screenHeightPx = screenHeightDp.toPx()

            // Calculate target positions
            val expandedTop = screenHeightPx * (1f - BottomSheetConstants.FULL_STATE_SCREEN_RATIO)
            val halfTop = screenHeightPx * (1f - BottomSheetConstants.HALF_STATE_SCREEN_RATIO)
            val collapsedTop = screenHeightPx - BottomSheetConstants.INITIAL_STATE_HEIGHT.toPx()
            val hiddenTop = screenHeightPx - BottomSheetConstants.HANDLE_ONLY_HEIGHT.toPx()
            val directionInitialTop = screenHeightPx * (1f - BottomSheetConstants.DIRECTION_INITIAL_SCREEN_RATIO)
            val directionFullTop = screenHeightPx * (1f - BottomSheetConstants.DIRECTION_FULL_SCREEN_RATIO)

            BottomSheetController(
                scope = scope,
                expandedTop = expandedTop,
                collapsedTop = collapsedTop,
                halfTop = halfTop,
                hiddenTop = hiddenTop,
                directionInitialTop = directionInitialTop,
                directionFullTop = directionFullTop
            )
        }
    }
}

