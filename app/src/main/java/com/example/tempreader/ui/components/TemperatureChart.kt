package com.example.tempreader.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.tempreader.data.model.Reading
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.Calendar

@Composable
fun TemperatureChart(readings: List<Reading>, modifier: Modifier = Modifier) {
    // Filter readings for current day
    val currentDayReadings = readings.filter { reading ->
        val calendar = Calendar.getInstance()

        // Get reading's day
        calendar.timeInMillis = reading.timestamp * 1000
        val readingDay = calendar.get(Calendar.DAY_OF_YEAR)

        // Get current day
        calendar.timeInMillis = System.currentTimeMillis()
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        readingDay == currentDay
    }.sortedBy { it.timestamp }

    if (currentDayReadings.isEmpty()) {
        Text(
            text = "No temperature data available for today",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    val chartModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(currentDayReadings) {
        chartModelProducer.runTransaction {
            lineSeries {
                series(
                    currentDayReadings.map { it.temperature }
                )
            }
        }
    }

    ChartCard(
        title = "Temperature History (°C)",
        chartModelProducer = chartModelProducer,
        modifier = modifier
    )
}