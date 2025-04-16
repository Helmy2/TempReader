package com.example.tempreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ThresholdDialog(
    onDismiss: () -> Unit,
    onConfirm: (lower: Float, upper: Float) -> Unit,
    currentLower: Float,
    currentUpper: Float
) {
    var lowerTemp by remember { mutableStateOf(currentLower.toString()) }
    var upperTemp by remember { mutableStateOf(currentUpper.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.Companion.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set Temperature Thresholds",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = lowerTemp,
                    onValueChange = { lowerTemp = it },
                    label = { Text("Lower Threshold (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                    singleLine = true,
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                OutlinedTextField(
                    value = upperTemp,
                    onValueChange = { upperTemp = it },
                    label = { Text("Upper Threshold (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                    singleLine = true,
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.Companion.width(8.dp))
                    Button(onClick = {
                        try {
                            val lower = lowerTemp.toFloat()
                            val upper = upperTemp.toFloat()
                            if (lower >= upper) {
                                error = "Lower threshold must be less than upper threshold"
                                return@Button
                            }
                            onConfirm(lower, upper)
                            onDismiss()
                        } catch (_: NumberFormatException) {
                            error = "Please enter valid numbers"
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}