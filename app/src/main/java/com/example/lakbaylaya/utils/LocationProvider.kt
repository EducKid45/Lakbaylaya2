package com.example.lakbaylaya.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

/**
 * Location provider for GPS tracking
 *
 * Wraps FusedLocationProviderClient to provide location updates
 * in a coroutine-friendly way.
 *
 * Features:
 * - Real-time GPS location updates
 * - High accuracy mode
 * - Automatic cleanup
 * - Permission handling
 *
 * Usage:
 * ```
 * val locationProvider = LocationProvider(context)
 * locationProvider.startLocationUpdates { location ->
 *     // Handle location update
 * }
 * locationProvider.stopLocationUpdates()
 * ```
 */
class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * Start receiving location updates
     *
     * @param onLocationUpdate Callback invoked when location changes
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update every 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L) // Fastest update every 2 seconds
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        // Also get last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { onLocationUpdate(it) }
        }
    }

    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    /**
     * Get last known location (single call)
     *
     * @param onLocationReceived Callback with last known location or null
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation(onLocationReceived: (Location?) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            onLocationReceived(location)
        }
    }
}

