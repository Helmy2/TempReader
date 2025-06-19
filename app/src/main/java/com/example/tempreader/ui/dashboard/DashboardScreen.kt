package com.example.tempreader.ui.dashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tempreader.service.PreferencesManager
import com.example.tempreader.ui.components.SensorDataChart
import com.example.tempreader.ui.components.ThresholdDialog
import com.example.tempreader.ui.util.formatTimestamp
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val readings by viewModel.readings.collectAsState()

    val context = LocalContext.current
    val latestReading = readings.lastOrNull()
    val scrollState = rememberScrollState()
    val preferencesManager = remember { PreferencesManager(context) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showPermissionPrompt by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionPrompt = true
            Toast.makeText(
                context,
                "Notifications disabled. High temperature alerts won't work.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            showPermissionPrompt = false
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AnimatedVisibility(showPermissionPrompt) {
        AlertDialog(onDismissRequest = {
            showPermissionPrompt = false
        }, confirmButton = {
            Button({
                showPermissionPrompt = false
                val intent = Intent(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                ).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, "com.example.tempreader")
                }
                context.startActivity(intent)
            }) {
                Text("Confirm")
            }
        }, title = {
            Text("You should allow notifications to use temperature alerts")
        })

    }

    if (showThresholdDialog) {
        ThresholdDialog(
            onDismiss = { showThresholdDialog = false },
            onConfirm = { lower, upper ->
                preferencesManager.setTemperatureThresholds(lower, upper)
                Toast.makeText(
                    context, "Temperature thresholds updated", Toast.LENGTH_SHORT
                ).show()
            },
            currentLower = preferencesManager.getLowerTemperatureThreshold(),
            currentUpper = preferencesManager.getUpperTemperatureThreshold()
        )
    }
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.Companion.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Text(
                        text = "Latest Reading",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { showThresholdDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Set temperature thresholds",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "Temperature: ${latestReading?.temperature}°C",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Humidity: ${latestReading?.humidity}%",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Latest Reading: ${latestReading?.timestamp?.formatTimestamp()}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thresholds: ${preferencesManager.getLowerTemperatureThreshold()}°C - ${preferencesManager.getUpperTemperatureThreshold()}°C",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(readings.isNotEmpty()) {
            Column {
                SensorDataChart(
                    readings = readings,
                    chartTitle = "Temperature History (°C)",
                    lineAndFillColor = MaterialTheme.colorScheme.primary,
                    valueSelector = { it.temperature })

                SensorDataChart(
                    readings = readings,
                    chartTitle = "Humidity History (%)",
                    lineAndFillColor = MaterialTheme.colorScheme.secondary,
                    valueSelector = { it.humidity })
            }
        }
    }
}