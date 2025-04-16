package com.example.tempreader.service

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "TempReaderPrefs"
        private const val KEY_LOWER_THRESHOLD = "lower_threshold"
        private const val KEY_UPPER_THRESHOLD = "upper_threshold"
        private const val KEY_NETWORK_LOST_NOTIFIED = "network_lost_notified"
        private const val KEY_DATA_STALE_NOTIFIED = "data_stale_notified"
        private const val KEY_LAST_ALERT_TEMP = "last_alert_temp"
        private const val KEY_LAST_ALERT_HUMIDITY = "last_alert_humidity"
        private const val KEY_LAST_ALERT_TIME = "last_alert_time"
        
        private const val DEFAULT_LOWER_THRESHOLD = 18.0f
        private const val DEFAULT_UPPER_THRESHOLD = 25.0f
        private const val MIN_THRESHOLD_DIFFERENCE = 2.0f
    }

    fun getLowerTemperatureThreshold(): Float {
        return prefs.getFloat(KEY_LOWER_THRESHOLD, DEFAULT_LOWER_THRESHOLD)
    }

    fun getUpperTemperatureThreshold(): Float {
        return prefs.getFloat(KEY_UPPER_THRESHOLD, DEFAULT_UPPER_THRESHOLD)
    }

    fun setTemperatureThresholds(lower: Float, upper: Float) {
        require(upper - lower >= MIN_THRESHOLD_DIFFERENCE) {
            "Minimum difference between thresholds must be $MIN_THRESHOLD_DIFFERENCE°C"
        }
        require(lower >= -50 && upper <= 100) {
            "Temperature thresholds must be between -50°C and 100°C"
        }
        prefs.edit()
            .putFloat(KEY_LOWER_THRESHOLD, lower)
            .putFloat(KEY_UPPER_THRESHOLD, upper)
            .apply()
    }

    fun isNetworkLostNotified(): Boolean = prefs.getBoolean(KEY_NETWORK_LOST_NOTIFIED, false)

    fun setNetworkLostNotified(notified: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_LOST_NOTIFIED, notified).apply()
    }

    fun isDataStaleNotified(): Boolean = prefs.getBoolean(KEY_DATA_STALE_NOTIFIED, false)

    fun setDataStaleNotified(notified: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_STALE_NOTIFIED, notified).apply()
    }

    fun setLastAlertData(temperature: Float, humidity: Float) {
        prefs.edit()
            .putFloat(KEY_LAST_ALERT_TEMP, temperature)
            .putFloat(KEY_LAST_ALERT_HUMIDITY, humidity)
            .putLong(KEY_LAST_ALERT_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getLastAlertData(): Triple<Float, Float, Long> {
        return Triple(
            prefs.getFloat(KEY_LAST_ALERT_TEMP, 0f),
            prefs.getFloat(KEY_LAST_ALERT_HUMIDITY, 0f),
            prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        )
    }

    fun clearLastAlertData() {
        prefs.edit()
            .remove(KEY_LAST_ALERT_TEMP)
            .remove(KEY_LAST_ALERT_HUMIDITY)
            .remove(KEY_LAST_ALERT_TIME)
            .apply()
    }
}