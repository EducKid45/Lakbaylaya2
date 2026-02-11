package com.example.lakbaylaya.ui.navigationbars.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.lakbaylaya.ui.screens.home.HomeScreen
import com.example.lakbaylaya.ui.screens.map.MapScreen
import com.example.lakbaylaya.ui.screens.profile.ProfileScreen
import com.example.lakbaylaya.ui.screens.route.RouteScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
    val animDuration = 220

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = modifier
    ) {
        composable(
            NavRoutes.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { 50 }, animationSpec = tween(animDuration)) },
            exitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popExitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { 50 }, animationSpec = tween(animDuration)) }
        ) {
            HomeScreen()
        }

        composable(
            NavRoutes.Map.route,
            enterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { 50 }, animationSpec = tween(animDuration)) },
            exitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popExitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { 50 }, animationSpec = tween(animDuration)) }
        ) {
            MapScreen(
                onBottomNavVisibilityChange = onBottomNavVisibilityChange
            )
        }

        composable(
            NavRoutes.Route.route,
            enterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { 50 }, animationSpec = tween(animDuration)) },
            exitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popExitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { 50 }, animationSpec = tween(animDuration)) }
        ) {
            RouteScreen()
        }

        composable(
            NavRoutes.Profile.route,
            enterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { 50 }, animationSpec = tween(animDuration)) },
            exitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) + slideInHorizontally(initialOffsetX = { -50 }, animationSpec = tween(animDuration)) },
            popExitTransition = { fadeOut(animationSpec = tween(animDuration)) + slideOutHorizontally(targetOffsetX = { 50 }, animationSpec = tween(animDuration)) }
        ) {
            ProfileScreen()
        }
    }
}
