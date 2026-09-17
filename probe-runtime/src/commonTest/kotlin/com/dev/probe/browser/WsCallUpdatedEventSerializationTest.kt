package com.dev.probe.browser

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class WsCallUpdatedEventSerializationTest {
    @Test
    fun encodesEventTypeForBrowserWebSocketClient() {
        val payload =
            Json {
                encodeDefaults = true
            }.encodeToString(
                WsCallUpdatedEvent(
                    call =
                        NetworkCallDto(
                            id = "call-1",
                            timestampIso = "2026-07-08T16:29:08.616Z",
                            method = "GET",
                            url = "https://www.zebapi.com/api/v1/coins/IN",
                            host = "www.zebapi.com",
                            path = "/api/v1/coins/IN",
                            query = null,
                            requestHeaders = emptyMap(),
                            requestBody = null,
                            responseStatus = 200,
                            responseHeaders = emptyMap(),
                            responseBody = null,
                            durationMs = 120L,
                            error = null,
                            isComplete = true,
                        ),
                ),
            )

        assertTrue(payload.contains("\"type\":\"call_updated\""))
        assertTrue(payload.contains("\"id\":\"call-1\""))
    }
}
