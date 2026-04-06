package com.example.lakbaylaya.bluetooth

import android.util.Log
import java.io.OutputStream
import java.io.IOException

/**
 * Thin wrapper that holds the live RFCOMM [OutputStream] to the ESP32 and
 * exposes a thread-safe [write] function.
 *
 * Call [attach] whenever a new socket connection succeeds (from
 * [BluetoothManagerHelper]) and [detach] on disconnect.
 */
class Esp32SocketWriter {

    @Volatile
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "Esp32SocketWriter"

        /** Singleton so every caller shares the same live stream. */
        @Volatile
        private var instance: Esp32SocketWriter? = null

        fun getInstance(): Esp32SocketWriter =
            instance ?: synchronized(this) {
                instance ?: Esp32SocketWriter().also { instance = it }
            }
    }

    /** Called by BluetoothManagerHelper once the RFCOMM socket.connect() succeeds. */
    fun attach(stream: OutputStream) {
        outputStream = stream
        Log.d(TAG, "OutputStream attached")
    }

    /** Called on disconnect / cleanup. */
    fun detach() {
        outputStream = null
        Log.d(TAG, "OutputStream detached")
    }

    /** Returns true when an OutputStream is currently attached. */
    fun isAttached(): Boolean = outputStream != null

    /**
     * Write a raw string to the ESP32.  Fire-and-forget; logs errors without
     * crashing the caller.
     */
    fun write(data: String) {
        val stream = outputStream
        if (stream == null) {
            Log.w(TAG, "write() called but no OutputStream attached – ignored")
            return
        }
        try {
            stream.write(data.toByteArray(Charsets.UTF_8))
            stream.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to ESP32: ${e.message}")
            // Detach so callers know the stream is dead
            detach()
        }
    }
}
