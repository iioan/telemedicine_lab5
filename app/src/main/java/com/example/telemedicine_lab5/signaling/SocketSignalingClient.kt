package com.example.telemedicine_lab5.signaling

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate
import java.net.URI

class SocketSignalingClient {
    private var socket: Socket? = null

    fun connect(
        serverUrl: String,
        roomId: String,
        onConnected: () -> Unit,
        onReady: () -> Unit,
        onError: (String) -> Unit,
        onMessage: (SignalingMessage) -> Unit,
    ) {
        disconnect()

        val normalizedUrl = normalizeServerUrl(serverUrl)

        val options = IO.Options.builder()
            .setReconnection(true)
            .setReconnectionAttempts(10)
            .setTimeout(10_000)
            .setForceNew(true)
            .setPath("/socket.io/")
            // Use default websocket + polling
            .setTransports(arrayOf("websocket", "polling"))
            .build()

        socket = IO.socket(URI.create(normalizedUrl), options).apply {
            on(Socket.EVENT_CONNECT) {
                emit("join", roomId)
                onConnected()
            }

            on("ready") {
                onReady()
            }

            on("full") { args ->
                val room = args.firstOrNull()?.toString() ?: ""
                onError("Room $room is full")
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                onError(args.firstOrNull()?.toString() ?: "Socket connection error")
            }

            on("error") { args ->
                onError(args.firstOrNull()?.toString() ?: "Socket error")
            }

            on(Socket.EVENT_DISCONNECT) { args ->
                val reason = args.firstOrNull()?.toString() ?: "unknown"
                onError("Socket disconnected: $reason")
            }

            on("message") { args ->
                val payload = args.firstOrNull()
                if (payload is JSONObject) {
                    val normalizedPayload = if (payload.has("payload") && payload.opt("payload") is JSONObject) {
                        payload.optJSONObject("payload") ?: payload
                    } else {
                        payload
                    }
                    onMessage(parseSignalingMessage(normalizedPayload))
                }
            }

            connect()
        }
    }

    private fun normalizeServerUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    fun sendOffer(roomId: String, sdp: String) {
        sendPayload(roomId, JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp)
        })
    }

    fun sendAnswer(roomId: String, sdp: String) {
        sendPayload(roomId, JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp)
        })
    }

    fun sendCandidate(roomId: String, candidate: IceCandidate) {
        sendPayload(roomId, JSONObject().apply {
            put("type", "candidate")
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        })
    }

    private fun sendPayload(roomId: String, payload: JSONObject) {
        socket?.emit(
            "message",
            JSONObject().apply {
                put("room", roomId)
                put("payload", payload)
            },
        )
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }
}
