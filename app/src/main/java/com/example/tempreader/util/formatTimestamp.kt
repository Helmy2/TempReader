package com.example.tempreader.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatTimestamp(): String {
    return try {
        val format = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        format.format(Date(this * 1000))
    } catch (e: Exception) {
        Log.e("TemperatureChecker", "Error formatting timestamp: ${e.message}", e)
        "Unknown"
    }
}

fun Long.formatTimestampChart(): String {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(Date(this * 1000))
    } catch (e: Exception) {
        Log.e("TemperatureChecker", "Error formatting chart timestamp: ${e.message}", e)
        "Unknown"
    }
}