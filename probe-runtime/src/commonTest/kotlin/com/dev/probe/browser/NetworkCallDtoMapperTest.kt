package com.dev.probe.browser

import com.dev.probe.network.model.NetworkCall
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkCallDtoMapperTest {
    @Test
    fun mapsDomainToDtoWithIsoTimestamp() {
        val call = NetworkCall(
            id = "abc",
            timestampMillis = 1_700_000_000_000L,
            method = "GET",
            url = "https://api.example.com/foo",
            host = "api.example.com",
            path = "/foo",
            query = null,
            requestHeaders = mapOf("Authorization" to "***"),
            requestBody = null,
            responseStatus = 200,
            responseHeaders = emptyMap(),
            responseBody = """{"ok":true}""",
            durationMs = 42L,
            error = null,
            isComplete = true,
        )
        val dto = call.toDto()
        assertEquals("abc", dto.id)
        assertEquals("2023-11-14T22:13:20Z", dto.timestampIso)
        assertEquals("GET", dto.method)
        assertEquals("***", dto.requestHeaders["Authorization"])
        assertEquals(200, dto.responseStatus)
    }
}
