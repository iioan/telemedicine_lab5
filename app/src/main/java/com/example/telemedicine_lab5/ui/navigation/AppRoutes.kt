package com.example.telemedicine_lab5.ui.navigation

object AppRoutes {
    const val HOME = "home"
    const val CALL_PATTERN = "call/{role}/{roomId}/{serverUrl}"

    fun callRoute(role: String, roomId: String, serverUrl: String): String {
        return "call/$role/$roomId/$serverUrl"
    }
}

