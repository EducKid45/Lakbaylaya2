package com.example.lakbaylaya.emergency

import android.Manifest
import android.app.Activity
import android.location.Location
import android.net.Uri
import android.content.Intent
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * EmergencyManager: one-tap emergency flow using the system SMS composer (pre-filled).
 *
 * Behavior:
 * - Requests runtime permission for ACCESS_FINE_LOCATION when needed
 * - Fetches last known location (falls back to a single high-accuracy update)
 * - Builds an emergency message with a Google Maps link: https://maps.google.com/?q=LAT,LNG
 * - Opens the system SMS app with recipient and message prefilled (ACTION_SENDTO, smsto:)
 * - Shows toast feedback and invokes optional callback with success (composer opened) or failure
 *
 * Note: No SEND_SMS permission is required since we use the system SMS app. The user must tap Send.
 */
class EmergencyManager(
    private val activity: Activity,
    private val emergencyNumber: String,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
    private val onResult: ((success: Boolean, message: String) -> Unit)? = null
) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 1420
    }

    // One-tap entry point
    fun sendEmergencySms() {
        if (!hasLocationPermission()) {
            requestPermissions()
            return
        }

        fetchLocationAndOpenSmsComposer()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionLauncher?.launch(perms) ?: run {
            ActivityCompat.requestPermissions(activity, perms, REQUEST_PERMISSIONS_CODE)
        }
    }

    /**
     * Call this from your ActivityResult launcher (RequestMultiplePermissions) result map.
     */
    fun onPermissionResults(results: Map<String, Boolean>) {
        val grantedLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (grantedLocation) {
            fetchLocationAndOpenSmsComposer()
        } else {
            val msg = "Location permission denied: unable to prepare emergency message"
            notifyResult(false, msg)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchLocationAndOpenSmsComposer() {
        try {
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    openSmsComposerWithLocation(location)
                } else {
                    requestSingleLocationUpdate()
                }
            }.addOnFailureListener {
                requestSingleLocationUpdate()
            }
        } catch (_: SecurityException) {
            val msg = "Location permission missing"
            notifyResult(false, msg)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestSingleLocationUpdate() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0L)
            .setMinUpdateIntervalMillis(0L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    openSmsComposerWithLocation(loc)
                } else {
                    // If we still couldn't obtain location, open composer with fallback text
                    openSmsComposerWithFallback()
                }
                fusedClient.removeLocationUpdates(this)
            }
        }

        try {
            fusedClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            val msg = "Location permission missing"
            notifyResult(false, msg)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun openSmsComposerWithLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = "EMERGENCY: I need help. My location: $mapsLink"

        openSmsComposer(message)
    }

    private fun openSmsComposerWithFallback() {
        val message = "EMERGENCY: I need help. My location is unavailable."
        openSmsComposer(message)
    }

    private fun openSmsComposer(message: String) {
        try {
            val uri = Uri.parse("smsto:${Uri.encode(emergencyNumber)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
            }

            // Verify there's an app to handle the intent
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                val okMsg = "Opened messaging app"
                notifyResult(true, okMsg)
                Toast.makeText(activity, okMsg, Toast.LENGTH_SHORT).show()
            } else {
                val err = "No SMS app available"
                notifyResult(false, err)
                Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            val err = "Failed to open messaging app: ${e.message}"
            notifyResult(false, err)
            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
        }
    }

    private fun notifyResult(success: Boolean, message: String) {
        onResult?.invoke(success, message)
    }
}
