package com.example.tempreader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tempreader.data.model.Reading
import com.example.tempreader.util.formatTimestampChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Scroll.Absolute
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import kotlin.math.roundToInt

@Composable
fun HumidityChart(readings: List<Reading>, modifier: Modifier = Modifier) {
    val currentDayReadings = readings.sortedBy { it.timestamp }.fold(
        mutableListOf<Reading>()
    ) { accumulator, currentReading ->
        if (accumulator.isEmpty() || accumulator.last().humidity.roundToInt() != currentReading.humidity.roundToInt()) {
            accumulator.add(currentReading)
        }
        accumulator
    }

    if (currentDayReadings.isEmpty()) {
        Text(
            text = "No humidity data available for today",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    val chartModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(currentDayReadings) {
        chartModelProducer.runTransaction {
            lineSeries {
                series(currentDayReadings.map { it.humidity })
            }
        }
    }

    val scrollState = rememberVicoScrollState(
        initialScroll = Absolute.End,
        scrollEnabled = true
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Humidity History (%)",
                style = MaterialTheme.typography.titleMedium,
            )
            CartesianChartHost(
                zoomState = rememberVicoZoomState(
                    initialZoom = Zoom.Companion.x(20.0)
                ),
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill(MaterialTheme.colorScheme.primary)
                                )
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { x, value, y ->
                            currentDayReadings.getOrNull(value.toInt())?.timestamp?.formatTimestampChart()
                                ?: "N/A"
                        },
                    )
                ),
                modelProducer = chartModelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                scrollState = scrollState
            )
        }
    }
}