package com.dev.probe.network

import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderRedactorTest {
    @Test
    fun redactsAuthorizationCaseInsensitive() {
        val input = mapOf("Authorization" to "Bearer secret", "Content-Type" to "application/json")
        val result = redactHeaders(input)
        assertEquals("***", result["Authorization"])
        assertEquals("application/json", result["Content-Type"])
    }

    @Test
    fun redactsLowercaseSessionToken() {
        val input = mapOf("sessiontoken" to "abc123")
        val result = redactHeaders(input)
        assertEquals("***", result["sessiontoken"])
    }

    @Test
    fun redactsCookie() {
        val input = mapOf("Cookie" to "session=abc", "Accept" to "application/json")
        val result = redactHeaders(input)
        assertEquals("***", result["Cookie"])
        assertEquals("application/json", result["Accept"])
    }

    @Test
    fun redactsXApiKeyCaseInsensitive() {
        val input = mapOf("x-api-key" to "secret-key")
        val result = redactHeaders(input)
        assertEquals("***", result["x-api-key"])
    }

    @Test
    fun redactsXAuthToken() {
        val input = mapOf("X-Auth-Token" to "token-value")
        val result = redactHeaders(input)
        assertEquals("***", result["X-Auth-Token"])
    }
}
