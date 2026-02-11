package com.example.lakbaylaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.lakbaylaya.ui.navigationbars.top.TopBar
import com.example.lakbaylaya.ui.theme.LakbaylayaTheme
import com.example.lakbaylaya.ui.navigationbars.viewmodel.AppViewModel
import com.example.lakbaylaya.ui.screens.map.maplibre.MapLibreManager

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre SDK for native map rendering
        MapLibreManager.initialize(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            LakbaylayaTheme {
                MainApp()
            }
        }
    }
}

/**
 * Main app composable
 * Wires together all components with state management
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(
    appViewModel: AppViewModel = viewModel()
) {
    // Navigation controller
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Observe app state from ViewModel
    val isBluetoothEnabled by appViewModel.isBluetoothEnabled.collectAsState()
    val notificationCount by appViewModel.notificationCount.collectAsState()

    // State for bottom navigation visibility (hidden when search is active)
    var isBottomNavVisible by remember { mutableStateOf(true) }

    // Check if dark theme is active
    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Hide TopBar on Map screen so search bar can be at very top
            // Use key to prevent animation when switching screens
            key(currentRoute) {
                if (currentRoute != "map") {
                    TopBar(
                        isBluetoothEnabled = isBluetoothEnabled,
                        notificationCount = notificationCount,
                        isDarkTheme = isDarkTheme,
                        onSettingsClick = { appViewModel.onSettingsClick() },
                        onEmergencyClick = { appViewModel.onEmergencyClick() },
                        onBluetoothClick = { appViewModel.toggleBluetooth() },
                        onNotificationsClick = { appViewModel.onNotificationsClick() }
                    )
                }
            }
        },
        bottomBar = {
            // Hide bottom navigation when search is active
            if (isBottomNavVisible) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid building a large back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when navigating back
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Screen content with padding to avoid overlap with bars
        NavGraph(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onBottomNavVisibilityChange = { _ ->
            }
        )
    }
}
