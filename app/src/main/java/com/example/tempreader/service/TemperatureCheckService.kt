package com.example.tempreader.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import com.example.tempreader.MainActivity

class TemperatureCheckService : Service() {
    private lateinit var temperatureChecker: TemperatureChecker
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var ringtoneHelper: RingtoneHelper
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var pendingIntent: PendingIntent
    private val stopRingtoneReceiver = StopRingtoneReceiver()

    inner class StopRingtoneReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == STOP_RINGTONE_ACTION) {
                ringtoneHelper.stopRingtone()
                Log.d("Service", "Ringtone stopped via notification click")
                val appIntent =
                    Intent(this@TemperatureCheckService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                startActivity(appIntent)
            }
        }
    }

    companion object {
        private const val STOP_RINGTONE_ACTION = "com.example.tempreader.STOP_RINGTONE"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        ringtoneHelper = RingtoneHelper(this)
        preferencesManager = PreferencesManager(this)
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        temperatureChecker =
            TemperatureChecker(this, preferencesManager, onAlert = { temperature, humidity, isLow ->
                val message = if (isLow) {
                    "Low Temperature: $temperature°C, Humidity: $humidity%"
                } else {
                    "High Temperature: $temperature°C, Humidity: $humidity%"
                }
                val stopRingtoneIntent = Intent(STOP_RINGTONE_ACTION)
                val stopRingtonePendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    stopRingtoneIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alertNotification =
                    notificationHelper.createAlertNotification(message, stopRingtonePendingIntent)
                if (notificationHelper.canPostNotifications()) {
                    NotificationManagerCompat.from(this)
                        .notify(System.currentTimeMillis().toInt(), alertNotification)
                    ringtoneHelper.playRingtone()
                } else {
                    Log.w(
                        "Service",
                        "Cannot post alert notification because notifications are not enabled"
                    )
                    ringtoneHelper.playRingtone()
                }
            }, onTemperatureUpdate = { temperature, humidity ->
                val message = if (temperature == 0f && humidity == 0f) {
                    "No data available"
                } else {
                    "Temperature: $temperature°C, Humidity: $humidity%"
                }
                val updatedNotification =
                    notificationHelper.updateForegroundNotification(message, pendingIntent)
                if (notificationHelper.canPostNotifications()) {
                    NotificationManagerCompat.from(this).notify(1, updatedNotification)
                } else {
                    Log.w(
                        "Service",
                        "Cannot update foreground notification because notifications are not enabled"
                    )
                }
            }, onNetworkLost = {
                val networkNotification =
                    notificationHelper.createNetworkLostNotification(pendingIntent)
                if (notificationHelper.canPostNotifications()) {
                    NotificationManagerCompat.from(this).notify(
                        NotificationHelper.NETWORK_NOTIFICATION_ID, networkNotification
                    )
                } else {
                    Log.w(
                        "Service",
                        "Cannot post network lost notification because notifications are not enabled"
                    )
                }
            }, onNetworkRestored = {
                notificationHelper.cancelNetworkLostNotification()
            }, onStaleData = {
                val staleDataNotification =
                    notificationHelper.createStaleDataNotification(pendingIntent)
                if (notificationHelper.canPostNotifications()) {
                    NotificationManagerCompat.from(this).notify(
                        NotificationHelper.STALE_DATA_NOTIFICATION_ID, staleDataNotification
                    )
                } else {
                    Log.w(
                        "Service",
                        "Cannot post stale data notification because notifications are not enabled"
                    )
                }
            }, onFreshData = {
                notificationHelper.cancelStaleDataNotification()
            })
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(stopRingtoneReceiver, IntentFilter(STOP_RINGTONE_ACTION))

        val initialNotification = notificationHelper.createForegroundNotification(pendingIntent)
        startForeground(1, initialNotification)

        temperatureChecker.startChecking()

        return START_STICKY
    }

    override fun onDestroy() {
        temperatureChecker.stopChecking()
        ringtoneHelper.stopRingtone()
        unregisterReceiver(stopRingtoneReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
