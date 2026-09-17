package com.dev.probe.network

import com.dev.probe.network.model.NetworkCall
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurlBuilderTest {

    @Test
    fun buildsCurlWithRedactedAuthorization() {
        val call = sampleCall(
            method = "POST",
            url = "https://api.zebpay.com/v1/foo",
            requestHeaders = mapOf(
                "Authorization" to "***",
                "Content-Type" to "application/json",
            ),
            requestBody = """{"a":1}""",
        )

        val curl = buildCurl(call)

        assertTrue(curl.startsWith("curl -X POST"))
        assertTrue(curl.contains("https://api.zebpay.com/v1/foo"))
        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
        assertFalse(curl.contains("Bearer"))
    }

    @Test
    fun buildsCurlWithPostJsonBody() {
        val call = sampleCall(
            method = "POST",
            url = "https://api.zebpay.com/v1/login",
            requestHeaders = mapOf("Content-Type" to "application/json"),
            requestBody = """{"email":"user@example.com","password":"secret"}""",
        )

        val curl = buildCurl(call)

        assertTrue(curl.startsWith("curl -X POST"))
        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
        assertTrue(curl.contains("""-d '{"email":"user@example.com","password":"secret"}'"""))
        assertTrue(curl.contains("'https://api.zebpay.com/v1/login'"))
    }

    private fun sampleCall(
        method: String = "GET",
        url: String = "https://api.example.com/test",
        requestHeaders: Map<String, String> = emptyMap(),
        requestBody: String? = null,
    ) = NetworkCall(
        id = "1",
        timestampMillis = 0L,
        method = method,
        url = url,
        host = "api.example.com",
        path = "/test",
        query = null,
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        responseStatus = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10L,
        error = null,
        isComplete = true,
    )
}
