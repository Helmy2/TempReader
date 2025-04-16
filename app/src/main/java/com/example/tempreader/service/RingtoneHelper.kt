package com.example.tempreader.service

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import java.util.Timer
import java.util.TimerTask

class RingtoneHelper(private val context: Context) {
    private var ringtone: Ringtone? = null
    private val timer = Timer()

    fun playRingtone() {
        try {
            ringtone?.stop()
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            ringtone?.play()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    stopRingtone()
                }
            }, 60000) // Stop after 60 seconds
        } catch (e: Exception) {
            Log.e("RingtoneHelper", "Error playing ringtone: ${e.message}", e)
        }
    }

    fun stopRingtone() {
        ringtone?.stop()
        ringtone = null
    }
}
