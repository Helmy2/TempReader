package com.example.tempreader.ui.util

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen() {
    @Serializable
    object Login : Screen()
    @Serializable
    object Main : Screen()
}