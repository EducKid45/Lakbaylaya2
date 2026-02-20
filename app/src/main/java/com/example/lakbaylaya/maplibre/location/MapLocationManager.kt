package com.example.lakbaylaya.maplibre.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * Lightweight manager that encapsulates MapLibre location component activation and deactivation.
 *
 * Keeps location-specific logic separate from map rendering and layer management.
 */
class MapLocationManager(private val context: Context) {
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Enable location component on the provided map and style.
     * Call after location permission is granted.
     * @return true if the location component was successfully enabled; false otherwise (including missing permission).
     */
    fun enable(map: MapLibreMap, style: Style): Boolean {
        if (!hasLocationPermission()) {
            // Permission not granted; caller should request it before enabling.
            Log.w("MapLocationManager", "Location permission not granted; skipping enable().")
            return false
        }
        return try {
            map.locationComponent.apply {
                // Hook the location component to the provided style and enable the default engine.
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(true)
                        .build()
                )
                isLocationComponentEnabled = true
                renderMode = RenderMode.COMPASS
                cameraMode = CameraMode.TRACKING
            }
            true
        } catch (se: SecurityException) {
            Log.w(
                "MapLocationManager",
                "SecurityException enabling location component: ${se.message}"
            )
            false
        } catch (e: Exception) {
            Log.e("MapLocationManager", "Failed to enable location: ${e.message}", e)
            false
        }
    }

    /**
     * Disable location component on the provided map.
     * Safe to call even if the location component was not previously enabled.
     * @return true if disabled successfully or already disabled; false if a SecurityException occurred.
     */
    fun disable(map: MapLibreMap): Boolean {
        return try {
            // If permission is absent, still attempt to clear the flag but handle SecurityException.
            if (!hasLocationPermission()) {
                try {
                    map.locationComponent.isLocationComponentEnabled = false
                } catch (ignored: SecurityException) {
                    Log.w(
                        "MapLocationManager",
                        "SecurityException while disabling location component: ${ignored.message}"
                    )
                    return false
                }
                return true
            }

            map.locationComponent.isLocationComponentEnabled = false
            true
        } catch (se: SecurityException) {
            Log.w(
                "MapLocationManager",
                "SecurityException disabling location component: ${se.message}"
            )
            false
        } catch (e: Exception) {
            Log.e("MapLocationManager", "Failed to disable location: ${e.message}", e)
            false
        }
    }

    /**
     * Clean up any resources held by the manager. No-op for now but kept for symmetry.
     */
    fun onDestroy() {
        // No resources to clean up yet; placeholder for future enhancements
    }
}