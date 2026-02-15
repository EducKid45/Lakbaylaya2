package com.example.lakbaylaya.emergency

/**
 * Simple in-memory dispatcher for SMS send/delivery results.
 * EmergencyManager sets a callback before sending; manifest receivers call dispatch to notify it.
 */
object SmsResultDispatcher {
    @Volatile
    private var callback: ((success: Boolean, message: String) -> Unit)? = null

    @JvmStatic
    fun setCallback(cb: ((Boolean, String) -> Unit)?) {
        callback = cb
    }

    @JvmStatic
    fun dispatch(success: Boolean, message: String) {
        callback?.invoke(success, message)
        // Clear after invocation to avoid stale references
        callback = null
    }
}
