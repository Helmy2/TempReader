package com.example.tempreader.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "temperature_check_prefs"
        private const val KEY_NETWORK_LOST_NOTIFIED = "is_network_lost_notified"
        private const val KEY_DATA_STALE_NOTIFIED = "is_data_stale_notified"
        private const val KEY_LAST_ALERT_TEMPERATURE = "last_alert_temperature"
        private const val KEY_LAST_ALERT_HUMIDITY = "last_alert_humidity"
        private const val KEY_LOWER_TEMPERATURE_THRESHOLD = "lower_temperature_threshold"
        private const val KEY_UPPER_TEMPERATURE_THRESHOLD = "upper_temperature_threshold"
        private const val DEFAULT_LOWER_THRESHOLD = 20f
        private const val DEFAULT_UPPER_THRESHOLD = 25f
    }

    fun setNetworkLostNotified(isNotified: Boolean) {
        prefs.edit { putBoolean(KEY_NETWORK_LOST_NOTIFIED, isNotified) }
    }

    fun isNetworkLostNotified(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_LOST_NOTIFIED, false)
    }

    fun setDataStaleNotified(isNotified: Boolean) {
        prefs.edit { putBoolean(KEY_DATA_STALE_NOTIFIED, isNotified) }
    }

    fun isDataStaleNotified(): Boolean {
        return prefs.getBoolean(KEY_DATA_STALE_NOTIFIED, false)
    }

    fun setLastAlertData(temperature: Float, humidity: Float) {
        prefs.edit {
            putFloat(KEY_LAST_ALERT_TEMPERATURE, temperature)
            putFloat(KEY_LAST_ALERT_HUMIDITY, humidity)
        }
    }

    fun getLastAlertTemperature(): Float? {
        return if (prefs.contains(KEY_LAST_ALERT_TEMPERATURE)) {
            prefs.getFloat(KEY_LAST_ALERT_TEMPERATURE, 0f)
        } else {
            null
        }
    }

    fun getLastAlertHumidity(): Float? {
        return if (prefs.contains(KEY_LAST_ALERT_HUMIDITY)) {
            prefs.getFloat(KEY_LAST_ALERT_HUMIDITY, 0f)
        } else {
            null
        }
    }

    fun clearLastAlertData() {
        prefs.edit {
            remove(KEY_LAST_ALERT_TEMPERATURE)
            remove(KEY_LAST_ALERT_HUMIDITY)
        }
    }

    fun setTemperatureThresholds(lower: Float, upper: Float) {
        if (lower >= upper) {
            Log.w(
                "PreferencesManager",
                "Lower threshold ($lower) must be less than upper ($upper). Ignoring."
            )
            return
        }
        prefs.edit {
            putFloat(KEY_LOWER_TEMPERATURE_THRESHOLD, lower)
            putFloat(KEY_UPPER_TEMPERATURE_THRESHOLD, upper)
        }
    }

    fun getLowerTemperatureThreshold(): Float {
        val lower = prefs.getFloat(KEY_LOWER_TEMPERATURE_THRESHOLD, DEFAULT_LOWER_THRESHOLD)
        val upper = prefs.getFloat(KEY_UPPER_TEMPERATURE_THRESHOLD, DEFAULT_UPPER_THRESHOLD)
        if (lower >= upper) {
            Log.w(
                "PreferencesManager",
                "Invalid thresholds: lower ($lower) >= upper ($upper). Using default $DEFAULT_LOWER_THRESHOLD."
            )
            return DEFAULT_LOWER_THRESHOLD
        }
        return lower
    }

    fun getUpperTemperatureThreshold(): Float {
        val lower = prefs.getFloat(KEY_LOWER_TEMPERATURE_THRESHOLD, DEFAULT_LOWER_THRESHOLD)
        val upper = prefs.getFloat(KEY_UPPER_TEMPERATURE_THRESHOLD, DEFAULT_UPPER_THRESHOLD)
        if (lower >= upper) {
            Log.w(
                "PreferencesManager",
                "Invalid thresholds: lower ($lower) >= upper ($upper). Using default $DEFAULT_UPPER_THRESHOLD."
            )
            return DEFAULT_UPPER_THRESHOLD
        }
        return upper
    }
}
