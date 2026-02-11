package com.example.lakbaylaya.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lakbaylaya.ui.screens.map.components.*
import com.example.lakbaylaya.ui.screens.map.maplibre.config.MapStyleConfig
import com.example.lakbaylaya.ui.screens.map.models.BottomSheetState
import com.example.lakbaylaya.ui.screens.map.models.UiMode
import com.example.lakbaylaya.ui.screens.map.models.NavigationState
import com.example.lakbaylaya.ui.screens.map.navigation.components.NavigationInstructionCard
import com.example.lakbaylaya.ui.screens.map.navigation.components.NavigationBottomSheet
import com.example.lakbaylaya.ui.screens.map.navigation.components.FloatingNavigationControls
import com.example.lakbaylaya.ui.screens.map.navigation.tts.AndroidTextToSpeechEngine
import com.example.lakbaylaya.ui.screens.map.navigation.pedometer.AndroidStepDetector
import com.example.lakbaylaya.ui.screens.map.viewmodel.MapViewModel
import com.example.lakbaylaya.ui.screens.map.viewmodel.MapViewModelFactory
import com.example.lakbaylaya.ui.screens.map.utils.rememberLocationPermissionState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.example.lakbaylaya.ui.screens.map.models.ManeuverType
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep

/**
 * Map Screen - Main composable for the map feature with modern UI/UX
 *
 * Features:
 * - Full-screen native MapLibre map background with theme-aware styles
 * - Rounded search bar at top with proper window insets (no overlap with status bar/notch)
 * - State-based search overlay (Idle, Typing, Searching, Results, Empty, Error)
 * - Auto-focus on search activation
 * - Back button closes overlay
 * - Debounced search with 3-character minimum
 * - Radius-based nearest location search
 * - Recent searches (max 10 items)
 * - Maximum 10 search results sorted by distance
 * - Category-based icons for visual hierarchy
 * - User location as blue puck on map
 * - Auto-focus camera on user location
 * - Marker placement on selected location
 * - Camera animation to selected result
 * - Accessibility support (TalkBack, focus order)
 * - Responsive design for all screen sizes
 * - Material 3 theming (light/dark mode)
 *
 * Architecture:
 * - MVVM pattern with MapViewModel
 * - Repository pattern for API access
 * - Unidirectional data flow
 * - Stateless composable
 * - Component-based architecture
 * - Clean architecture principles
 * - Sealed classes for type-safe state management
 * - Separation of concerns (UI, ViewModel, Repository, API, Models, Utils)
 *
 * Layout Structure (Normal State - Search Inactive):
 * ┌─────────────────────────┐
 * │   Status Bar Space      │ ← WindowInsets.statusBars
 * │ ┌───────────────────┐   │
 * │ │  Search Bar       │   │ ← Rounded, z-index 3, overlays map
 * │ └───────────────────┘   │
 * ├─────────────────────────┤
 * │                         │
 * │   Native MapLibre Map   │ ← Full screen, z-index 0
 * │   - Theme-aware style   │
 * │   - User location puck  │
 * │   - Selected marker     │
 * │                         │
 * └─────────────────────────┘
 *
 * Layout Structure (Search Active):
 * ┌─────────────────────────┐
 * │   Status Bar Space      │ ← WindowInsets.statusBars
 * │ ┌───────────────────┐   │
 * │ │ Search Bar + Back │   │ ← Rounded, at top, no big gap
 * │ └───────────────────┘   │
 * ├─────────────────────────┤
 * │   Search Overlay        │ ← z-index 2, solid background
 * │   - Recent / Results    │    Completely hides map & bottom bar
 * │   - Loading / Empty     │
 * │   (Scrollable)          │
 * └─────────────────────────┘
 *
 * State Flow:
 * 1. User clicks search bar -> isSearchOverlayActive = true, auto-focus
 * 2. User types:
 *    - 0 chars: Idle (show recent)
 *    - 1-2 chars: Typing (show recent, no search)
 *    - 3+ chars: Typing -> debounce -> Searching -> Results/Empty/Error
 * 3. User clicks result -> camera animates, marker placed, overlay closes
 * 4. User clicks back -> overlay closes, returns to map
 *
 * @param modifier Modifier for customization
 * @param viewModel MapViewModel for state management (injected for testing)
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    /**
     * A Dp amount to subtract from the system status bar inset applied to the
     * top of the search bar. Use a positive value to move the bar up (reduce
     * the effective inset). The resulting padding will never be negative.
     */
    topInsetAdjustment: Dp = 100.dp,
    /**
     * Callback to control bottom navigation bar visibility
     * Should hide bottom nav when search is active
     */
    onBottomNavVisibilityChange: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current

    // Create ViewModel with Application context using custom factory
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            context.applicationContext as android.app.Application
        )
    )

    val state by viewModel.state.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()

    // Create and remember MapLibreManager (explicit type ensures methods are resolved)
    val mapManager: com.example.lakbaylaya.ui.screens.map.maplibre.MapLibreManager = remember { com.example.lakbaylaya.ui.screens.map.maplibre.MapLibreManager(context) }
    var isMapReady by remember { mutableStateOf(false) }

    // Center-on-location toggle state (used by FAB and location updates)
    var isCenterOnLocation by remember { mutableStateOf(false) }

    // Initialize TTS and Step Detector for navigation
    val ttsEngine = remember { AndroidTextToSpeechEngine(context) }
    val stepDetector = remember { AndroidStepDetector(context) }
    val locationTracker = remember { com.example.lakbaylaya.ui.screens.map.navigation.location.AndroidLocationTracker(context) }

    // Initialize TTS when needed
    LaunchedEffect(Unit) {
        ttsEngine.initialize(
            onReady = {
                android.util.Log.d("MapScreen", "TTS ready")
            },
            onError = { error ->
                android.util.Log.e("MapScreen", "TTS error: $error")
            }
        )
    }

    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            stepDetector.stop()
            ttsEngine.shutdown()
            locationTracker.stopTracking()
        }
    }

    // Track navigation active state separately from internal state changes
    val isNavigationActive = state.navigationState is NavigationState.Active
    val navigationRouteId = (state.navigationState as? NavigationState.Active)?.routeOption?.name

    // Handle navigation start/stop (not internal state changes like mute/mapMode)
    LaunchedEffect(isNavigationActive, navigationRouteId) {
        when (val navState = state.navigationState) {
            is NavigationState.Active -> {
                // Start GPS location tracking
                locationTracker.setMinimumDistance(1f) // 1 meter minimum for highest accuracy
                locationTracker.setMinimumInterval(500L) // 0.5 second interval for responsive updates
                locationTracker.startTracking(
                    onLocationUpdate = { location ->
                        // Update navigation with GPS location for step threshold detection
                        viewModel.updateNavigationLocation(location)

                        // Also update map camera to follow user only when center-on-location is enabled
                        if (isCenterOnLocation) {
                            mapManager.animateTo(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                zoom = 16.0,
                                duration = 500 // Smooth following
                            )
                        }
                    }
                )

                // Start step detection for fitness tracking
                stepDetector.reset()
                stepDetector.start(
                    onStepDetected = { stepCount ->
                        viewModel.updateNavigationStepCount(stepCount)
                    },
                    onDistanceUpdated = { _ ->
                        // GPS is primary for navigation, step detector is for UI stats only
                    }
                )

                // Speak first instruction only when navigation starts (not on mute/mode changes)
                navState.getCurrentStep()?.let { step ->
                    if (!navState.isMuted) {
                        ttsEngine.speak(step.instruction, priority = true)
                    }
                }
            }
            is NavigationState.Inactive -> {
                // Stop GPS tracking and step detection
                locationTracker.stopTracking()
                stepDetector.stop()
                ttsEngine.stop()
            }
        }
    }

    // Handle automatic TTS for step advancement
    LaunchedEffect((state.navigationState as? NavigationState.Active)?.currentStepIndex) {
        val navState = state.navigationState
        if (navState is NavigationState.Active) {
            // Get previous step index to detect when it changes
            val currentStepIndex = navState.currentStepIndex

            // Only trigger TTS if this is not the initial state (step 0 is handled above)
            if (currentStepIndex > 0) {
                // Step was advanced - speak new instruction if not muted
                navState.getCurrentStep()?.let { step ->
                    if (!navState.isMuted) {
                        android.util.Log.d("MapScreen", "Speaking instruction for step $currentStepIndex: ${step.instruction}")
                        ttsEngine.speak(step.instruction, priority = true)
                    }
                }
            }
        }
    }

    // Handle mute state changes - stop TTS immediately when muted
    LaunchedEffect((state.navigationState as? NavigationState.Active)?.isMuted) {
        val navState = state.navigationState
        if (navState is NavigationState.Active && navState.isMuted) {
            ttsEngine.stop() // Immediately stop any ongoing speech when muted
        }
    }

    // Location permission handling
    val locationPermission = rememberLocationPermissionState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Update permission state in ViewModel
    LaunchedEffect(locationPermission.hasPermission) {
        viewModel.onLocationPermissionChanged(locationPermission.hasPermission)
    }

    // Control bottom navigation visibility based on search state
    LaunchedEffect(state.isSearchOverlayActive) {
        onBottomNavVisibilityChange?.invoke(!state.isSearchOverlayActive)
    }

    // Show permission dialog if not granted
    LaunchedEffect(Unit) {
        if (!locationPermission.hasPermission) {
            showPermissionDialog = true
        }
    }

    // Update marker when selected result changes
    LaunchedEffect(state.selectedResult, isMapReady) {
        if (isMapReady) {
            state.selectedResult?.let { result ->
                // Clear previous markers
                mapManager.clearMarkers()

                // Add new marker
                mapManager.addMarker(
                    latitude = result.latitude,
                    longitude = result.longitude,
                    title = result.placeName
                )

                // Animate camera to marker
                mapManager.animateTo(
                    latitude = result.latitude,
                    longitude = result.longitude,
                    zoom = 15.0
                )
            } ?: run {
                // When selectedResult is null, clear markers
                mapManager.clearMarkers()
            }
        }
    }

    // Draw polylines when in Direction Mode
    // Only trigger when polylines actually change or map becomes ready
    // Don't trigger on directionData selection changes
    LaunchedEffect(state.polylines, isMapReady) {
        if (isMapReady) {
            // Clear existing polylines and labels
            mapManager.clearPolylines()
            mapManager.clearMarkers()
            mapManager.clearStopMarkers()

            // Draw all route polylines and add labels at midpoint
            if (state.polylines.isNotEmpty()) {
                val dirData = state.directionData

                // If we have stops, color the first segment differently (current route)
                val hasStops = dirData?.stops?.isNotEmpty() == true
                val firstSegmentColor = android.graphics.Color.parseColor("#4CAF50") // green for current leg
                val futureSegmentColor = android.graphics.Color.parseColor("#9E9E9E") // gray for future legs

                state.polylines.forEachIndexed { idx, polylineData ->
                    // Always use dotted lines (isPrimary = false ensures dotted)
                    // Color: first segment green if stops exist, otherwise use polyline color
                    val color = if (hasStops && idx == 0) {
                        firstSegmentColor
                    } else if (hasStops) {
                        futureSegmentColor
                    } else {
                        null // use default
                    }

                    mapManager.drawPolyline(
                        routeId = polylineData.routeId,
                        coordinates = polylineData.coordinates,
                        isPrimary = false, // Always false = always dotted
                        color = color
                    )
                }

                // Draw waypoint markers A, B, C...
                // Only actual stops get circle markers (not origin/current location)
                // Destination uses standard marker icon
                dirData?.let { dd ->
                    // ONLY waypoint stops as circle markers with labels (A, B, C...)
                    val stopLabelPoints = buildList {
                        dd.stops.forEachIndexed { index, stop ->
                            val labelChar = ('A'.code + index).toChar().toString()
                            add(Triple(stop.latitude, stop.longitude, labelChar))
                        }
                    }
                    if (stopLabelPoints.isNotEmpty()) {
                        mapManager.drawStopMarkers(stopLabelPoints)
                    }

                    // Destination as standard marker icon
                    mapManager.addMarker(
                        latitude = dd.destination.latitude,
                        longitude = dd.destination.longitude,
                        title = dd.destination.name
                    )
                }
            }
        }
    }

    // Determine if we're in Direction Mode
    val isDirectionMode = state.uiMode is UiMode.Direction
    val isEditingStops = (state.uiMode as? UiMode.Direction)?.isEditingStops == true

    // Permission dialog
    if (showPermissionDialog && !locationPermission.hasPermission) {
        LocationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                locationPermission.requestPermission()
                showPermissionDialog = false
            }
        )
    }

    // Compute the status bar top padding once and apply a user-controlled
    // adjustment. Ensure the final padding is not negative.
    val topStatusBarPadding = (WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding() - topInsetAdjustment)
        .coerceAtLeast(10.dp)

    // Measure navigation instruction card height so we can position the map compass below it
    val instructionCardHeightPx = remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Update compass margins when map is ready or instruction card size changes
    LaunchedEffect(isMapReady, instructionCardHeightPx.value, topStatusBarPadding) {
        if (isMapReady) {
            try {
                val leftRightDp = 16.dp
                val bottomDp = 16.dp
                val extraSpacingDp = 8.dp

                val topPx = with(density) {
                    // topStatusBarPadding + 16.dp (card top padding) + instructionCardHeight + extra spacing
                    topStatusBarPadding.toPx() + 18.dp.toPx() + instructionCardHeightPx.value + extraSpacingDp.toPx()
                }.toInt()

                val lrPx = with(density) { leftRightDp.toPx().toInt() }
                val bPx = with(density) { bottomDp.toPx().toInt() }

                mapManager.setCompassMargins(lrPx, topPx, lrPx, bPx)
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to set compass margins: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTag = "map_screen" }
    ) {
        // Native MapLibre map view (full screen, behind everything)
        // Theme-aware style: dark map in dark mode, light map in light mode
        MapLibreView(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            // Always use the OSM-Carto / Streets style. Dark mode is disabled for map tiles.
            stylePreset = MapStyleConfig.StylePreset.STREETS,
            hasLocationPermission = state.hasLocationPermission,
            mapManager = mapManager,
            onMapReady = { manager ->
                isMapReady = true

                // Auto-focus on user location when map loads if permission granted
                if (state.hasLocationPermission) {
                    state.currentLocation?.let { location ->
                        manager.animateTo(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            zoom = 14.0
                        )
                    }
                }
            }
        )

        // Search overlay - only visible when search is active AND NOT in Direction Mode (or editing stops)
        if (state.isSearchOverlayActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .zIndex(2f)
            ) {
                // Search bar pinned at top
                FloatingSearchBar(
                    query = state.searchQuery,
                    isOverlayActive = state.isSearchOverlayActive,
                    onQueryChange = { query ->
                        viewModel.onSearchQueryChange(query)
                    },
                    onSearchBarClick = {
                        viewModel.onSearchBarClick()
                    },
                    onBackClick = {
                        if (isEditingStops) {
                            // If editing stops, go back to Direction Mode
                            viewModel.onFinishEditingStops()
                        } else {
                            viewModel.onBackClick()
                        }
                    },
                    onClearClick = {
                        viewModel.onClearSearch()
                    },
                    onMicClick = {
                        // TODO: Implement voice search
                        viewModel.onSearchBarClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topStatusBarPadding)
                )

                // Search results overlay
                SearchOverlay(
                    isVisible = state.isSearchOverlayActive,
                    searchUiState = state.searchUiState,
                    recentSearches = state.recentSearches,
                    onResultClick = { result ->
                        if (isEditingStops) {
                            // Add as stop
                            viewModel.onAddStopFromSearch(result)
                        } else {
                            // Normal search result selection
                            viewModel.onResultSelected(result)
                        }
                    },
                    onNavigateClick = { result ->
                        viewModel.navigateToLocation(result)
                    },
                    onDismiss = {
                        if (isEditingStops) {
                            viewModel.onFinishEditingStops()
                        } else {
                            viewModel.onBackClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        } else {
            // NORMAL MODE: Show search bar
            // DIRECTION MODE: Show Overlay Stop Panel instead (unless sheet is full expanded)
            val isDirectionFullExpanded = state.bottomSheetState is BottomSheetState.DirectionFullExpand

            // Navigation UI (only show when navigation is active)
            val navigationState = state.navigationState
            if (navigationState is NavigationState.Active) {

                // Top instruction card (highest priority)
                if (navigationState.isCompleted) {
                    // Show arrival message when navigation is completed
                    val arrivalStep = DirectionStep(
                        instruction = "You have arrived at your destination",
                        distanceMeters = 0.0,
                        durationMinutes = 0,
                        maneuver = ManeuverType.ARRIVE,
                        latitude = state.directionData?.destination?.latitude ?: 0.0,
                        longitude = state.directionData?.destination?.longitude ?: 0.0
                    )

                    NavigationInstructionCard(
                        step = arrivalStep,
                        onClick = {
                            // No-op or repeat TTS for arrival
                            if (!navigationState.isMuted) {
                                ttsEngine.speak(arrivalStep.instruction, priority = true)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = topStatusBarPadding + 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                            .zIndex(5f)
                            .onGloballyPositioned { coords ->
                                instructionCardHeightPx.value = coords.size.height
                            }
                    )
                } else {
                    navigationState.getCurrentStep()?.let { step ->
                        NavigationInstructionCard(
                            step = step,
                            onClick = {
                                // Only repeat instruction via TTS, don't zoom map
                                if (!navigationState.isMuted) {
                                    ttsEngine.speak(step.instruction, priority = true)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(
                                    top = topStatusBarPadding + 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                )
                                .zIndex(5f)
                                .onGloballyPositioned { coords ->
                                    // Capture the height of the instruction card in pixels
                                    instructionCardHeightPx.value = coords.size.height
                                }
                        )
                    }
                }

                // Floating navigation controls (right side)
                FloatingNavigationControls(
                    isMuted = navigationState.isMuted,
                    isCentered = isCenterOnLocation,
                    onMuteToggle = {
                        viewModel.toggleNavigationMute()
                    },
                    onCenterToggle = {
                        // Toggle center-on-location mode
                        isCenterOnLocation = !isCenterOnLocation

                        // If enabling, immediately center on current location
                        if (isCenterOnLocation) {
                            state.currentLocation?.let { loc ->
                                mapManager.animateTo(
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    zoom = 16.0
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, bottom = 200.dp)
                        .zIndex(4f)
                )

                // Navigation bottom sheet
                NavigationBottomSheet(
                    navigationState = navigationState,
                    onClose = {
                        viewModel.stopNavigation()
                    },
                    onRecenter = {
                        // Show full route
                        val route = navigationState.routeOption
                        if (route.polylineCoordinates.isNotEmpty()) {
                            // Calculate bounds and zoom to fit route
                            val firstPoint = route.polylineCoordinates.first()
                            mapManager.animateTo(
                                latitude = firstPoint.first,
                                longitude = firstPoint.second,
                                zoom = 13.0
                            )
                        }
                    },
                    onDone = {
                        // Finish navigation and return to normal mode
                        viewModel.finishNavigation()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(3f)
                )
            } else {
                // NORMAL MODE: Show search bar
                // DIRECTION MODE: Show Overlay Stop Panel instead (unless sheet is full expanded)
                val isDirectionFullExpanded = state.bottomSheetState is BottomSheetState.DirectionFullExpand

                if (isDirectionMode && !isDirectionFullExpanded) {
                    // Overlay Stop Panel (replaces search bar in Direction Mode, hidden when full expanded)
                    state.directionData?.let { currentDirectionData ->
                        OverlayStopPanel(
                            directionData = currentDirectionData,
                            isEditingStops = isEditingStops,
                            onBack = {
                                viewModel.onDirectionBack()
                            },
                            onAddStop = {
                                viewModel.onStartEditingStops()
                            },
                            onRemoveStop = { index ->
                                viewModel.onRemoveStop(index)
                            },
                            onSwapOriginDestination = {
                                viewModel.onSwapOriginDestination()
                            },
                            onSwapStops = { i, j -> viewModel.onSwapStops(i, j) },
                            onSwapStopWithDestination = { index -> viewModel.onSwapStopWithDestination(index) },
                            onDone = {
                                viewModel.onFinishEditingStops()
                            },
                            onExpandedChange = { isExpanded ->
                                viewModel.onOverlayPanelExpandedChange(isExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .zIndex(3f)
                                .padding(top = topStatusBarPadding)
                        )
                    }
                } else if (!isDirectionMode) {
                    // Normal Mode: Search bar at top
                    FloatingSearchBar(
                        query = state.searchQuery,
                        isOverlayActive = state.isSearchOverlayActive,
                        onQueryChange = { query ->
                            viewModel.onSearchQueryChange(query)
                        },
                        onSearchBarClick = {
                            viewModel.onSearchBarClick()
                        },
                        onBackClick = {
                            viewModel.onBackClick()
                        },
                        onClearClick = {
                            viewModel.onClearSearch()
                        },
                        onMicClick = {
                            // TODO: Implement voice search
                            viewModel.onSearchBarClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .zIndex(3f)
                            .padding(top = topStatusBarPadding)
                    )
                }

                // Bottom Sheets based on mode
                if (isDirectionMode) {
                    // Direction Mode: Show Direction sheet (unless overlay panel is expanded)
                    if (!state.isOverlayPanelExpanded) {
                        DirectionBottomSheet(
                            sheetState = state.bottomSheetState,
                            onStateChange = { newState ->
                                viewModel.onBottomSheetStateChange(newState)
                            },
                            onRouteSelected = { routeIndex ->
                                viewModel.onRouteChange(routeIndex)
                            },
                            onAddStopsClick = {
                                viewModel.onStartEditingStops()
                            },
                            // When the user presses Close(X) in the directions sheet, go back from direction mode
                            onClose = { viewModel.onDirectionBack() },
                            onStartNavigation = {
                                viewModel.startNavigation()
                            },
                            topInsetAdjustment = topInsetAdjustment,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(2f),
                            bottomNavigationHeight = 80.dp
                        )
                    }
                } else {
                    // Normal Mode: Show appropriate sheet

                    // User location sheet (shown when no search and no place selected)
                    val showUserLocationSheet = state.bottomSheetState is BottomSheetState.Hidden

                    UserLocationBottomSheet(
                        isVisible = showUserLocationSheet,
                        currentLocation = state.currentLocation,
                        reverseGeocodedLocation = state.reverseGeocodedLocation,
                        isExpanded = state.isUserLocationSheetExpanded,
                        onToggle = {
                            viewModel.toggleUserLocationSheet()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f),
                        bottomNavigationHeight = 80.dp
                    )

                    // Place sheet (when place is selected)
                    if (state.bottomSheetState !is BottomSheetState.Hidden &&
                        state.bottomSheetState !is BottomSheetState.DirectionInitial &&
                        state.bottomSheetState !is BottomSheetState.DirectionFullExpand
                    ) {
                        PlaceBottomSheet(
                            sheetState = state.bottomSheetState,
                            onStateChange = { newState ->
                                viewModel.onBottomSheetStateChange(newState)
                            },
                            onActionClick = { action, place ->
                                viewModel.onPlaceAction(action, place)
                            },
                            onDismiss = {
                                viewModel.dismissPlace()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(
                                    if (state.bottomSheetState is BottomSheetState.Full) {
                                        4f
                                    } else {
                                        1f
                                    }
                                ),
                            bottomNavigationHeight = 80.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Location permission dialog
 */
@Composable
fun LocationPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "This app needs access to your location to show your position on the map and provide navigation features.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
