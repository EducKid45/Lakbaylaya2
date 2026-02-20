package com.example.lakbaylaya.ui.screens.map

import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lakbaylaya.ui.screens.map.components.*
import com.example.lakbaylaya.maplibre.config.MapStyleConfig
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
import com.example.lakbaylaya.ui.screens.map.viewmodel.MarkerDataViewModel
import com.example.lakbaylaya.utils.rememberLocationPermissionState
import com.example.lakbaylaya.data.model.VoiceNote
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.lakbaylaya.ui.screens.map.components.MapLibreView
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import com.example.lakbaylaya.ui.screens.map.models.ManeuverType
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.lakbaylaya.ui.screens.map.navigation.voice.VoiceCommand


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
    onBottomNavVisibilityChange: ((Boolean) -> Unit)? = null,
    /**
     * Callback to navigate to Voice Notes screen with location and place data
     */
    onNavigateToVoiceNotes: ((Double, Double, String?) -> Unit)? = null
) {
    val context = LocalContext.current

    // Create ViewModel with Application context using custom factory
    val vm: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            context.applicationContext as android.app.Application
        )
    )

    // ViewModel for marker data persistence using AndroidViewModelFactory
    val markerDataVm: MarkerDataViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as android.app.Application
        )
    )

    val state by vm.state.collectAsState()

    // Create a coroutine scope for launching short delays before opening the recognizer
    val composeScope = rememberCoroutineScope()

    // Speech recognizer launcher: system voice recognition activity (shows Google mic UI)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Always stop listening state first
        vm.stopVoiceListening()

        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                // Get the best (first) non-empty result
                val recognizedText = matches?.firstOrNull { it.isNotBlank() }?.trim() ?: ""

                if (recognizedText.isNotBlank()) {
                    android.util.Log.d("MapScreen", "Speech recognized: '$recognizedText'")
                    Toast.makeText(context, "✓ Heard: $recognizedText", Toast.LENGTH_SHORT).show()

                    // Send the recognized text to ViewModel to populate search and trigger search
                    vm.onVoiceResult(recognizedText, true)
                } else {
                    android.util.Log.d("MapScreen", "Speech result was empty")
                    Toast.makeText(context, "No speech detected. Try again.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            android.app.Activity.RESULT_CANCELED -> {
                // User cancelled or speech recognizer was dismissed
                android.util.Log.d("MapScreen", "Speech recognition cancelled by user")
                // Don't show toast for cancel - user knows they cancelled
            }

            else -> {
                // Some error occurred
                android.util.Log.e(
                    "MapScreen",
                    "Speech recognition failed with code: ${result.resultCode}"
                )
                Toast.makeText(
                    context,
                    "Speech recognition failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Permission launcher for RECORD_AUDIO - required before launching speech recognizer
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, speak cue then launch the speech recognizer shortly after
            composeScope.launch {
                vm.playListeningCue()
                delay(350)
                launchSpeechRecognizer(context, speechLauncher, vm)
            }
        } else {
            Toast.makeText(
                context,
                "Microphone permission required for voice input",
                Toast.LENGTH_SHORT
            ).show()
            vm.stopVoiceListening()
        }
    }

    // (No initial metadata) Marker dialog is opened using the selected place only

    // Create and remember MapLibreManager (explicit type ensures methods are resolved)
    val mapManager: MapLibreManager = remember { MapLibreManager(context) }
    var isMapReady by remember { mutableStateOf(false) }

    // Set mapManager reference in ViewModel for camera control
    LaunchedEffect(mapManager) {
        vm.mapManager = mapManager
    }

    // Center-on-location toggle state (used by FAB and location updates)
    var isCenterOnLocation by remember { mutableStateOf(false) }

    // Marker action dialog state - managed at top level for z-index ordering
    var showMarkerDialog by remember { mutableStateOf(false) }
    var selectedPlaceForMarker by remember {
        mutableStateOf<com.example.lakbaylaya.ui.screens.map.models.SearchResult?>(
            null
        )
    }
    // Initial metadata used to pre-fill the MarkerActionDialog when marking current location or a place
    var initialMarkerMetadata by remember {
        mutableStateOf<com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata?>(
            null
        )
    }
    val focusManager = LocalFocusManager.current
    // use the composeScope declared earlier (rememberCoroutineScope) to launch short coroutines

    // When marker editor becomes active, ensure all overlays and bottom sheets are closed
    // This guards against races where search overlay or place-sheet might still be visible.
    LaunchedEffect(state.isMarkerEditing) {
        if (state.isMarkerEditing) {
            // Clear focus to hide keyboard
            focusManager.clearFocus(force = true)

            // Close search overlay immediately
            vm.onBackClick()

            // Hide any bottom sheet to ensure editor OK/Confirm buttons are visible
            vm.onBottomSheetStateChange(BottomSheetState.Hidden)
        }
    }

    // Handle mic click: check permission and launch Google speech recognizer
    fun handleMicClick() {
        vm.startVoiceListening()

        // Check if RECORD_AUDIO permission is granted
        val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                )

        if (hasPermission) {
            // Permission already granted, speak cue then launch speech recognizer shortly after
            composeScope.launch {
                vm.playListeningCue()
                delay(350)
                launchSpeechRecognizer(context, speechLauncher, vm)
            }
        } else {
            // Request permission first
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // When marker dialog opens, clear focus to dismiss keyboard/search focus which may render UI above the dialog
    LaunchedEffect(showMarkerDialog) {
        if (showMarkerDialog) {
            focusManager.clearFocus(force = true)
            // Also ensure the search overlay is closed so no search UI remains above the dialog
            vm.onBackClick()
        }
    }

    // Initialize TTS and Step Detector for navigation
    val ttsEngine = remember { AndroidTextToSpeechEngine(context) }
    val stepDetector = remember { AndroidStepDetector(context) }
    val locationTracker = remember { com.example.lakbaylaya.ui.screens.map.navigation.location.AndroidLocationTracker(context) }

    // Initialize NavigationVoiceManager for voice commands during navigation
    val navigationVoiceManager = remember {
        // Create the handler object first
        val handler =
            object : com.example.lakbaylaya.ui.screens.map.navigation.voice.VoiceCommandHandler {
                override fun onRepeatInstruction() {
                    // Repeat the current instruction
                    (state.navigationState as? NavigationState.Active)?.getCurrentStep()
                        ?.let { step ->
                            ttsEngine.speak(step.instruction, priority = true)
                        }
                }

                override fun onPauseNavigation() {
                    // TODO: Implement pause logic in ViewModel/NavigationManager
                    android.util.Log.d("MapScreen", "Pause navigation command")
                }

                override fun onResumeNavigation() {
                    // TODO: Implement resume logic
                    android.util.Log.d("MapScreen", "Resume navigation command")
                }

                override fun onCheckCurrentPosition(latitude: Double, longitude: Double) {
                    // Current position is already being displayed on map
                    android.util.Log.d("MapScreen", "Current position: $latitude, $longitude")
                }

                override fun onDistanceToDestination(distanceMeters: Double) {
                    // Distance feedback is spoken by NavigationVoiceManager
                    android.util.Log.d(
                        "MapScreen",
                        "Distance to destination: $distanceMeters meters"
                    )
                }

                override fun onSwitchRoute() {
                    // Switch to next available route if multiple routes exist
                    val currentNav = state.navigationState
                    if (currentNav is NavigationState.Active) {
                        val dirData = state.directionData
                        if (dirData != null && dirData.routes.size > 1) {
                            val nextIndex = (dirData.selectedRouteIndex + 1) % dirData.routes.size
                            vm.onRouteChange(nextIndex)
                        }
                    }
                }

                override fun onCancelNavigation() {
                    vm.stopNavigation()
                }

                override fun onActivateEmergencyMode() {
                    // TODO: Trigger emergency SMS/call from MainActivity
                    android.util.Log.d("MapScreen", "Emergency mode activated")
                }
            }

        // Construct the manager with application context and handler
        com.example.lakbaylaya.ui.screens.map.navigation.voice.NavigationVoiceManager(
            context.applicationContext as android.app.Application,
            handler
        )
    }

    // Voice command state for toggleable mic
    var isVoiceCommandActive by remember { mutableStateOf(false) }
    // Retry state for navigation voice commands
    val navVoiceRetries = remember { mutableStateOf(0) }
    val maxNavVoiceRetries = 2
    // Flag to request relaunching the navigation voice recognizer from outside the launcher callback
    val navRelaunchFlag = remember { mutableStateOf(false) }

    // Speech recognizer launcher for voice commands during navigation
    val navVoiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Do NOT reset the active flag here — keep the mic toggle ON while TTS/auto-retry runs.

        // Helper vibrator function
        fun vibrateShort() {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(
                            VibrationEffect.createOneShot(
                                120,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(120)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MapScreen", "Vibration failed: ${e.message}")
            }
        }

        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.firstOrNull { it.isNotBlank() }?.trim() ?: ""

                if (recognizedText.isNotBlank()) {
                    android.util.Log.d("MapScreen", "Voice command: '$recognizedText'")

                    // Process voice command using NavigationVoiceManager
                    val currentNav = state.navigationState
                    if (currentNav is NavigationState.Active) {
                        // Estimate remaining distance (fallback)
                        var remainingDistance = 0.0
                        state.directionData?.routes?.firstOrNull()?.let { route ->
                            remainingDistance = 1000.0
                        }

                        val command = navigationVoiceManager.processVoiceCommand(
                            transcript = recognizedText,
                            currentLocationLat = state.currentLocation?.latitude ?: 0.0,
                            currentLocationLng = state.currentLocation?.longitude ?: 0.0,
                            distanceToDestination = remainingDistance
                        )

                        // If command not recognized, retry a couple times with TTS + vibration
                        if (command is VoiceCommand.UnknownCommand) {
                            // Use a retry counter stored in remember
                            navVoiceRetries.value =
                                (navVoiceRetries.value + 1).coerceAtMost(maxNavVoiceRetries)

                            if (navVoiceRetries.value <= maxNavVoiceRetries) {
                                // Speak a short retry prompt
                                navigationVoiceManager.speak("Sorry, I didn't catch that. Please say the command again.")
                                vibrateShort()

                                // Relaunch recognizer after a short delay to give user time
                                // Request a relaunch via LaunchedEffect below (can't reference launcher inside its own initializer)
                                // Only relaunch if the mic is still active
                                if (isVoiceCommandActive) navRelaunchFlag.value = true
                            } else {
                                navigationVoiceManager.speak("Sorry, I couldn't understand. Try again later.")
                                navVoiceRetries.value = 0
                                // end voice mode after exhausted retries
                                isVoiceCommandActive = false
                            }
                        } else {
                            // Successful recognition - reset retry counter and provide haptic confirmation
                            navVoiceRetries.value = 0
                            vibrateShort()
                            // Keep mic active (user explicitly toggles off)
                        }
                    }
                } else {
                    // No speech recognized - give feedback and offer retry
                    android.util.Log.d("MapScreen", "Voice command result was empty")
                    navigationVoiceManager.speak("No speech detected. Please say the command again.")
                    // vibrate and relaunch up to retry limit
                    navVoiceRetries.value =
                        (navVoiceRetries.value + 1).coerceAtMost(maxNavVoiceRetries)
                    if (navVoiceRetries.value <= maxNavVoiceRetries) {
                        vibrateShort()
                        // Request relaunch via LaunchedEffect (only if mic still active)
                        if (isVoiceCommandActive) navRelaunchFlag.value = true
                    } else {
                        navigationVoiceManager.speak("No input detected. Cancelling voice command mode.")
                        navVoiceRetries.value = 0
                        isVoiceCommandActive = false
                    }
                }
            }

            android.app.Activity.RESULT_CANCELED -> {
                android.util.Log.d("MapScreen", "Voice command cancelled")
                // reset retries and turn mic off — user likely dismissed recognizer
                navVoiceRetries.value = 0
                isVoiceCommandActive = false
            }

            else -> {
                android.util.Log.e(
                    "MapScreen",
                    "Voice command failed with code: ${result.resultCode}"
                )
                navVoiceRetries.value = 0
            }
        }
    }

    // Mic permission launcher for voice commands during navigation
    val navMicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted: mark mic active and launch recognizer
            isVoiceCommandActive = true
            composeScope.launch {
                navigationVoiceManager.speakListeningPrompt()
                delay(500)
                launchNavigationVoiceCommand(context, navVoiceSpeechLauncher)
            }
        } else {
            isVoiceCommandActive = false
            android.util.Log.w("MapScreen", "Microphone permission denied for voice commands")
        }
    }

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
            navigationVoiceManager.shutdown()
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
                        vm.updateNavigationLocation(location)

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
                        vm.updateNavigationStepCount(stepCount)
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
        vm.onLocationPermissionChanged(locationPermission.hasPermission)
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

    // Handle Voice Notes navigation
    LaunchedEffect(state.pendingVoiceNotesPlace) {
        state.pendingVoiceNotesPlace?.let { place ->
            onNavigateToVoiceNotes?.invoke(
                place.latitude,
                place.longitude,
                place.placeName
            )
            vm.clearPendingVoiceNotesPlace()
        }
    }

    // Load and display voice notes markers on map
    var voiceNotes by remember { mutableStateOf<List<VoiceNote>>(emptyList()) }
    var refreshVoiceNotes by remember { mutableStateOf(0) } // Trigger refresh counter


    LaunchedEffect(isMapReady, refreshVoiceNotes) {
        if (isMapReady) {

            // Do not clear user-added temporary markers here. Only refresh voice note markers.

            // Display voice note markers on map (ensure persistent)
            voiceNotes.forEach { note ->
                mapManager.addMarker(
                    latitude = note.latitude,
                    longitude = note.longitude,
                    title = "Voice Note: ${note.id}",
                    persistent = true
                )
            }

            android.util.Log.d("MapScreen", "Loaded ${voiceNotes.size} voice note markers")
        }
    }

    // Refresh voice notes when returning from Voice Notes screen
    LaunchedEffect(state.pendingVoiceNotesPlace) {
        if (state.pendingVoiceNotesPlace == null && voiceNotes.isNotEmpty()) {
            // User might have created a new note, refresh the list
            refreshVoiceNotes++
        }
    }

    // Update marker when selected result changes
    LaunchedEffect(state.selectedResult, isMapReady) {
        if (isMapReady) {
            state.selectedResult?.let { result ->
                // Clear previous temporary markers (preserve saved/persistent markers)
                // Only clear preview markers (don't remove user-set temporary red markers)
                mapManager.clearPreviewMarkers()

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
                // When selectedResult is null, clear preview markers only (preserve user-set temporary red markers)
                mapManager.clearPreviewMarkers()
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
            mapManager.clearAllMarkers()
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
        // Also hide the overlay while the marker editor is active so its OK/Cancel controls remain visible
        if (state.isSearchOverlayActive && !showMarkerDialog && !state.isMarkerEditing) {
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
                    isMicActive = state.isVoiceListening,
                    onQueryChange = { vm.onSearchQueryChange(it) },
                    onSearchBarClick = { vm.onSearchBarClick() },
                    onBackClick = { vm.onBackClick() },
                    onClearClick = { vm.onClearSearch() },
                    onMicClick = { handleMicClick() }
                )

                // Search results overlay
                SearchOverlay(
                    isVisible = state.isSearchOverlayActive,
                    searchUiState = state.searchUiState,
                    recentSearches = state.recentSearches,
                    onResultClick = { result ->
                        if (isEditingStops) {
                            // Add as stop
                            vm.onAddStopFromSearch(result)
                        } else {
                            // Normal search result selection
                            vm.onResultSelected(result)
                        }
                    },
                    onNavigateClick = { result ->
                        vm.navigateToLocation(result)
                    },
                    onMarkerEditClick = { result ->
                        // Open marker editor overlay to reposition pin at search location
                        vm.onSearchResultMarkerEdit(result)
                    },
                    onDismiss = {
                        if (isEditingStops) {
                            vm.onFinishEditingStops()
                        } else {
                            vm.onBackClick()
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
                            // Repeat TTS for arrival
                            if (!navigationState.isMuted) {
                                ttsEngine.speak(arrivalStep.instruction, priority = true)
                            }
                        },
                        isArrival = true, // Mark as arrival to show special styling
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
                        vm.toggleNavigationMute()
                    },
                    isVoiceActive = isVoiceCommandActive,
                    onVoiceToggle = {
                        isVoiceCommandActive = !isVoiceCommandActive

                        if (isVoiceCommandActive) {
                            // Check microphone permission and launch voice command recognizer
                            val hasPermission =
                                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        )

                            if (hasPermission) {
                                composeScope.launch {
                                    navigationVoiceManager.speakListeningPrompt()
                                    delay(500)
                                    launchNavigationVoiceCommand(context, navVoiceSpeechLauncher)
                                }
                            } else {
                                navMicPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            navigationVoiceManager.stop()
                        }
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
                    useVoiceCommand = true,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, bottom = 200.dp)
                        .zIndex(4f)
                )

                // Navigation bottom sheet
                NavigationBottomSheet(
                    navigationState = navigationState,
                    onClose = {
                        vm.stopNavigation()
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
                        vm.finishNavigation()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(3f)
                )
            } else {
                // NORMAL MODE: Show search bar
                // DIRECTION MODE: Show Overlay Stop Panel instead (unless sheet is full expanded)
                val isDirectionFullExpanded = state.bottomSheetState is BottomSheetState.DirectionFullExpand

                if (isDirectionMode && !isDirectionFullExpanded && !state.isMarkerEditing) {
                    // Overlay Stop Panel (replaces search bar in Direction Mode, hidden when full expanded)
                    state.directionData?.let { currentDirectionData ->
                        OverlayStopPanel(
                            directionData = currentDirectionData,
                            isEditingStops = isEditingStops,
                            onBack = {
                                vm.onDirectionBack()
                            },
                            onAddStop = {
                                vm.onStartEditingStops()
                            },
                            onRemoveStop = { index ->
                                vm.onRemoveStop(index)
                            },
                            onSwapOriginDestination = {
                                vm.onSwapOriginDestination()
                            },
                            onSwapStops = { i, j -> vm.onSwapStops(i, j) },
                            onSwapStopWithDestination = { index ->
                                vm.onSwapStopWithDestination(
                                    index
                                )
                            },
                            onDone = {
                                vm.onFinishEditingStops()
                            },
                            onExpandedChange = { isExpanded ->
                                vm.onOverlayPanelExpandedChange(isExpanded)
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
                    if (!showMarkerDialog && !state.isMarkerEditing) {
                        FloatingSearchBar(
                            query = state.searchQuery,
                            isOverlayActive = state.isSearchOverlayActive,
                            isMicActive = state.isVoiceListening,
                            onQueryChange = { vm.onSearchQueryChange(it) },
                            onSearchBarClick = { vm.onSearchBarClick() },
                            onBackClick = { vm.onBackClick() },
                            onClearClick = { vm.onClearSearch() },
                            onMicClick = { handleMicClick() }
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(top = topStatusBarPadding)
                        )
                    }
                }

                // Bottom Sheets based on mode
                if (isDirectionMode) {
                    // Direction Mode: Show Direction sheet (unless overlay panel is expanded)
                    if (!state.isOverlayPanelExpanded) {
                        DirectionBottomSheet(
                            sheetState = state.bottomSheetState,
                            onStateChange = { newState ->
                                vm.onBottomSheetStateChange(newState)
                            },
                            onRouteSelected = { routeIndex ->
                                vm.onRouteChange(routeIndex)
                            },
                            onAddStopsClick = {
                                vm.onStartEditingStops()
                            },
                            // When the user presses Close(X) in the directions sheet, go back from direction mode
                            onClose = { vm.onDirectionBack() },
                            onStartNavigation = {
                                vm.startNavigation()
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
                    // Hide the user location sheet while marker editor is active so it doesn't cover the editor controls
                    val showUserLocationSheet =
                        (state.bottomSheetState is BottomSheetState.Hidden) && !state.isMarkerEditing

                    UserLocationBottomSheet(
                        isVisible = showUserLocationSheet,
                        currentLocation = state.currentLocation,
                        reverseGeocodedLocation = state.reverseGeocodedLocation,
                        isExpanded = state.isUserLocationSheetExpanded,
                        onToggle = {
                            vm.toggleUserLocationSheet()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f),
                        bottomNavigationHeight = 80.dp
                    )

                    // Place sheet (when place is selected)
                    if ((state.bottomSheetState !is BottomSheetState.Hidden &&
                        state.bottomSheetState !is BottomSheetState.DirectionInitial &&
                                state.bottomSheetState !is BottomSheetState.DirectionFullExpand) && !showMarkerDialog && !state.isMarkerEditing
                    ) {
                        PlaceBottomSheet(
                            sheetState = state.bottomSheetState,
                            onStateChange = { newState ->
                                vm.onBottomSheetStateChange(newState)
                            },
                            onActionClick = { action, place ->
                                vm.onPlaceAction(action, place)
                            },
                            onDismiss = {
                                vm.dismissPlace()
                            },
                            showMarkerDialog = showMarkerDialog,
                            onShowMarkerDialog = { show, place ->
                                if (show) {
                                    // close search overlay first to avoid any race where search UI stays on top
                                    vm.onBackClick()
                                    focusManager.clearFocus(force = true)

                                    composeScope.launch {
                                        delay(120)

                                        if (place == null) {
                                            // Mark current device location: use ViewModel's currentLocation when available
                                            val curLoc = state.currentLocation
                                            val displayName =
                                                state.reverseGeocodedLocation?.placeName
                                                    ?: "Current Location"

                                            initialMarkerMetadata = if (curLoc != null) {
                                                com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata(
                                                    latitude = curLoc.latitude,
                                                    longitude = curLoc.longitude,
                                                    landmarkName = displayName
                                                )
                                            } else {
                                                com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata(
                                                    landmarkName = "Current Location"
                                                )
                                            }

                                            selectedPlaceForMarker = null
                                            showMarkerDialog = true
                                        } else {
                                            // Mark the selected place from the bottom sheet
                                            initialMarkerMetadata =
                                                com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata(
                                                    latitude = place.latitude,
                                                    longitude = place.longitude,
                                                    landmarkName = place.placeName
                                                )

                                            selectedPlaceForMarker = place
                                            showMarkerDialog = true
                                        }
                                    }
                                } else {
                                    showMarkerDialog = false
                                    selectedPlaceForMarker = null
                                    initialMarkerMetadata = null
                                }
                            },

                            modifier = Modifier
                                .align(Alignment.BottomCenter),
                            bottomNavigationHeight = 80.dp
                        )
                    }
                }
            }
        }

        // Marker action dialog - ALWAYS rendered last to ensure highest z-index
        // This must be outside all conditional blocks to be on top of everything
        if (showMarkerDialog && (selectedPlaceForMarker != null || initialMarkerMetadata != null)) {
            MarkerActionDialog(
                isVisible = showMarkerDialog,
                onDismiss = {
                    showMarkerDialog = false
                    selectedPlaceForMarker = null
                    initialMarkerMetadata = null
                },
                onSaveNotes = { metadata ->
                    // If coordinates were provided by the dialog, add a new marker to the map
                    metadata.latitude?.let { lat ->
                        metadata.longitude?.let { lng ->
                            // Ensure map is ready before adding the marker. If not ready, schedule add.
                            composeScope.launch {
                                if (!isMapReady) {
                                    // wait until map is initialized
                                    while (!isMapReady) {
                                        delay(100)
                                    }
                                }

                                // Clear preview markers before adding the new green one
                                mapManager.clearPreviewMarkers()

                                // Add ONLY the green persistent marker directly - no preview, no promotion
                                mapManager.addMarker(
                                    latitude = lat,
                                    longitude = lng,
                                    title = metadata.landmarkName.ifBlank { "Saved Marker" },
                                    persistent = true // GREEN MARKER
                                )

                                android.util.Log.d(
                                    "MapScreen",
                                    "Added green persistent marker at $lat,$lng"
                                )

                                // Persist to database
                                markerDataVm.saveLandmark(
                                    latitude = lat,
                                    longitude = lng,
                                    locationName = metadata.landmarkName,
                                    routeDifficulty = metadata.routeDifficulty,
                                    description = metadata.landmarkDescription
                                )

                                // Save voice notes to database
                                metadata.voiceNotes.forEach { voiceNote ->
                                    voiceNote.audioFilePath?.let { audioPath ->
                                        markerDataVm.saveVoiceNote(
                                            latitude = lat,
                                            longitude = lng,
                                            locationName = metadata.landmarkName,
                                            audioFilePath = audioPath,
                                            transcription = voiceNote.text,
                                            durationSeconds = 0 // TODO: calculate duration
                                        )
                                    }
                                }

                                android.util.Log.d("MapScreen", "Persisted marker data to database")
                            }
                        }
                    }

                    showMarkerDialog = false
                    selectedPlaceForMarker = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f), // ensure dialog is highest z-order
                mapManager = mapManager,
                initialMetadata = initialMarkerMetadata
                    ?: com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata()
            )
        }

        // Marker editor overlay for repositioning search result pins
        if (state.isMarkerEditing) {
            com.example.lakbaylaya.ui.screens.map.components.markerEdit.MarkerLocationEditorDialog(
                isVisible = true,
                mapManager = mapManager,
                initialResult = state.markerEditingResult,
                onLocationSelected = { latitude, longitude, address ->
                    vm.onMarkerEditConfirmed(latitude, longitude, address)
                },
                onCancel = {
                    vm.onMarkerEditCancelled()
                }
            )
        }
    }
}

/**
 * Launch the system speech recognizer with proper settings to prevent auto-close
 *
 * Key settings to prevent premature closing:
 * - Use WEB_SEARCH language model (more tolerant of pauses)
 * - Set explicit language
 * - Configure silence timeouts (API 23+)
 * - Disable offline mode for better accuracy
 */
private fun launchSpeechRecognizer(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    vm: MapViewModel
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Use WEB_SEARCH model - it's more tolerant of pauses and background noise
            // FREE_FORM can close too quickly on some devices
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )

            // Explicitly set language - use English for place names, or device default
            val deviceLocale = java.util.Locale.getDefault()
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceLocale.toLanguageTag())

            // Also set the only_return_language_preference to false so it accepts any language
            putExtra("android.speech.extra.ONLY_RETURN_LANGUAGE_PREFERENCE", false)

            // Prompt shown in the recognizer UI - clear instruction
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "🎤 Say a place name (e.g., 'SM Mall', 'Ayala Center')"
            )

            // ===== CRITICAL: Silence timeout settings to prevent auto-close =====
            // These require API 23+ but are crucial for preventing early termination

            // Wait 5 seconds of complete silence AFTER speech before ending
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)

            // Wait at least 10 seconds for user to START speaking before timeout
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)

            // Wait 3 seconds after POSSIBLE end of speech (helps with pauses)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000L
            )

            // ===== Additional settings for better recognition =====

            // Request partial results so user sees what's being heard
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Maximum number of results to return
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // Prefer online recognition for better accuracy (requires internet)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)

            // Enable dictation mode on some devices (Samsung, etc.)
            putExtra("android.speech.extra.DICTATION_MODE", true)

            // Secure mode - some devices require this
            putExtra(RecognizerIntent.EXTRA_SECURE, true)
        }

        // Check if there's a speech recognizer available
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(intent, 0)

        if (activities.isNotEmpty()) {
            Toast.makeText(context, "🎤 Speak now...", Toast.LENGTH_LONG).show()
            launcher.launch(intent)
            android.util.Log.d("MapScreen", "Speech recognizer launched successfully")
        } else {
            // No speech recognizer found - show helpful message
            android.util.Log.e("MapScreen", "No speech recognizer activity found")
            Toast.makeText(
                context,
                "Speech recognition not available. Please install Google app.",
                Toast.LENGTH_LONG
            ).show()
            vm.stopVoiceListening()
        }
    } catch (e: android.content.ActivityNotFoundException) {
        android.util.Log.e("MapScreen", "Speech recognizer activity not found: ${e.message}", e)
        Toast.makeText(
            context,
            "Voice input not available. Please install Google app.",
            Toast.LENGTH_LONG
        ).show()
        vm.stopVoiceListening()
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "Failed to launch speech recognizer: ${e.message}", e)
        Toast.makeText(context, "Voice input error: ${e.message}", Toast.LENGTH_SHORT).show()
        vm.stopVoiceListening()
    }
}

/**
 * Launch the navigation voice command recognizer during active navigation
 *
 * Configured for voice commands with shorter timeouts and specific intent
 */
private fun launchNavigationVoiceCommand(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )

            val deviceLocale = java.util.Locale.getDefault()
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLocale.toLanguageTag())

            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "🎤 Voice Command: repeat, pause, resume, distance, or emergency"
            )

            // Shorter timeouts for navigation commands (user expects quick response)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )

            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        val pm = context.packageManager
        val activities = pm.queryIntentActivities(intent, 0)

        if (activities.isNotEmpty()) {
            launcher.launch(intent)
            android.util.Log.d("MapScreen", "Navigation voice command recognizer launched")
        } else {
            android.util.Log.e("MapScreen", "No voice recognizer available for navigation commands")
        }
    } catch (e: Exception) {
        android.util.Log.e(
            "MapScreen",
            "Failed to launch navigation voice command: ${e.message}",
            e
        )
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
