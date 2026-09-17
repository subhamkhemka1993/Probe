package com.dev.probe.network.ui

import com.dev.probe.network.model.NetworkCall
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkResponseSizeTest {

    @Test
    fun formatNetworkByteSize_usesBytesForSmallValues() {
        assertEquals("512 B", formatNetworkByteSize(512))
    }

    @Test
    fun formatNetworkByteSize_usesKilobytes() {
        assertEquals("1.5 KB", formatNetworkByteSize(1_536))
    }

    @Test
    fun responseSizeLabel_prefersContentLengthHeader() {
        val call = sampleCall(
            responseHeaders = mapOf("Content-Length" to "2048"),
            responseBody = "{}",
        )
        assertEquals("2 KB", call.responseSizeLabel())
    }

    @Test
    fun responseSizeLabel_usesCapturedBodyWhenNoContentLength() {
        val body = """{"token":"abc"}"""
        val call = sampleCall(responseBody = body)
        assertEquals("${body.encodeToByteArray().size} B", call.responseSizeLabel())
    }

    @Test
    fun responseSizeLabel_showsPendingWhenIncomplete() {
        val call = sampleCall(isComplete = false)
        assertEquals("…", call.responseSizeLabel())
    }

    private fun sampleCall(
        responseHeaders: Map<String, String>? = mapOf("content-type" to "application/json"),
        responseBody: String? = null,
        isComplete: Boolean = true,
    ) = NetworkCall(
        id = "test",
        timestampMillis = 0L,
        method = "GET",
        url = "https://example.com",
        host = "example.com",
        path = "/",
        query = null,
        requestHeaders = emptyMap(),
        requestBody = null,
        responseStatus = if (isComplete) 200 else null,
        responseHeaders = responseHeaders,
        responseBody = responseBody,
        durationMs = 100L,
        error = null,
        isComplete = isComplete,
    )
}
