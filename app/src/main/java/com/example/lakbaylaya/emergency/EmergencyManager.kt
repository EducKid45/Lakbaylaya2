package com.example.lakbaylaya.emergency

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
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
 * EmergencyManager: one-tap emergency flow that directly sends an SMS (no composer app).
 * Implements EmergencyHandler for interchangeable usage.
 *
 * @param emergencyNumberProvider Lambda called at send-time to get the current emergency number.
 *   This allows the number to come from the user's saved profile instead of being hardcoded.
 */
class EmergencyManager(
    private val activity: Activity,
    private val emergencyNumberProvider: () -> String,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
    private val onResult: ((success: Boolean, message: String) -> Unit)? = null
) : EmergencyHandler {

    /** Convenience constructor for a fixed number (backwards-compatible). */
    constructor(
        activity: Activity,
        emergencyNumber: String,
        permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) : this(activity, { emergencyNumber }, permissionLauncher, onResult)

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 1420
        private const val TAG = "EmergencyManager"
        private const val SMS_SENT_ACTION = "com.example.lakbaylaya.EMERGENCY_SMS_SENT"
    }

    // One-tap entry point
    override fun sendEmergencySms() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }
        fetchLocationAndSend()
    }

    /**
     * Call this from your ActivityResult launcher (RequestMultiplePermissions) result map.
     */
    override fun onPermissionResults(results: Map<String, Boolean>) {
        val grantedLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val grantedSms     = results[Manifest.permission.SEND_SMS] == true

        when {
            grantedLocation && grantedSms -> fetchLocationAndSend()
            !grantedLocation -> {
                val msg = "Location permission denied: unable to prepare emergency message"
                notifyResult(false, msg)
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
            else -> {
                val msg = "SMS permission denied: cannot send emergency message"
                notifyResult(false, msg)
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val locOk = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val smsOk = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return locOk && smsOk
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        permissionLauncher?.launch(perms) ?: run {
            ActivityCompat.requestPermissions(activity, perms, REQUEST_PERMISSIONS_CODE)
        }
    }

    private fun fetchLocationAndSend() {
        try {
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendSmsWithLocation(location)
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
                    sendSmsWithLocation(loc)
                } else {
                    sendSmsWithFallback()
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

    private fun sendSmsWithLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = "EMERGENCY: I need help. My location: $mapsLink"
        sendDirectSms(message)
    }

    private fun sendSmsWithFallback() {
        val message = "EMERGENCY: I need help. My location is unavailable."
        sendDirectSms(message)
    }

    @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
    private fun sendDirectSms(message: String) {
        val emergencyNumber = emergencyNumberProvider().trim()
        if (emergencyNumber.isBlank()) {
            val err = "No emergency contact number set. Please add one in Settings."
            Log.e(TAG, err)
            notifyResult(false, err)
            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Register a one-shot dynamic receiver so we get the real modem result
            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        activity.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    when (val code = resultCode) {
                        Activity.RESULT_OK -> {
                            val okMsg = "Emergency SMS sent successfully"
                            Log.d(TAG, okMsg)
                            notifyResult(true, okMsg)
                            Toast.makeText(activity, okMsg, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            val reason = when (code) {
                                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "generic failure"
                                SmsManager.RESULT_ERROR_NO_SERVICE       -> "no service"
                                SmsManager.RESULT_ERROR_NULL_PDU         -> "null PDU"
                                SmsManager.RESULT_ERROR_RADIO_OFF        -> "radio off"
                                else                                     -> "error code $code"
                            }
                            val errMsg = "Emergency SMS not sent: $reason"
                            Log.e(TAG, errMsg)
                            notifyResult(false, errMsg)
                            Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            val filter = IntentFilter(SMS_SENT_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(sentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                activity.registerReceiver(sentReceiver, filter)
            }

            // Build PendingIntent targeting this app's package explicitly
            val sentIntent = Intent(SMS_SENT_ACTION).also { it.setPackage(activity.packageName) }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT

            val sentPendingIntent = PendingIntent.getBroadcast(activity, 0, sentIntent, pendingFlags)

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split message if longer than 160 chars
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(emergencyNumber, null, message, sentPendingIntent, null)
            } else {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                    repeat(parts.size) { add(sentPendingIntent) }
                }
                smsManager.sendMultipartTextMessage(
                    emergencyNumber, null, parts, sentIntents, null
                )
            }

            // Inform the user the request was dispatched (actual result via receiver above)
            Toast.makeText(activity, "Sending emergency SMS…", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "sendTextMessage dispatched to $emergencyNumber")

        } catch (e: SecurityException) {
            val err = "SEND_SMS permission missing: ${e.message}"
            Log.e(TAG, err, e)
            notifyResult(false, err)
            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val err = "Failed to send emergency SMS: ${e.message}"
            Log.e(TAG, err, e)
            notifyResult(false, err)
            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
        }
    }

    private fun notifyResult(success: Boolean, message: String) {
        onResult?.invoke(success, message)
    }
}
