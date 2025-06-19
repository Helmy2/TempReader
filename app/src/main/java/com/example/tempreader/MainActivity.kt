package com.example.tempreader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.example.tempreader.service.TemperatureCheckService
import com.example.tempreader.ui.AppNavigation
import com.example.tempreader.ui.theme.TempReaderTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startForegroundService(Intent(this, TemperatureCheckService::class.java))

        setContent {
            TempReaderTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}