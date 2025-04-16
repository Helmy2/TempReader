package com.example.tempreader.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
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
        private const val NOTIFICATION_ID = 1
        private const val TAG = "TempCheckService"
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
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, updatedNotification)
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
        super.onStartCommand(intent, flags, startId)

        try {
            if (!::temperatureChecker.isInitialized) {
                Log.e(TAG, "TemperatureChecker not initialized!")
                stopSelf()
                return START_NOT_STICKY
            }

            registerReceiver(stopRingtoneReceiver, IntentFilter(STOP_RINGTONE_ACTION))
            
            val initialNotification = notificationHelper.createForegroundNotification(pendingIntent)
            startForeground(NOTIFICATION_ID, initialNotification)

            if (!temperatureChecker.startChecking()) {
                Log.e(TAG, "Failed to start temperature checking")
                stopSelf()
                return START_NOT_STICKY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            temperatureChecker.stopChecking()
            ringtoneHelper.stopRingtone()
            try {
                unregisterReceiver(stopRingtoneReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver not registered", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        } finally {
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
