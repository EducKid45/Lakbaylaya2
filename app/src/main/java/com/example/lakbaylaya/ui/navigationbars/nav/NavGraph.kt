package com.example.lakbaylaya.ui.navigationbars.nav

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.lakbaylaya.ui.screens.home.HomeScreen
import com.example.lakbaylaya.ui.screens.map.MapScreen
import com.example.lakbaylaya.ui.screens.profile.LocationMarkerEditorDialog
import com.example.lakbaylaya.ui.screens.profile.LocationMarkerType
import com.example.lakbaylaya.ui.screens.profile.ProfileEditorScreen
import com.example.lakbaylaya.ui.screens.profile.ProfileScreen
import com.example.lakbaylaya.ui.screens.route.RoutesScreen
import com.example.lakbaylaya.ui.screens.route.SavedRoute
import com.example.lakbaylaya.ui.screens.route.SavedPlace
import com.example.lakbaylaya.ui.screens.setting.SettingsScreen
import com.example.lakbaylaya.ui.screens.help.HelpScreen
import com.example.lakbaylaya.ui.screens.help.FullGuideScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lakbaylaya.data.repository.RoutesRepositoryRoom
import com.example.lakbaylaya.ui.screens.route.RoutesViewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.example.lakbaylaya.ui.screens.profile.ProfileViewModel
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Navigation graph for the app.
 * Defines all composable screens and their routes.
 *
 * @param navController The navigation controller for managing app navigation
 * @param modifier Modifier to apply to the NavHost (e.g., for padding)
 * @param onBottomNavVisibilityChange Callback to control bottom navigation visibility
 *
 * Architecture: Clean separation of navigation from business logic.
 * Each screen is responsible only for UI, ViewModels handle state.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onBottomNavVisibilityChange: ((Boolean) -> Unit)? = null,
    bluetoothViewModel: com.example.lakbaylaya.bluetooth.BluetoothViewModel? = null,
    globalVoiceViewModel: com.example.lakbaylaya.voice.GlobalVoiceViewModel? = null,
    // Shared RoutesViewModel — same instance used by voice providers so Room updates
    // are immediately visible to voice commands without any restart.
    routesViewModel: com.example.lakbaylaya.ui.screens.route.RoutesViewModel? = null
) {
    val ctx = LocalContext.current

    fun openMapPreserveStack() {
        android.util.Log.d("NavGraph", "openMapPreserveStack called")
        // Clear any existing Map entries and push a fresh one on top of Home
        navController.navigate(NavRoutes.Map.route) {
            popUpTo(NavRoutes.Home.route) {
                inclusive = false  // Keep Home in the stack
            }
            // No launchSingleTop, no restoreState - always fresh
        }
    }

    // Helper: navigate to Map with query params; skip if identical top entry exists
    fun navigateToMapSafely(
        destLat: Double,
        destLon: Double,
        name: String,
        autoStart: Boolean,
        selectOnly: Boolean = false
    ) {
        val topEntry = navController.currentBackStackEntry
        val topRoute = topEntry?.destination?.route
        android.util.Log.d("NavGraph", "navigateToMapSafely - from=$topRoute lat=$destLat lon=$destLon name=$name auto=$autoStart")

        // If Map is already the top destination and has the identical params, skip navigating again
        if (topRoute != null && topRoute.startsWith(NavRoutes.Map.route)) {
            val topArgs = topEntry.arguments
            val topLat = topArgs?.getString("start_nav_dest_lat")?.toDoubleOrNull() ?: -999.0
            val topLon = topArgs?.getString("start_nav_dest_lon")?.toDoubleOrNull() ?: -999.0
            val topName = topArgs?.getString("start_nav_name") ?: ""
            val topAuto = topArgs?.getBoolean("start_nav_auto") ?: false
            val topSelectOnly = topArgs?.getBoolean("start_nav_select_only") ?: false
            val eps = 1e-6
            if (kotlin.math.abs(topLat - destLat) < eps && kotlin.math.abs(topLon - destLon) < eps
                && topName == name && topAuto == autoStart && topSelectOnly == selectOnly) {
                android.util.Log.d("NavGraph", "Skipping duplicate navigate to Map (already top with same params)")
                return
            }
        }

        val safeName = Uri.encode(name)
        val targetRoute = "${NavRoutes.Map.route}?start_nav_dest_lat=$destLat" +
                "&start_nav_dest_lon=$destLon" +
                "&start_nav_name=$safeName" +
                "&start_nav_auto=$autoStart" +
                "&start_nav_select_only=$selectOnly"

        // CRITICAL FIX: Always clear any existing Map entries from the back stack to prevent
        // duplicate Map instances. Pop to Home (inclusive=false keeps Home), then push Map.
        // This ensures a clean stack: [Home, Map] with no stale Map entries lingering below.
        navController.navigate(targetRoute) {
            popUpTo(NavRoutes.Home.route) {
                inclusive = false  // Keep Home in the stack
            }
            // Do NOT use launchSingleTop here - we always want a fresh Map instance
            // Do NOT use restoreState - we want clean state, not potentially corrupted saved state
        }
        android.util.Log.d("NavGraph", "After navigate - new stack top=${navController.currentBackStackEntry?.destination?.route}")
    }

    NavHost(navController = navController, startDestination = NavRoutes.Home.route, modifier = modifier) {
        // Home
        composable(NavRoutes.Home.route) {
            HomeScreen(
                bluetoothViewModel = bluetoothViewModel ?: viewModel(),
                globalVoiceViewModel = globalVoiceViewModel,
                onNavigateToMap = { openMapPreserveStack() },
                onNavigateToRoutes = {
                    navController.navigate(NavRoutes.Route.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Map accepts optional query params (strings for lat/lon parsed inside)
        composable(
            route = "${NavRoutes.Map.route}?start_nav_dest_lat={start_nav_dest_lat}&start_nav_dest_lon={start_nav_dest_lon}&start_nav_name={start_nav_name}&start_nav_auto={start_nav_auto}&start_nav_select_only={start_nav_select_only}",
            arguments = listOf(
                navArgument("start_nav_dest_lat") { type = NavType.StringType; defaultValue = "" },
                navArgument("start_nav_dest_lon") { type = NavType.StringType; defaultValue = "" },
                navArgument("start_nav_name") { type = NavType.StringType; defaultValue = "" },
                navArgument("start_nav_auto") { type = NavType.BoolType; defaultValue = false },
                navArgument("start_nav_select_only") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val lat = args?.getString("start_nav_dest_lat")?.toDoubleOrNull() ?: -999.0
            val lon = args?.getString("start_nav_dest_lon")?.toDoubleOrNull() ?: -999.0
            val name = args?.getString("start_nav_name") ?: ""
            val auto = args?.getBoolean("start_nav_auto") ?: false
            val selectOnly = args?.getBoolean("start_nav_select_only") ?: false
            val startReq = if (lat != -999.0 && lon != -999.0) MapStartNavigationRequest(lat, lon, name, auto, selectOnly) else null

            // Pass navController so MapScreen can call navigateUp() — ensures correct back behavior
            MapScreen(
                navController = navController,
                onBottomNavVisibilityChange = onBottomNavVisibilityChange,
                startNavigationRequest = startReq,
                globalVoiceViewModel = globalVoiceViewModel
            )
        }

        // Routes (Saved Routes)
        composable(NavRoutes.Route.route) { backStackEntry ->
            // Prefer the shared routesViewModel passed from MainActivity — it's the same
            // instance the voice providers read, so any newly saved route/place is
            // immediately visible to voice commands without restart.
            val vm: com.example.lakbaylaya.ui.screens.route.RoutesViewModel = routesViewModel
                ?: run {
                    val routesRepo  = remember { RoutesRepositoryRoom(ctx.applicationContext) }
                    val placesRepo  = remember { com.example.lakbaylaya.data.repository.SavedPlaceRepositoryImpl(ctx.applicationContext) }
                    val markersRepo = remember { com.example.lakbaylaya.data.repository.CustomMarkerRepositoryImpl(ctx.applicationContext) }
                    val factory     = remember(routesRepo, placesRepo, markersRepo) { RoutesViewModelFactory(routesRepo, placesRepo, markersRepo, ctx.applicationContext) }
                    viewModel(viewModelStoreOwner = backStackEntry, factory = factory)
                }

            // Read optional savedStateHandle key set by MapScreen when saving a route
            val openRouteId: String? = backStackEntry.savedStateHandle.get<String>("open_saved_route_id")
            // Clear it immediately so it doesn't reopen on subsequent navigations
            if (openRouteId != null) {
                backStackEntry.savedStateHandle.remove<String>("open_saved_route_id")
            }

            RoutesScreen(
                viewModel = vm,
                onPreviewRoute = { route: SavedRoute ->
                    val destLat = if (route.endLatitude != 0.0) route.endLatitude else route.routeSteps?.lastOrNull()?.latitude ?: 0.0
                    val destLon = if (route.endLongitude != 0.0) route.endLongitude else route.routeSteps?.lastOrNull()?.longitude ?: 0.0
                    if (destLat == 0.0 && destLon == 0.0) {
                        android.widget.Toast.makeText(ctx, "Route has no destination coordinates", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        navigateToMapSafely(destLat, destLon, route.name, false)
                    }
                },
                onStartNavigation = { route: SavedRoute ->
                    val destLat = if (route.endLatitude != 0.0) route.endLatitude else route.routeSteps?.lastOrNull()?.latitude ?: 0.0
                    val destLon = if (route.endLongitude != 0.0) route.endLongitude else route.routeSteps?.lastOrNull()?.longitude ?: 0.0
                    if (destLat == 0.0 && destLon == 0.0) {
                        android.widget.Toast.makeText(ctx, "Cannot start navigation: route destination unknown", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        navigateToMapSafely(destLat, destLon, route.name, true)
                    }
                },
                onNavigateToMap = { place: SavedPlace ->
                    navigateToMapSafely(place.latitude, place.longitude, place.placeName, false, selectOnly = true)
                },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                openRouteId = openRouteId
            )
        }

        // ── Profile ──────────────────────────────────────────────────────────
        composable(NavRoutes.Profile.route) {
            // Share the same ProfileViewModel instance across profile sub-screens
            val profileVm: ProfileViewModel = viewModel()
            ProfileScreen(viewModel = profileVm)
        }

        composable(NavRoutes.ProfileEditor.route) {
            val profileVm: ProfileViewModel = viewModel()
            ProfileEditorScreen(
                viewModel = profileVm,
                onBack = { navController.popBackStack() },
                onOpenHomeMarker = { navController.navigate("profile_home_marker") },
                onOpenWorkMarker = { navController.navigate("profile_work_marker") }
            )
        }

        composable("profile_home_marker") {
            val profileVm: ProfileViewModel = viewModel()
            val profile by profileVm.uiState.collectAsState()
            LocationMarkerEditorDialog(
                type = LocationMarkerType.HOME,
                initialLat = if (profile.userProfile.homeLat != 0.0) profile.userProfile.homeLat else 16.2333,
                initialLon = if (profile.userProfile.homeLon != 0.0) profile.userProfile.homeLon else 120.4833,
                viewModel = profileVm,
                onDismiss = { navController.popBackStack() },
                onSaved = { _, _, _ -> navController.popBackStack() }
            )
        }

        composable("profile_work_marker") {
            val profileVm: ProfileViewModel = viewModel()
            val profile by profileVm.uiState.collectAsState()
            LocationMarkerEditorDialog(
                type = LocationMarkerType.WORK,
                initialLat = if (profile.userProfile.workLat != 0.0) profile.userProfile.workLat else 16.2333,
                initialLon = if (profile.userProfile.workLon != 0.0) profile.userProfile.workLon else 120.4833,
                viewModel = profileVm,
                onDismiss = { navController.popBackStack() },
                onSaved = { _, _, _ -> navController.popBackStack() }
            )
        }

        // Help / Voice Guide
        composable(NavRoutes.Help.route) {
            HelpScreen(
                onViewFullGuide = { navController.navigate("help_full_guide") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("help_full_guide") {
            FullGuideScreen()
        }

        // Settings
        composable(NavRoutes.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

// Container for Map start request
data class MapStartNavigationRequest(
    val destLat: Double,
    val destLon: Double,
    val name: String = "",
    val autoStart: Boolean = false,
    /**
     * When true the Map screen will only select / preview the place (show marker +
     * bottom-sheet) without entering Direction mode.  Used when navigating from
     * the Saved Places list so no search-bar interaction is required.
     */
    val selectOnly: Boolean = false
)
