package com.example.lakbaylaya.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val resultCode = getResultCode()
        when (resultCode) {
            android.app.Activity.RESULT_OK -> SmsResultDispatcher.dispatch(
                true,
                "Emergency SMS sent"
            )

            else -> SmsResultDispatcher.dispatch(
                false,
                "Failed to send emergency SMS (send failed)"
            )
        }
    }
}
