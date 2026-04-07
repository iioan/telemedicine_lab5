package com.example.telemedicine_lab5.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.telemedicine_lab5.ui.screens.CallScreen
import com.example.telemedicine_lab5.ui.screens.HomeScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(route = AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(
            route = AppRoutes.CALL_PATTERN,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType },
                navArgument("serverUrl") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val role = Uri.decode(backStackEntry.arguments?.getString("role").orEmpty())
            val roomId = Uri.decode(backStackEntry.arguments?.getString("roomId").orEmpty())
            val serverUrl = Uri.decode(backStackEntry.arguments?.getString("serverUrl").orEmpty())

            CallScreen(
                navController = navController,
                role = role,
                roomId = roomId,
                serverUrl = serverUrl,
            )
        }
    }
}

