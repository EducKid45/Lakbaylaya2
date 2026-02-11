package com.example.lakbaylaya.ui.screens.map.navigation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Android GPS-based implementation of LocationTracker
 *
 * Uses device GPS to track user location during navigation.
 * Provides high-accuracy positioning for navigation step progression.
 */
class AndroidLocationTracker(
    private val context: Context
) : LocationTracker, LocationListener {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var isTracking = false
    private var lastKnownLocation: Location? = null

    // Configuration
    private var minimumDistance = 1f // meters
    private var minimumInterval = 1000L // milliseconds

    // Callbacks
    private var onLocationUpdateCallback: ((Location) -> Unit)? = null
    private var onAccuracyChangedCallback: ((Float) -> Unit)? = null

    companion object {
        private const val TAG = "AndroidLocationTracker"
    }

    override fun startTracking(
        onLocationUpdate: (Location) -> Unit,
        onAccuracyChanged: (Float) -> Unit
    ) {
        if (isTracking) {
            Log.w(TAG, "Already tracking location")
            return
        }

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        onLocationUpdateCallback = onLocationUpdate
        onAccuracyChangedCallback = onAccuracyChanged

        try {
            // Try GPS provider first for highest accuracy
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minimumInterval,
                    minimumDistance,
                    this
                )
                Log.d(TAG, "Started GPS location tracking")
            }

            // Also use network provider as fallback
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    minimumInterval * 2, // Less frequent for network
                    minimumDistance * 2, // Larger distance threshold
                    this
                )
                Log.d(TAG, "Started network location tracking")
            }

            // Get last known location immediately if available
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val bestLastLocation = when {
                lastGps != null && lastNetwork != null -> {
                    if (lastGps.accuracy < lastNetwork.accuracy) lastGps else lastNetwork
                }
                lastGps != null -> lastGps
                lastNetwork != null -> lastNetwork
                else -> null
            }

            bestLastLocation?.let {
                lastKnownLocation = it
                onLocationUpdate(it)
                Log.d(TAG, "Used last known location: ${it.latitude}, ${it.longitude}")
            }

            isTracking = true

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location tracking", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
        }
    }

    override fun stopTracking() {
        if (!isTracking) {
            Log.w(TAG, "Already stopped tracking location")
            return
        }

        try {
            locationManager.removeUpdates(this)
            isTracking = false
            onLocationUpdateCallback = null
            onAccuracyChangedCallback = null
            Log.d(TAG, "Stopped location tracking")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }

    override fun getLastLocation(): Location? = lastKnownLocation

    override fun isTracking(): Boolean = isTracking

    override fun setMinimumDistance(distanceMeters: Float) {
        minimumDistance = distanceMeters
        Log.d(TAG, "Set minimum distance to $distanceMeters meters")
    }

    override fun setMinimumInterval(intervalMs: Long) {
        minimumInterval = intervalMs
        Log.d(TAG, "Set minimum interval to $intervalMs ms")
    }

    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location
        onLocationUpdateCallback?.invoke(location)

        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude} " +
                "accuracy: ${location.accuracy}m speed: ${location.speed}m/s")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }

    @Deprecated("Deprecated in API level 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "Provider status changed: $provider status: $status")
    }
}
