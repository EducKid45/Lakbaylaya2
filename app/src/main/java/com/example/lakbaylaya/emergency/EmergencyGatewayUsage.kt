package com.example.lakbaylaya.emergency

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

/**
 * Example Activity showing how you might use EmergencySmsGateway with SemaphoreSmsSender.
 * This is illustrative; adapt to your app's Activity / ViewModel and permission flow.
 */
class EmergencyGatewayUsageActivity : AppCompatActivity() {

    private lateinit var gateway: EmergencySmsGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register a permissions launcher for ACCESS_FINE_LOCATION
        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                gateway.onPermissionResults(results)
            }

        // Create a Semaphore sender with your API key (keep this secure)
        val apiKey = "eb0f38d82021fde23d7f4bbfa506b46d"
        val sender = SemaphoreSmsSender(apiKey)

        gateway = EmergencySmsGateway(
            activity = this,
            emergencyNumber = "+09086096506",
            smsSender = sender,
            permissionLauncher = launcher,
            onResult = { success, message ->
                Log.d("EmergencyGatewayUsage", "Result: $success - $message")
            }
        )

        // To trigger:
        // gateway.sendEmergencySms()
    }
}

