package com.example.telemedicine_lab5.signaling

import org.json.JSONObject

data class SignalingMessage(
    val type: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
)

fun parseSignalingMessage(payload: JSONObject): SignalingMessage {
    val sdpMLineIndex = when (val raw = payload.opt("sdpMLineIndex")) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }

    return SignalingMessage(
        type = payload.optString("type"),
        sdp = payload.optString("sdp").ifBlank { null },
        candidate = payload.optString("candidate").ifBlank { null },
        sdpMid = payload.optString("sdpMid").ifBlank { null },
        sdpMLineIndex = sdpMLineIndex,
    )
}

