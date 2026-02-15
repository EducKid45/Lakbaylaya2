package com.example.lakbaylaya.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        SmsResultDispatcher.dispatch(true, "Emergency SMS delivered")
    }
}

