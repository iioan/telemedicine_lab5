package com.example.telemedicine_lab5.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.telemedicine_lab5.ui.navigation.AppRoutes

@Composable
fun HomeScreen(navController: NavController) {
    var roomId by rememberSaveable { mutableStateOf("camera_101") }
    var serverUrl by rememberSaveable { mutableStateOf("http://10.0.2.2:3000") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Telemedicine WebRTC")
            Text("Use Node signaling URL. Emulator localhost is http://10.0.2.2:3000.")

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Signaling URL") },
                singleLine = true,
            )

            OutlinedTextField(
                value = roomId,
                onValueChange = { roomId = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Room ID") },
                singleLine = true,
            )

            Button(
                onClick = {
                    navController.navigate(
                        AppRoutes.callRoute(
                            role = "doctor",
                            roomId = Uri.encode(roomId),
                            serverUrl = Uri.encode(serverUrl),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = roomId.isNotBlank() && serverUrl.isNotBlank(),
            ) {
                Text("Start Consultation")
            }

            Button(
                onClick = {
                    navController.navigate(
                        AppRoutes.callRoute(
                            role = "patient",
                            roomId = Uri.encode(roomId),
                            serverUrl = Uri.encode(serverUrl),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = roomId.isNotBlank() && serverUrl.isNotBlank(),
            ) {
                Text("Join as Patient")
            }
        }
    }
}

