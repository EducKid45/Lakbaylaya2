package com.example.lakbaylaya.ui.navigationbars.nav

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.lakbaylaya.ui.screens.home.HomeScreen
import com.example.lakbaylaya.ui.screens.map.MapScreen
import com.example.lakbaylaya.ui.screens.profile.ProfileScreen
import com.example.lakbaylaya.ui.screens.route.RoutesScreen
import com.example.lakbaylaya.ui.screens.setting.SettingsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lakbaylaya.data.repository.RoutesRepositoryRoom
import com.example.lakbaylaya.ui.screens.route.RoutesViewModelFactory
import androidx.compose.ui.platform.LocalContext

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
    onBottomNavVisibilityChange: ((Boolean) -> Unit)? = null
) {

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = modifier
    ) {
        // Home
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(NavRoutes.Map.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // Map
        composable(NavRoutes.Map.route) {
            MapScreen(
                onBottomNavVisibilityChange = onBottomNavVisibilityChange
            )
        }

        // Routes (Saved Routes)
        composable(NavRoutes.Route.route) { backStackEntry ->
            // Use remember so repository and factory are not recreated on every recomposition
            val context = LocalContext.current
            val repo = remember { RoutesRepositoryRoom(context.applicationContext) }
            val factory = remember(repo) { RoutesViewModelFactory(repo) }

            // Scope ViewModel to this nav backStackEntry so it's lifecycle-aware and cleared on pop
            val viewModel: com.example.lakbaylaya.ui.screens.route.RoutesViewModel =
                viewModel(viewModelStoreOwner = backStackEntry, factory = factory)

            RoutesScreen(
                viewModel = viewModel,
                onPreviewRoute = { /* TODO: Implement preview behaviour - navigate to map and show preview */ },
                onStartNavigation = { /* TODO: Implement start navigation behaviour - pass route to Map feature */ },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // Profile
        composable(NavRoutes.Profile.route) {
            // `ProfileScreen` no longer takes an onNavigateToSettings parameter; call with the default signature.
            ProfileScreen()
        }

        // Settings
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
