package com.example.lakbaylaya.ui.screens.map.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Location permission state and handler
 *
 * Manages runtime location permission requests and provides current permission state.
 * Handles both FINE and COARSE location permissions for GPS functionality.
 *
 * @property hasPermission Whether location permission is granted
 * @property requestPermission Function to request location permission
 */
data class LocationPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

/**
 * Composable function to remember and manage location permission state
 *
 * Usage:
 * ```
 * val locationPermission = rememberLocationPermissionState()
 * if (!locationPermission.hasPermission) {
 *     // Show rationale or request permission
 *     locationPermission.requestPermission()
 * }
 * ```
 *
 * @return LocationPermissionState with current permission status and request function
 */
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(checkLocationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Update state based on granted permissions
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    return LocationPermissionState(
        hasPermission = hasPermission,
        requestPermission = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    )
}

/**
 * Check if location permissions are granted
 */
private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

