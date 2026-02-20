package com.example.lakbaylaya.emergency

/**
 * Generic SMS sender contract. Implementations should call the provided callback
 * on the main thread.
 */
interface SmsCallback {
    fun onSuccess(response: String? = null)
    fun onFailure(errorMessage: String)
}

interface SmsSender {
    fun sendSms(phoneNumber: String, message: String, callback: SmsCallback)
}
