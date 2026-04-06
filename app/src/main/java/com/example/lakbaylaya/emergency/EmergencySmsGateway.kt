package com.example.lakbaylaya.emergency

import android.Manifest
import android.app.Activity
import android.location.Location
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
import android.util.Log

/**
 * EmergencySmsGateway: one-tap emergency flow that sends SMS using a provided SmsSender
 * (for example, SemaphoreSmsSender). This class fetches location (if permitted) and sends
 * the composed emergency message via the gateway.
 *
 * @param emergencyNumberProvider Lambda called at send-time to get the current emergency number.
 */
class EmergencySmsGateway(
    private val activity: Activity,
    private val emergencyNumberProvider: () -> String,
    private val smsSender: SmsSender,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
    private val onResult: ((success: Boolean, message: String) -> Unit)? = null
) : EmergencyHandler {

    /** Convenience constructor for a fixed number (backwards-compatible). */
    constructor(
        activity: Activity,
        emergencyNumber: String,
        smsSender: SmsSender,
        permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) : this(activity, { emergencyNumber }, smsSender, permissionLauncher, onResult)
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 1421
        private const val TAG = "EmergencySmsGateway"
    }

    override fun sendEmergencySms() {
        if (!hasLocationPermission()) {
            requestPermissions()
            return
        }

        fetchLocationAndSend()
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
    override fun onPermissionResults(results: Map<String, Boolean>) {
        val grantedLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (grantedLocation) {
            fetchLocationAndSend()
        } else {
            val msg = "Location permission denied: unable to prepare emergency message"
            notifyResult(false, msg)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchLocationAndSend() {
        try {
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendWithLocation(location)
                } else {
                    requestSingleLocationUpdate()
                }
            }.addOnFailureListener { _ ->
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
                    sendWithLocation(loc)
                } else {
                    // If still unavailable, send with fallback text
                    sendWithFallback()
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

    private fun sendWithLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = "EMERGENCY: I need help. My location: $mapsLink"
        sendMessageToGateway(message)
    }

    private fun sendWithFallback() {
        val message = "EMERGENCY: I need help. My location is unavailable."
        sendMessageToGateway(message)
    }

    private fun sendMessageToGateway(message: String) {
        val emergencyNumber = emergencyNumberProvider().trim()
        if (emergencyNumber.isBlank()) {
            val err = "No emergency contact number set. Please add one in Settings."
            Log.e(TAG, err)
            notifyResult(false, err)
            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
            return
        }
        val sender = SendSmsManager(activity, smsSender)
        sender.send(emergencyNumber, message, object : SmsCallback {
            override fun onSuccess(response: String?) {
                val okMsg = "Emergency SMS sent via gateway"
                notifyResult(true, okMsg)
                Toast.makeText(activity, okMsg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Gateway send success: $response")
            }

            override fun onFailure(errorMessage: String) {
                val err = "Gateway SMS failed: $errorMessage"
                notifyResult(false, err)
                Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
                Log.e(TAG, err)
            }
        })
    }

    private fun notifyResult(success: Boolean, message: String) {
        onResult?.invoke(success, message)
    }
}
