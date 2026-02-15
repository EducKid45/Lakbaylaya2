package com.example.lakbaylaya.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * BluetoothNotificationManager - Manages persistent notifications for Bluetooth connection
 *
 * Responsibilities:
 * - Create notification channel (API 26+)
 * - Show persistent "Connected" notification
 * - Dismiss notification
 * - Handle notification lifecycle
 *
 * Architecture:
 * - Singleton pattern for notification management
 * - Uses androidx NotificationCompat for backward compatibility
 * - Creates separate channel for Bluetooth notifications
 *
 * Notification Strategy:
 * - When connected: Show persistent notification with Bluetooth icon
 * - When disconnected: Dismiss notification
 * - Notification importance: IMPORTANCE_LOW (silent, no sound/vibration)
 */
class BluetoothNotificationManager(private val context: Context) {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "lakbaylaya_bluetooth_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (required for API 26+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Bluetooth Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Bluetooth device connection status"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show connected notification
     * @param deviceName Name of the connected Bluetooth device
     */
    fun showConnectedNotification(deviceName: String) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("$deviceName Connected")
            .setContentText("Device ready")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system bluetooth icon
            .setAutoCancel(false)
            .setOngoing(true) // Persistent notification, not dismissible by user
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Dismiss the notification
     */
    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
