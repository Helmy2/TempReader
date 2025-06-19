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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tempreader.data.local.Reading
import com.example.tempreader.util.formatTimestampChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import kotlin.math.roundToInt

@Composable
fun SensorDataChart(
    readings: List<Reading>,
    modifier: Modifier = Modifier,
    chartTitle: String,
    noDataText: String,
    lineAndFillColor: Color = MaterialTheme.colorScheme.primary,
    valueSelector: (Reading) -> Float
) {
    val filteredReadings = remember(readings, valueSelector) {
        val sortedReadings = readings.sortedBy { it.timestamp }
        if (sortedReadings.size < 2) {
            return@remember sortedReadings
        }

        // Use buildList for an efficient way to construct the final list.
        buildList {
            // Iterate through all but the last reading.
            sortedReadings.forEachIndexed { index, reading ->
                if (index == sortedReadings.lastIndex) return@forEachIndexed

                // Add the reading if it's the first one, or if its value
                // is different from the previous one.
                if (index == 0 || valueSelector(reading).roundToInt() != valueSelector(sortedReadings[index - 1]).roundToInt()) {
                    add(reading)
                }
            }
            // Finally, unconditionally add the very last reading to ensure the chart is up-to-date.
            add(sortedReadings.last())
        }
    }

    if (filteredReadings.isEmpty()) {
        Text(
            text = noDataText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val chartModelProducer = remember { CartesianChartModelProducer() }

    // Update the chart model whenever the filtered data changes.
    LaunchedEffect(filteredReadings) {
        chartModelProducer.runTransaction {
            lineSeries {
                series(y = filteredReadings.map(valueSelector))
            }
        }
    }

    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End, scrollEnabled = true
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = chartTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CartesianChartHost(
                zoomState = rememberVicoZoomState(initialZoom = Zoom.x(30.0)),
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill(lineAndFillColor)
                                )
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { _, value, _ ->
                            filteredReadings.getOrNull(value.toInt())?.timestamp?.formatTimestampChart()
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
