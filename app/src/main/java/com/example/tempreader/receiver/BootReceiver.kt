package com.example.tempreader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tempreader.service.TemperatureCheckService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, TemperatureCheckService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
