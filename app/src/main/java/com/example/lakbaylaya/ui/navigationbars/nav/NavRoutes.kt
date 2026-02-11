package com.example.lakbaylaya.ui.navigationbars.nav

/**
 * Sealed class representing navigation routes in the app.
 * Using sealed classes provides type safety and prevents routing errors.
 * Follows SOLID principles - Open/Closed: can add screens without modifying navigation logic.
 */
sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object Map : NavRoutes("map")
    data object Route : NavRoutes("route")
    data object Profile : NavRoutes("profile")
}