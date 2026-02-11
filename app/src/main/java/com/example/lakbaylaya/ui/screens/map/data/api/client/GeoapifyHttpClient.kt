package com.example.lakbaylaya.ui.screens.map.data.api.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * HTTP client for making API requests to Geoapify using OkHttp
 */
class GeoapifyHttpClient {

    companion object {
        private const val CONNECT_TIMEOUT = 15L // seconds
        private const val READ_TIMEOUT = 15L // seconds
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .build()

    /**
     * Perform a GET request
     *
     * @param url Full URL including query parameters
     * @return Response body as String
     * @throws Exception if request fails
     */
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "Lakbaylaya/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                return@withContext body
            } else {
                val errorBody = response.body?.string()
                val message = errorBody ?: "HTTP error ${response.code}"
                throw Exception("API request failed: $message")
            }
        }
    }
}
