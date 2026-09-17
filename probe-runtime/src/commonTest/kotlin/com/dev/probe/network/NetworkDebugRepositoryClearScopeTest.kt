package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the C3 fix: [NetworkDebugRepository.clear] must scope its delete to the session passed
 * in, not wipe every session's captured calls (the old `dao.clearAll()` behavior). Uses the
 * shared [InMemoryNetworkCallDao] fixture so calls can be seeded directly into arbitrary session
 * ids, bypassing [DebugSessionManager]'s single-active-session bootstrap.
 */
internal class NetworkDebugRepositoryClearScopeTest {
    @Test
    fun clearIsScopedToOneSession() =
        runTest {
            val dao = InMemoryNetworkCallDao()
            val repository =
                NetworkDebugRepository(
                    dao = dao,
                    config = ProbeCaptureLimits(),
                    scope = this,
                    sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), dao),
                )
            dao.insert(sampleCall(id = "call-a", sessionId = "session-a"))
            dao.insert(sampleCall(id = "call-b", sessionId = "session-b"))

            repository.clear(sessionId = "session-a")

            assertTrue(repository.observeCalls("session-a", "").first().isEmpty())
            assertEquals(1, repository.observeCalls("session-b", "").first().size)
        }

    private fun sampleCall(
        id: String,
        sessionId: String,
    ) = NetworkCallEntity(
        id = id,
        timestampMillis = 0L,
        method = "GET",
        url = "https://api.example.com/$id",
        host = "api.example.com",
        path = "/$id",
        query = null,
        requestHeadersJson = "{}",
        requestBody = null,
        responseStatus = 200,
        responseHeadersJson = null,
        responseBody = null,
        durationMs = 10L,
        error = null,
        isComplete = true,
        sessionId = sessionId,
    )
}
