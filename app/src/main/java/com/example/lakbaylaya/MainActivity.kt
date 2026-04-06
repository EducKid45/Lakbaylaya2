package com.example.lakbaylaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lakbaylaya.bluetooth.ui.BluetoothDeviceBottomSheet
import com.example.lakbaylaya.ui.navigationbars.bottom.BottomNavBar
import com.example.lakbaylaya.ui.navigationbars.nav.NavGraph
import com.example.lakbaylaya.ui.navigationbars.nav.NavRoutes
import com.example.lakbaylaya.ui.navigationbars.top.TopBar
import com.example.lakbaylaya.ui.theme.LakbaylayaTheme
import com.example.lakbaylaya.ui.navigationbars.viewmodel.AppViewModel
import com.example.lakbaylaya.maplibre.manager.MapLibreManager
import com.example.lakbaylaya.ui.screens.onboarding.WelcomeOnboardingScreen
import com.example.lakbaylaya.ui.screens.onboarding.ProfileSetupOnboardingScreen
import androidx.core.content.edit
import com.example.lakbaylaya.emergency.EmergencyHandler
import com.example.lakbaylaya.emergency.EmergencyManager
import com.example.lakbaylaya.emergency.EmergencySmsGateway
import com.example.lakbaylaya.emergency.SemaphoreSmsSender
import android.content.pm.PackageManager
import android.util.Log
import com.example.lakbaylaya.utils.PermissionResultHandler
import com.example.lakbaylaya.voice.AppVoiceCommand
import com.example.lakbaylaya.voice.GlobalVoiceViewModel
import com.example.lakbaylaya.voice.VoiceMapEvent
import com.example.lakbaylaya.data.repository.RoutesRepositoryRoom
import com.example.lakbaylaya.data.repository.SavedPlaceRepositoryImpl
import com.example.lakbaylaya.data.repository.CustomMarkerRepositoryImpl
import com.example.lakbaylaya.data.repository.UserProfileRepository
import com.example.lakbaylaya.ui.screens.route.RoutesViewModelFactory
import kotlinx.coroutines.runBlocking

/**
 * MainActivity - Main entry point of the app
 *
 * Responsibilities:
 * - Hosts Scaffold with TopBar and BottomNavBar
 * - Holds NavController for navigation between screens
 * - Observes state from AppViewModel
 * - Applies theme, colors, typography
 * - Applies WindowInsets padding to avoid overlap
 * - Wiring point for dependency injection
 * - Initializes MapLibre SDK
 *
 * Architecture:
 * - MVVM pattern
 * - Composition over inheritance
 * - Single Activity architecture with Jetpack Compose
 * - Navigation handled by NavController
 *
 * UI Structure:
 * ┌─────────────────────┐
 * │      TopBar         │ ← Settings, App Name, Emergency, Bluetooth, Notifications
 * ├─────────────────────┤
 * │                     │
 * │   Screen Content    │ ← Home, Map, Route, Profile (scrollable)
 * │                     │
 * ├─────────────────────┤
 * │   BottomNavBar      │ ← Home, Map, Route, Profile
 * └─────────────────────┘
 *
 * Accessibility:
 * - Edge-to-edge display with proper insets
 * - System bars respect theme
 * - TalkBack navigation support
 * - Respects system reduce motion preference
 */
class MainActivity : ComponentActivity() {

    private enum class OnboardingState { NONE, WELCOME, PROFILE }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> PermissionResultHandler.setResults(results) }

    private lateinit var emergencyHandler: EmergencyHandler

    // Voice ViewModel owned by the Activity — survives all screen navigation
    private lateinit var voiceViewModel: GlobalVoiceViewModel

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibreManager.initialize(this)
        enableEdgeToEdge(
            statusBarStyle     = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        // Ensure dark icons on light (white/transparent) system bars
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true

        // Obtain the voice VM at Activity level so it is never tied to a screen
        voiceViewModel = androidx.lifecycle.ViewModelProvider(this)[GlobalVoiceViewModel::class.java]

        val emergencyPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (::emergencyHandler.isInitialized) emergencyHandler.onPermissionResults(results)
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val useGateway = prefs.getBoolean("use_sms_gateway", false)

        // Read the emergency contact number from the user's saved profile at send-time
        val profileRepo = UserProfileRepository.create(this)
        val emergencyNumberProvider: () -> String = {
            runBlocking { profileRepo.getProfile()?.emergencyContactNumber?.trim() ?: "" }
        }

        if (useGateway) {
            val apiKey = try {
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                appInfo.metaData?.getString("SEMAPHORE_API_KEY") ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read SEMAPHORE_API_KEY: ${e.message}"); ""
            }
            emergencyHandler = if (apiKey.isEmpty()) {
                Log.w(TAG, "SMS gateway key missing — falling back to device flow")
                EmergencyManager(this, emergencyNumberProvider, emergencyPermissionLauncher)
            } else {
                EmergencySmsGateway(this, emergencyNumberProvider, SemaphoreSmsSender(apiKey), emergencyPermissionLauncher)
            }
        } else {
            emergencyHandler = EmergencyManager(this, emergencyNumberProvider, emergencyPermissionLauncher)
        }

        setContent {
            LakbaylayaTheme {
                var onboardingState by remember {
                    mutableStateOf(
                        if (prefs.getBoolean("onboarding_complete", false))
                            OnboardingState.NONE else OnboardingState.WELCOME
                    )
                }
                when (onboardingState) {
                    OnboardingState.WELCOME -> WelcomeOnboardingScreen(
                        onSetUp = { onboardingState = OnboardingState.PROFILE },
                        onSkip = {
                            prefs.edit { putBoolean("onboarding_complete", true) }
                            onboardingState = OnboardingState.NONE
                        }
                    )
                    OnboardingState.PROFILE -> ProfileSetupOnboardingScreen(
                        onFinish = {
                            prefs.edit { putBoolean("onboarding_complete", true) }
                            onboardingState = OnboardingState.NONE
                        },
                        onSetLocation = {}
                    )
                    OnboardingState.NONE -> MainAppHost(
                        activity = this@MainActivity,
                        permissionLauncher = permissionLauncher,
                        onEmergency = { emergencyHandler.sendEmergencySms() },
                        globalVoiceViewModel = voiceViewModel
                    )
                }
            }
        }
    }

    // ── Voice engine lifecycle — pure Android, zero Compose involvement ───────
    // onResume fires on: first launch, return from background, screen unlock.
    // It does NOT fire on in-app navigation — the voice engine keeps running
    // uninterrupted when the user switches screens inside the app.
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume → startWakeListening()")
        voiceViewModel.startWakeListening()
    }

    // onStop fires only when the app truly goes to background / screen off.
    // In-app navigation never triggers onStop.
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop → stopAll()")
        voiceViewModel.stopAll()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainAppHost(
    activity: android.app.Activity? = null,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null,
    onEmergency: (() -> Unit)? = null,
    // Passed from MainActivity — already lifecycle-managed by onResume/onStop.
    // Do NOT create or lifecycle-manage this inside Compose.
    globalVoiceViewModel: GlobalVoiceViewModel
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application

    val appViewModel: AppViewModel = viewModel(
        factory = com.example.lakbaylaya.ui.navigationbars.viewmodel.AppViewModelFactory(app)
    )

    // Wire live saved-routes / saved-places into GlobalVoiceViewModel so
    // NavigateDialogEngine always reads fresh Room data.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val routesVmForVoice: com.example.lakbaylaya.ui.screens.route.RoutesViewModel = viewModel(
        factory = remember(ctx) {
            RoutesViewModelFactory(
                routesRepo  = RoutesRepositoryRoom(ctx.applicationContext),
                placesRepo  = SavedPlaceRepositoryImpl(ctx.applicationContext),
                markersRepo = CustomMarkerRepositoryImpl(ctx.applicationContext),
                context     = ctx.applicationContext
            )
        }
    )
    val routesUiState by routesVmForVoice.uiState.collectAsState()

    // SideEffect runs on EVERY recomposition — so whenever Room emits new routes/places,
    // Compose recomposes with the new routesUiState and this block re-assigns the lambdas
    // with the latest data captured. LaunchedEffect(globalVoiceViewModel) only ran ONCE
    // and captured a stale snapshot — that's why newly saved routes/places were invisible
    // to voice commands until the app was restarted.
    SideEffect {
        globalVoiceViewModel.savedRoutesProvider    = { routesUiState.savedRoutes }
        globalVoiceViewModel.savedPlacesProvider    = { routesUiState.savedPlaces }
        globalVoiceViewModel.savedLocationsProvider = { routesUiState.savedLocations }
    }

    permissionLauncher?.let {
        appViewModel.setBluetoothPermissionLauncher(it)
        appViewModel.setActivity(activity)
    }

    // Moved MainApp body here to keep onboarding wiring clean
    // Navigation controller
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Observe app state from ViewModel
    val isBluetoothEnabled by appViewModel.isBluetoothEnabled.collectAsState()
    val bluetoothState by appViewModel.bluetoothState.collectAsState()
    val showBluetoothSheet by appViewModel.showBluetoothSheet.collectAsState()
    val discoveredDevices by appViewModel.discoveredDevices.collectAsState()

    // State for bottom navigation visibility (hidden when search is active)
    var isBottomNavVisible by remember { mutableStateOf(true) }

    // Determine if we are on the Settings screen
    val isSettingsScreen = currentRoute == NavRoutes.Settings.route
    // Determine if we're on the Map screen (match prefix so query params don't break it)
    val isMapScreen = currentRoute?.startsWith(NavRoutes.Map.route) == true
    // Determine if we're on the Help or FullGuide screen
    val isHelpScreen = currentRoute == NavRoutes.Help.route || currentRoute == "help_full_guide"
    // Determine bottom nav visibility directly
    isBottomNavVisible = !isSettingsScreen

    // ── Global voice: navigation events ──────────────────────────────────────
    // Collect navigation events from GlobalVoiceViewModel and drive navController
    LaunchedEffect(globalVoiceViewModel) {
        globalVoiceViewModel.navigationEvent.collect { targetRoute ->
            Log.d("MainAppHost", "Voice navigation → $targetRoute")
            when (targetRoute) {
                NavRoutes.Home.route -> {
                    // Pop back to Home if it exists, otherwise navigate fresh
                    val popped = navController.popBackStack(NavRoutes.Home.route, false)
                    if (!popped) navController.navigate(NavRoutes.Home.route) {
                        launchSingleTop = true
                    }
                }
                NavRoutes.Map.route -> {
                    // Keep Home as root, push Map on top
                    navController.navigate(NavRoutes.Map.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                else -> {
                    // Route / Profile / Settings — keep Home as root, replace everything above it
                    navController.navigate(targetRoute) {
                        popUpTo(NavRoutes.Home.route) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    // ── Global voice: action events ───────────────────────────────────────────
    // Handle non-navigation feature commands emitted by GlobalVoiceViewModel
    LaunchedEffect(globalVoiceViewModel) {
        globalVoiceViewModel.actionEvent.collect { action ->
            Log.d("MainAppHost", "Voice action → ${action.commandName}")
            when (action) {
                AppVoiceCommand.ActionCommand.CheckBluetooth ->
                    appViewModel.onBluetoothIconClick()
                AppVoiceCommand.ActionCommand.SendEmergency ->
                    if (onEmergency != null) onEmergency() else appViewModel.onEmergencyClick()
                AppVoiceCommand.ActionCommand.CallEmergencyContact ->
                    if (onEmergency != null) onEmergency() else appViewModel.onEmergencyClick()
                else -> { /* forwarded to screens via SharedFlow if needed */ }
            }
        }
    }

    // ── Global voice: map events (navigate dialog) ────────────────────────────
    // OpenSearchMap: navigate to map and activate search bar.
    // StartSavedRoute: navigate to map with real coords and auto-start navigation.
    // Search queries are handled entirely via pendingVoiceSearch StateFlow in MapScreen.
    LaunchedEffect(globalVoiceViewModel) {
        globalVoiceViewModel.voiceMapEvent.collect { event ->
            Log.d("MainAppHost", "VoiceMapEvent → $event")
            when (event) {
                is VoiceMapEvent.OpenSearchMap -> {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    val isMapVisible = currentRoute?.startsWith(NavRoutes.Map.route) == true
                    if (isMapVisible) {
                        // Map already on top — just activate search bar via savedStateHandle
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("start_nav_name", "__open_search__")
                        Log.d("MainAppHost", "OpenSearchMap: map already visible, updating savedStateHandle")
                    } else {
                        // Navigate to Map keeping Home as root in backstack
                        navController.navigate(
                            "${NavRoutes.Map.route}?start_nav_name=__open_search__" +
                            "&start_nav_auto=false&start_nav_select_only=false"
                        ) {
                            // Pop back to Home (but keep it) so Map sits on top of Home
                            popUpTo(NavRoutes.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        Log.d("MainAppHost", "OpenSearchMap: navigated to map over Home")
                    }
                }
                is VoiceMapEvent.StartSavedRoute -> {
                    val safeName = android.net.Uri.encode(event.name)
                    val mapRoute = "${NavRoutes.Map.route}?start_nav_dest_lat=${event.destLat}" +
                                "&start_nav_dest_lon=${event.destLon}" +
                                "&start_nav_name=$safeName" +
                                "&start_nav_auto=true" +
                                "&start_nav_select_only=false"
                    // Always keep Home as the root — pop everything above Home, push Map
                    navController.navigate(mapRoute) {
                        popUpTo(NavRoutes.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                    Log.d("MainAppHost", "StartSavedRoute: navigated to map over Home")
                }
            }
        }
    }

    // Bluetooth ModalBottomSheet overlaying any screen (shown when BT icon tapped)
    if (showBluetoothSheet) {
        BluetoothDeviceBottomSheet(
            bluetoothState = bluetoothState,
            devices = discoveredDevices,
            onDismiss = { appViewModel.dismissBluetoothSheet() },
            onToggleDevice = { address -> appViewModel.toggleDeviceConnection(address) },
            onRescan = { appViewModel.rescanBluetooth() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            key(currentRoute) {
                // Hide TopBar when on any Map route (including with query params)
                if (!isMapScreen) {
                    TopBar(
                        isBluetoothEnabled = isBluetoothEnabled,
                        bluetoothState = bluetoothState,
                        isDarkTheme = false,
                        onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                        onBackClick = { navController.popBackStack() },
                        onEmergencyClick = {
                            // Prefer one-tap EmergencyManager flow when provided, fall back to ViewModel
                            if (onEmergency != null) onEmergency() else appViewModel.onEmergencyClick()
                        },
                        onBluetoothClick = { appViewModel.onBluetoothIconClick() },
                        onHelpClick = {
                            navController.navigate(NavRoutes.Help.route) {
                                launchSingleTop = true
                            }
                        },
                        showBackIcon = isSettingsScreen || isHelpScreen,
                        showActions = !isSettingsScreen && !isHelpScreen,
                        title = when {
                            isSettingsScreen -> "Settings"
                            currentRoute == NavRoutes.Help.route -> "Help & Voice Guide"
                            currentRoute == "help_full_guide" -> "Full Guide"
                            else -> null
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (isBottomNavVisible) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Defensive navigation with logging and explicit popBackStack for Home
                        Log.d("MainActivity", "BottomNav requested: route=$route top=${navController.currentBackStackEntry?.destination?.route}")
                        when (route) {
                            NavRoutes.Home.route -> {
                                // Try to pop back to an existing Home in the backstack first so state is preserved.
                                val popped = navController.popBackStack(NavRoutes.Home.route, inclusive = false)
                                Log.d("MainActivity", "Attempted popBackStack(Home) -> popped=$popped topAfterPop=${navController.currentBackStackEntry?.destination?.route}")
                                if (!popped) {
                                    // Home not in backstack (rare) - navigate to it explicitly
                                    navController.navigate(NavRoutes.Home.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    Log.d("MainActivity", "Navigated to Home explicitly; top=${navController.currentBackStackEntry?.destination?.route}")
                                }
                            }

                            NavRoutes.Map.route -> {
                                Log.d("MainActivity", "Navigating to Map (clearing any existing Map entries)")
                                // Clear any existing Map entries and push fresh Map on top of Home
                                // This prevents multiple Map entries in the back stack
                                navController.navigate(NavRoutes.Map.route) {
                                    popUpTo(NavRoutes.Home.route) {
                                        inclusive = false  // Keep Home in stack
                                    }
                                    // No launchSingleTop, no restoreState - always fresh Map
                                }
                                Log.d("MainActivity", "After navigate(Map) top=${navController.currentBackStackEntry?.destination?.route}")
                            }

                            else -> {
                                Log.d("MainActivity", "Navigating to $route with popUpTo(start)")
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                Log.d("MainActivity", "After navigate($route) top=${navController.currentBackStackEntry?.destination?.route}")
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            bluetoothViewModel = appViewModel.bluetoothViewModel,
            globalVoiceViewModel = globalVoiceViewModel,
            routesViewModel = routesVmForVoice  // shared — voice + UI read the same Room subscription
        )
    }
}
