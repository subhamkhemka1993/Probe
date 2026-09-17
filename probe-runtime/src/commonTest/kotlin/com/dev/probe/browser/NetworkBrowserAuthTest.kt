package com.dev.probe.browser

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkBrowserAuthTest {
    @Test
    fun rejectsMissingToken() {
        val session = BrowserSession("secret", 0L, Long.MAX_VALUE)
        assertEquals(
            false,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = null,
                queryToken = null,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun acceptsBearerToken() {
        val session = BrowserSession("secret", 0L, Long.MAX_VALUE)
        assertEquals(
            true,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = "secret",
                queryToken = null,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun rejectsExpiredSession() {
        val session = BrowserSession("secret", 0L, expiresAtMillis = 100L)
        assertEquals(
            false,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = "secret",
                queryToken = null,
                nowMillis = 200L,
            ),
        )
    }

    @Test
    fun acceptsQueryToken() {
        val session = BrowserSession("secret", 0L, Long.MAX_VALUE)
        assertEquals(
            true,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = null,
                queryToken = "secret",
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun stripsBearerPrefix() {
        val session = BrowserSession("secret", 0L, Long.MAX_VALUE)
        assertEquals(
            true,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = "Bearer secret",
                queryToken = null,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun rejectsNullSession() {
        assertEquals(
            false,
            NetworkBrowserAuth.isAuthorized(
                session = null,
                bearerToken = "secret",
                queryToken = null,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun fallsBackToQueryTokenWhenHeaderDoesNotMatch() {
        val session = BrowserSession("secret", 0L, Long.MAX_VALUE)
        assertEquals(
            true,
            NetworkBrowserAuth.isAuthorized(
                session = session,
                bearerToken = "garbage",
                queryToken = "secret",
                nowMillis = 1L,
            ),
        )
    }
}
