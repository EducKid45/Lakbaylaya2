package com.example.lakbaylaya.emergency

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

private const val TAG = "SendSmsManager"

/**
 * Manager that can send SMS either via device SmsManager or via a provided SmsSender implementation.
 * If `smsSender` is null, the device SMS API is used (requires SEND_SMS permission).
 */
class SendSmsManager(private val context: Context, private val smsSender: SmsSender? = null) {

    fun send(phoneNumber: String, message: String, callback: SmsCallback? = null) {
        smsSender?.let { sender ->
            // Use provided implementation (e.g., SemaphoreSmsSender)
            sender.sendSms(phoneNumber, message, object : SmsCallback {
                override fun onSuccess(response: String?) {
                    Toast.makeText(context, "SMS sent (gateway)", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Gateway SMS success: $response")
                    callback?.onSuccess(response)
                }

                override fun onFailure(errorMessage: String) {
                    Toast.makeText(context, "Gateway SMS failed: $errorMessage", Toast.LENGTH_LONG)
                        .show()
                    Log.e(TAG, "Gateway SMS failed: $errorMessage")
                    callback?.onFailure(errorMessage)
                }
            })
            return
        }

        // Fallback to device SmsManager
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS sent (device)", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Device SMS sent to $phoneNumber")
            callback?.onSuccess("device")
        } catch (e: SecurityException) {
            Toast.makeText(context, "Missing SEND_SMS permission", Toast.LENGTH_LONG).show()
            Log.e(TAG, "SEND_SMS permission missing", e)
            callback?.onFailure("Missing SEND_SMS permission")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "sendSms error", e)
            callback?.onFailure(e.message ?: "Unknown error")
        }
    }
}

