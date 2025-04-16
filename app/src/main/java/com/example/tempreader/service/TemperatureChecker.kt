package com.example.tempreader.service

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlin.math.abs

class TemperatureChecker(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val onAlert: (temperature: Float, humidity: Float, isLow: Boolean) -> Unit,
    private val onTemperatureUpdate: (temperature: Float, humidity: Float) -> Unit,
    private val onNetworkLost: () -> Unit,
    private val onNetworkRestored: () -> Unit,
    private val onStaleData: () -> Unit,
    private val onFreshData: () -> Unit
) {
    private val isMonitoring = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var lastTemp: Float = 0f
    private var lastHumidity: Float = 0f
    private var lastUpdateTime: Long = 0
    private var lastWasBelowLow: Boolean = false
    private var lastWasAboveHigh: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database =
        Firebase.database("https://esp-temp-89f99-default-rtdb.europe-west1.firebasedatabase.app")
    private val ref = database.getReference("/UsersData/IVcnpuP1hiX3p7SgsAa1n0M6gcI2/readings")

    companion object {
        private const val TAG = "TemperatureChecker"
        private const val CHECK_INTERVAL = 60_000L // 1 minute
        private const val STALE_DATA_THRESHOLD = 15 * 60 * 1000L // 15 minutes
    }

    fun startChecking(): Boolean {
        if (isMonitoring.getAndSet(true)) {
            Log.w(TAG, "Temperature checking already started")
            return true
        }

        return try {
            scope.launch {
                while (isMonitoring.get()) {
                    checkTemperature()
                    delay(CHECK_INTERVAL)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start temperature checking", e)
            isMonitoring.set(false)
            false
        }
    }

    fun stopChecking() {
        if (isMonitoring.getAndSet(false)) {
            scope.cancel()
            handler.removeCallbacksAndMessages(null)
            Log.d(TAG, "Temperature checking stopped")
        }
    }

    private suspend fun checkTemperature() = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                Log.d("TemperatureChecker", "No network available")
                if (!preferencesManager.isNetworkLostNotified()) {
                    onNetworkLost()
                    preferencesManager.setNetworkLostNotified(true)
                }
                return@withContext
            }

            // Network is available, reset notification state and cancel notification
            if (preferencesManager.isNetworkLostNotified()) {
                onNetworkRestored()
                preferencesManager.setNetworkLostNotified(false)
            }

            ref.get().addOnSuccessListener { snapshot ->
                val latestReading = snapshot.children.lastOrNull()
                if (latestReading != null) {
                    val currentTemp =
                        latestReading.child("temperature").getValue(Float::class.java) ?: 0f
                    val humidity = latestReading.child("humidity").getValue(Float::class.java) ?: 0f
                    val timestampValue = latestReading.child("timestamp").value

                    // Check if data is stale (older than 15 minutes)
                    val isStale = when (timestampValue) {
                        is Long -> isDataStale(timestampValue)
                        is String -> isDataStale(timestampValue) // Backward compatibility
                        else -> {
                            Log.w("TemperatureChecker", "Invalid timestamp format: $timestampValue")
                            true // Assume stale if format is unknown
                        }
                    }

                    if (isStale && !preferencesManager.isDataStaleNotified()) {
                        onStaleData()
                        preferencesManager.setDataStaleNotified(true)
                    } else if (!isStale && preferencesManager.isDataStaleNotified()) {
                        onFreshData()
                        preferencesManager.setDataStaleNotified(false)
                    }

                    // Get thresholds from SharedPreferences
                    val lowerThreshold = preferencesManager.getLowerTemperatureThreshold()
                    val upperThreshold = preferencesManager.getUpperTemperatureThreshold()

                    // Temperature threshold checks
                    if (!lastWasAboveHigh && currentTemp > upperThreshold) {
                        if (shouldSendAlert(currentTemp, humidity)) {
                            onAlert(currentTemp, humidity, false)
                            preferencesManager.setLastAlertData(currentTemp, humidity)
                        }
                        lastWasAboveHigh = true
                        lastWasBelowLow = false
                    } else if (!lastWasBelowLow && currentTemp < lowerThreshold) {
                        if (shouldSendAlert(currentTemp, humidity)) {
                            onAlert(currentTemp, humidity, true)
                            preferencesManager.setLastAlertData(currentTemp, humidity)
                        }
                        lastWasBelowLow = true
                        lastWasAboveHigh = false
                    } else if (currentTemp in lowerThreshold..upperThreshold) {
                        // Reset flags when temperature is in normal range
                        lastWasBelowLow = false
                        lastWasAboveHigh = false
                    }

                    lastTemp = currentTemp
                    lastHumidity = humidity
                    onTemperatureUpdate(currentTemp, humidity)
                } else {
                    onTemperatureUpdate(0f, 0f) // Default values if no data
                    if (!preferencesManager.isDataStaleNotified()) {
                        onStaleData() // Treat no data as stale
                        preferencesManager.setDataStaleNotified(true)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("TemperatureChecker", "Error fetching data: ${e.message}", e)
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime > STALE_DATA_THRESHOLD) {
                onStaleData()
            } else {
                onFreshData()
            }

            // Update last check time
            lastUpdateTime = currentTime
        } catch (e: Exception) {
            Log.e(TAG, "Error checking temperature", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        return activeNetwork != null
    }

    private fun isDataStale(timestampMs: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val fifteenMinutes = 15 * 60 * 1000 // 15 minutes in milliseconds
        return (currentTime - timestampMs) > fifteenMinutes
    }

    private fun isDataStale(timestampStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val timestampMs = format.parse(timestampStr)?.time ?: return true
            isDataStale(timestampMs)
        } catch (e: Exception) {
            Log.e("TemperatureChecker", "Error parsing string timestamp: ${e.message}", e)
            true // Assume stale if parsing fails
        }
    }

    private fun shouldSendAlert(currentTemp: Float, currentHumidity: Float): Boolean {
        val (lastTemp, lastHumidity, lastAlertTime) = preferencesManager.getLastAlertData()
        
        // If no previous alert or it was more than 30 minutes ago
        if (lastAlertTime == 0L || System.currentTimeMillis() - lastAlertTime > 30 * 60 * 1000) {
            return true
        }

        // If temperature changed significantly (more than 2 degrees)
        if (abs(currentTemp - lastTemp) > 2) {
            return true
        }

        // If humidity changed significantly (more than 5%)
        if (abs(currentHumidity - lastHumidity) > 5) {
            return true
        }

        return false
    }
}