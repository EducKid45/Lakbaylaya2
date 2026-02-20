package com.example.lakbaylaya.emergency

import okhttp3.*
import java.io.IOException
import android.os.Handler
import android.os.Looper

/**
 * SmsSender implementation that uses Semaphore API to send SMS.
 * API docs: https://semaphore.co/docs/api
 */
class SemaphoreSmsSender(private val apiKey: String) : SmsSender {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun sendSms(phoneNumber: String, message: String, callback: SmsCallback) {
        val formBody = FormBody.Builder()
            .add("apikey", apiKey)
            .add("number", phoneNumber)
            .add("message", message)
            .build()

        val request = Request.Builder()
            .url("https://api.semaphore.co/api/v4/messages")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                mainHandler.post { callback.onFailure(e.message ?: "Network error") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    mainHandler.post { callback.onSuccess(body) }
                } else {
                    mainHandler.post { callback.onFailure("${response.code} - ${body ?: ""}") }
                }
                response.close()
            }
        })
    }
}