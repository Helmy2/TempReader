package com.example.tempreader.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tempreader.ui.util.Screen
import com.example.tempreader.ui.auth.AuthViewModel
import com.example.tempreader.ui.auth.LoginScreen
import com.example.tempreader.ui.dashboard.DashboardScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = koinViewModel()
    val currentUser = authViewModel.currentUser

    val startDestination = if (currentUser != null) Screen.Main else Screen.Login

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Login> {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { user ->
                    navController.navigate(Screen.Main) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Main> {
            DashboardScreen()
        }
    }
}