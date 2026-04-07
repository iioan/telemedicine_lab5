package com.example.telemedicine_lab5

import com.example.telemedicine_lab5.signaling.parseSignalingMessage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalingMessageParserTest {

    @Test
    fun `parse offer payload`() {
        val payload = JSONObject()
            .put("type", "offer")
            .put("sdp", "test-sdp")

        val message = parseSignalingMessage(payload)

        assertEquals("offer", message.type)
        assertEquals("test-sdp", message.sdp)
        assertNull(message.candidate)
    }

    @Test
    fun `parse candidate payload`() {
        val payload = JSONObject()
            .put("type", "candidate")
            .put("candidate", "candidate:1")
            .put("sdpMid", "0")
            .put("sdpMLineIndex", 0)

        val message = parseSignalingMessage(payload)

        assertEquals("candidate", message.type)
        assertEquals("candidate:1", message.candidate)
        assertEquals("0", message.sdpMid)
        assertEquals(0, message.sdpMLineIndex)
    }
}

