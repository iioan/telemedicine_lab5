package com.example.telemedicine_lab5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.telemedicine_lab5.ui.navigation.AppNavGraph
import com.example.telemedicine_lab5.ui.theme.Telemedicine_lab5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Telemedicine_lab5Theme {
                AppNavGraph()
            }
        }
    }
}
