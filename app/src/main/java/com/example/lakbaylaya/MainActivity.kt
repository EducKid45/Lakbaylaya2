package com.example.lakbaylaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.example.lakbaylaya.emergency.EmergencyManager

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

    // Permission request launcher for Bluetooth
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Forward permission results to ViewModel for auto-resume
        PermissionResultHandler.setResults(results)
    }

    // Emergency manager instance (handles permissions, location, SMS sending)
    private lateinit var emergencyManager: EmergencyManager

    companion object {
        // Replace with your predefined emergency contact number (E.164 recommended)
        const val EMERGENCY_NUMBER = "+1234567890"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre SDK for native map rendering
        MapLibreManager.initialize(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Register permission launcher for emergency flow
        val emergencyPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            // Forward results to emergency manager (will be initialized below)
            emergencyManager.onPermissionResults(results)
        }

        // Initialize EmergencyManager - provide the emergency permission launcher
        // Initialize EmergencyManager (on-device SMS). Pass the permission launcher.
        emergencyManager = EmergencyManager(this, EMERGENCY_NUMBER, emergencyPermissionLauncher)

        // Get simple preferences to track whether onboarding was completed
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        setContent {
            LakbaylayaTheme {
                // Determine initial onboarding state from prefs
                var onboardingState by remember {
                    mutableStateOf(
                        if (prefs.getBoolean(
                                "onboarding_complete",
                                false
                            )
                        ) OnboardingState.NONE else OnboardingState.WELCOME
                    )
                }

                when (onboardingState) {
                    OnboardingState.WELCOME -> {
                        WelcomeOnboardingScreen(
                            onSetUp = {
                                // Move to profile setup step
                                onboardingState = OnboardingState.PROFILE
                            },
                            onSkip = {
                                // Mark onboarding complete and go to main app
                                prefs.edit { putBoolean("onboarding_complete", true) }
                                onboardingState = OnboardingState.NONE
                            }
                        )
                    }

                    OnboardingState.PROFILE -> {
                        ProfileSetupOnboardingScreen(
                            onFinish = {
                                // Mark onboarding complete and show main app
                                prefs.edit { putBoolean("onboarding_complete", true) }
                                onboardingState = OnboardingState.NONE
                            },
                            onSetLocation = {
                                // No-op for now in this wiring; location flow can be implemented later
                            }
                        )
                    }

                    OnboardingState.NONE -> {
                        MainAppHost(
                            activity = this@MainActivity,
                            permissionLauncher = permissionLauncher,
                            onEmergency = { emergencyManager.sendEmergencySms() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainAppHost(
    activity: android.app.Activity? = null,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null,
    // New callback for emergency one-tap
    onEmergency: (() -> Unit)? = null
) {
    // Obtain Application from the Compose LocalContext to ensure non-null Application
    val app =
        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application

    // Create AppViewModel with Application for Bluetooth initialization
    val appViewModel: AppViewModel = viewModel(
        factory = com.example.lakbaylaya.ui.navigationbars.viewmodel.AppViewModelFactory(app)
    )

    // Set permission launcher for Bluetooth operations
    permissionLauncher?.let {
        appViewModel.setBluetoothPermissionLauncher(it)
        // Provide the Activity reference to ViewModel so it can launch system dialogs
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
    val notificationCount by appViewModel.notificationCount.collectAsState()

    // State for bottom navigation visibility (hidden when search is active)
    var isBottomNavVisible by remember { mutableStateOf(true) }

    // Check if dark theme is active
    val isDarkTheme = isSystemInDarkTheme()

    // Determine if we are on the Settings screen
    val isSettingsScreen = currentRoute == NavRoutes.Settings.route
    // Determine bottom nav visibility directly
    isBottomNavVisible = !isSettingsScreen

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            key(currentRoute) {
                if (currentRoute != "map") {
                    TopBar(
                        isBluetoothEnabled = isBluetoothEnabled,
                        bluetoothState = bluetoothState,
                        notificationCount = notificationCount,
                        isDarkTheme = isDarkTheme,
                        onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                        onBackClick = { navController.popBackStack() },
                        onEmergencyClick = {
                            // Prefer one-tap EmergencyManager flow when provided, fall back to ViewModel
                            if (onEmergency != null) onEmergency() else appViewModel.onEmergencyClick()
                        },
                        onBluetoothClick = { appViewModel.toggleBluetooth() },
                        onNotificationsClick = { appViewModel.onNotificationsClick() },
                        showBackIcon = isSettingsScreen,
                        showActions = !isSettingsScreen, // Hide actions on Settings
                        title = if (isSettingsScreen) "Settings" else null // Show "Settings" title
                    )
                }
            }
        },
        bottomBar = {
            if (isBottomNavVisible) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
                .padding(paddingValues)
            // onBottomNavVisibilityChange omitted; bottom nav visibility is derived from route
        )
    }
}
