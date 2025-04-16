package com.example.tempreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.tempreader.ui.App
import com.example.tempreader.ui.MainViewModel
import com.example.tempreader.ui.theme.TempReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel by viewModels<MainViewModel>()
        setContent {
            TempReaderTheme {
                App(viewModel = viewModel)
            }
        }
    }
}