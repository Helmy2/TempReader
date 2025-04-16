package com.example.tempreader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val serviceChannelId = "service_channel"
    private val alertChannelId = "temp_alert_channel"
    private val networkChannelId = "network_channel"
    private val staleDataChannelId = "stale_data_channel"

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (notificationManager.getNotificationChannel(serviceChannelId) == null) {
            val serviceChannel = NotificationChannel(
                serviceChannelId, "Temperature Check Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for temperature monitoring service"
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }

        if (notificationManager.getNotificationChannel(alertChannelId) == null) {
            val alertChannel = NotificationChannel(
                alertChannelId, "Temperature Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for high or low temperature readings"
            }
            notificationManager.createNotificationChannel(alertChannel)
        }

        if (notificationManager.getNotificationChannel(networkChannelId) == null) {
            val networkChannel = NotificationChannel(
                networkChannelId, "Network Status", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for network connectivity issues"
            }
            notificationManager.createNotificationChannel(networkChannel)
        }

        if (notificationManager.getNotificationChannel(staleDataChannelId) == null) {
            val staleDataChannel = NotificationChannel(
                staleDataChannelId, "Data Status", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for stale sensor data"
            }
            notificationManager.createNotificationChannel(staleDataChannel)
        }
    }

    fun canPostNotifications(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun createForegroundNotification(pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, serviceChannelId)
            .setContentTitle("Temperature Monitoring").setContentText("Starting...")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true)
            .setContentIntent(pendingIntent).build()
    }

    fun updateForegroundNotification(message: String, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, serviceChannelId)
            .setContentTitle("Temperature Monitoring").setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true)
            .setContentIntent(pendingIntent).build()
    }

    fun createAlertNotification(message: String, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Temperature Alert")
            .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(pendingIntent).build()
    }

    fun createNetworkLostNotification(pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, networkChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Network Unavailable")
            .setContentText("No internet connection. Temperature updates paused.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
            .setContentIntent(pendingIntent).build()
    }

    fun createStaleDataNotification(pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, staleDataChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Stale Data")
            .setContentText("No new sensor data received in the last 15 minutes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
            .setContentIntent(pendingIntent).build()
    }

    fun cancelNetworkLostNotification() {
        if (canPostNotifications()) {
            NotificationManagerCompat.from(context).cancel(NETWORK_NOTIFICATION_ID)
        }
    }

    fun cancelStaleDataNotification() {
        if (canPostNotifications()) {
            NotificationManagerCompat.from(context).cancel(STALE_DATA_NOTIFICATION_ID)
        }
    }

    companion object {
        const val NETWORK_NOTIFICATION_ID = 2
        const val STALE_DATA_NOTIFICATION_ID = 3
    }
}
