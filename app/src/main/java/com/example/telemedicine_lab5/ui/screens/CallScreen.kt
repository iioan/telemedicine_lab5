package com.example.telemedicine_lab5.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.telemedicine_lab5.signaling.SignalingMessage
import com.example.telemedicine_lab5.signaling.SocketSignalingClient
import com.example.telemedicine_lab5.webrtc.WebRtcSessionManager
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

private const val TAG = "CallScreen"

@Composable
fun CallScreen(
    navController: NavController,
    role: String,
    roomId: String,
    serverUrl: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val signalingClient = remember { SocketSignalingClient() }
    val sessionManager = remember { WebRtcSessionManager(context.applicationContext) }

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    var status by remember { mutableStateOf("Requesting camera and microphone permissions...") }
    var isMuted by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var hasRemoteVideo by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var offerSent by remember { mutableStateOf(false) }
    var isOfferer by remember { mutableStateOf(false) }
    var isRecoveringIce by remember { mutableStateOf(false) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true

        if (!granted) {
            status = "Camera and microphone permissions are required"
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            runCatching {
                startSession(
                    roomId = roomId,
                    serverUrl = serverUrl,
                    signalingClient = signalingClient,
                    sessionManager = sessionManager,
                    onStatusChange = { status = it },
                    onConnected = { isConnected = it },
                    onRemoteVideo = { hasRemoteVideo = true },
                    onIncomingMessage = { message ->
                        scope.launch {
                            handleSignalingMessage(message, roomId, signalingClient, sessionManager, onStatusChange = { status = it })
                        }
                    },
                    onReadyToOffer = {
                        scope.launch {
                            if (offerSent) return@launch
                            runCatching {
                                isOfferer = true
                                offerSent = true
                                status = "Creating offer..."
                                val offer = sessionManager.createOffer()
                                signalingClient.sendOffer(roomId, offer.description)
                                status = "Offer sent, waiting for answer"
                            }.onFailure {
                                offerSent = false
                                status = "Offer error: ${it.message}"
                            }
                        }
                    },
                    onIceFailed = {
                        if (!started || isRecoveringIce) {
                            Unit
                        } else if (!isOfferer) {
                            status = "Peer reconnection in progress..."
                        } else {
                            scope.launch {
                                isRecoveringIce = true
                                runCatching {
                                    status = "Connection failed, restarting ICE..."
                                    val restartOffer = sessionManager.createIceRestartOffer()
                                    signalingClient.sendOffer(roomId, restartOffer.description)
                                    status = "ICE restart offer sent"
                                }.onFailure {
                                    status = "ICE restart error: ${it.message}"
                                }
                                isRecoveringIce = false
                            }
                        }
                    }
                )
                started = true
            }.onFailure {
                status = it.message ?: "Failed to start consultation"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            signalingClient.disconnect()
            sessionManager.dispose()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermissions) {
            hasRequestedPermissions = true
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Consultatie Video Live")
                Text("Role: $role | Room: $roomId")
                Text("Status: $status", style = MaterialTheme.typography.bodySmall)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { viewContext ->
                    SurfaceViewRenderer(viewContext).also { renderer ->
                        remoteRenderer = renderer
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { renderer ->
                    if (localRenderer != null && remoteRenderer != null) {
                        sessionManager.initRenderers(localRenderer!!, remoteRenderer!!)
                    }
                    remoteRenderer = renderer
                },
            )


            if (started && !hasRemoteVideo) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            AndroidView(
                factory = { viewContext ->
                    SurfaceViewRenderer(viewContext).also { renderer ->
                        localRenderer = renderer
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 110.dp)
                    .size(width = 120.dp, height = 180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                update = { renderer ->
                    if (localRenderer != null && remoteRenderer != null) {
                        sessionManager.initRenderers(localRenderer!!, remoteRenderer!!)
                    }
                    localRenderer = renderer
                },
            )

            ControlBar(
                isMuted = isMuted,
                enabled = started,
                onToggleMute = {
                    val isMicEnabled = sessionManager.toggleMute()
                    isMuted = !isMicEnabled
                },
                onSwitchCamera = {
                    sessionManager.switchCamera()
                },
                onHangUp = {
                    signalingClient.disconnect()
                    sessionManager.hangUp()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
            )
        }
    }
}

private suspend fun startSession(
    roomId: String,
    serverUrl: String,
    signalingClient: SocketSignalingClient,
    sessionManager: WebRtcSessionManager,
    onStatusChange: (String) -> Unit,
    onConnected: (Boolean) -> Unit,
    onRemoteVideo: () -> Unit,
    onIncomingMessage: (SignalingMessage) -> Unit,
    onReadyToOffer: () -> Unit,
    onIceFailed: () -> Unit,
) {
    Log.d(TAG, "Starting session room=$roomId server=$serverUrl")
    onStatusChange("Opening user media")
    sessionManager.openUserMedia()

    onStatusChange("Creating peer connection")
    sessionManager.createPeerConnection(
        onIceCandidate = { signalingClient.sendCandidate(roomId, it) },
        onConnectionStateChange = { state ->
            onConnected(state == PeerConnection.PeerConnectionState.CONNECTED)
            if (state == PeerConnection.PeerConnectionState.FAILED) {
                onIceFailed()
            }
            onStatusChange(
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTING -> "Connecting"
                    PeerConnection.PeerConnectionState.CONNECTED -> "Connected"
                    PeerConnection.PeerConnectionState.FAILED -> "Failed - retrying ICE"
                    PeerConnection.PeerConnectionState.DISCONNECTED -> "Disconnected"
                    PeerConnection.PeerConnectionState.CLOSED -> "Closed"
                    PeerConnection.PeerConnectionState.NEW -> "New"
                },
            )
        },
        onRemoteTrack = onRemoteVideo,
    )

    onStatusChange("Connecting signaling server")
    signalingClient.connect(
        serverUrl = serverUrl,
        roomId = roomId,
        onConnected = {
            onStatusChange("Joined room $roomId. Waiting for partner...")
        },
        onReady = {
            onStatusChange("Partner joined. Starting negotiation...")
            onReadyToOffer()
        },
        onError = { error ->
            onStatusChange("Error: $error")
        },
        onMessage = onIncomingMessage,
    )
}

private suspend fun handleSignalingMessage(
    message: SignalingMessage,
    roomId: String,
    signalingClient: SocketSignalingClient,
    sessionManager: WebRtcSessionManager,
    onStatusChange: (String) -> Unit,
) {
    runCatching {
        when (message.type) {
            "offer" -> {
                val remoteSdp = message.sdp ?: return@runCatching
                Log.d(TAG, "Received offer from peer")
                onStatusChange("Setting remote offer...")
                sessionManager.setRemoteDescription("offer", remoteSdp)
                onStatusChange("Creating answer...")
                val answer = sessionManager.createAnswer()
                signalingClient.sendAnswer(roomId, answer.description)
                onStatusChange("Answer sent")
            }

            "answer" -> {
                val remoteSdp = message.sdp ?: return@runCatching
                Log.d(TAG, "Received answer from peer")
                onStatusChange("Setting remote answer...")
                sessionManager.setRemoteDescription("answer", remoteSdp)
                onStatusChange("Connecting...")
            }

            "candidate" -> {
                val candidate = message.candidate ?: return@runCatching
                val sdpMLineIndex = message.sdpMLineIndex ?: return@runCatching
                val sdpMid = message.sdpMid ?: "0"
                Log.d(TAG, "Received ICE candidate (mid=$sdpMid, line=$sdpMLineIndex)")
                sessionManager.addCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
            }
        }
    }.onFailure { e ->
        Log.e(TAG, "Error handling signaling message", e)
        onStatusChange("Signaling Error: ${e.message}")
    }
}

@Composable
private fun ControlBar(
    isMuted: Boolean,
    enabled: Boolean,
    onToggleMute: () -> Unit,
    onSwitchCamera: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingActionButton(
            onClick = onToggleMute,
            containerColor = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute toggle",
                tint = Color(0xFF1565C0),
            )
        }

        FloatingActionButton(
            onClick = onHangUp,
            containerColor = Color.Red,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End call",
                tint = Color.White,
            )
        }

        FloatingActionButton(
            onClick = onSwitchCamera,
            containerColor = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp),
            ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch camera",
                tint = Color(0xFF1565C0),
            )
        }
    }
}
