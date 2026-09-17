@file:OptIn(ExperimentalTime::class)

package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.network.model.NetworkCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal abstract class NetworkDebugRepositoryTestBase {
    protected abstract fun createRepository(
        config: ProbeCaptureLimits,
        scope: CoroutineScope,
    ): NetworkDebugRepository

    private fun sampleCall(
        id: String,
        timestampMillis: Long = Clock.System.now().toEpochMilliseconds(),
        path: String = "/test",
        method: String = "GET",
    ) = NetworkCall(
        id = id,
        timestampMillis = timestampMillis,
        method = method,
        url = "https://api.example.com$path",
        host = "api.example.com",
        path = path,
        query = null,
        requestHeaders = emptyMap(),
        requestBody = null,
        responseStatus = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10L,
        error = null,
        isComplete = true,
    )

    @Test
    fun evictsOldestWhenOverMaxEntries() =
        runTest {
            val repository = createRepository(ProbeCaptureLimits(maxEntries = 2), this)
            val now = Clock.System.now().toEpochMilliseconds()

            repository.insertPending(sampleCall("1", timestampMillis = now - 3_000))
            repository.insertPending(sampleCall("2", timestampMillis = now - 2_000))
            repository.insertPending(sampleCall("3", timestampMillis = now - 1_000))
            advanceUntilIdle()

            val calls = repository.observeActiveCalls("").first()
            assertEquals(2, calls.size)
            assertEquals("3", calls[0].id)
            assertEquals("2", calls[1].id)
        }

    @Test
    fun clearRemovesAll() =
        runTest {
            val repository = createRepository(ProbeCaptureLimits(maxEntries = 10), this)

            repository.insertPending(sampleCall("1"))
            val sessionId = requireNotNull(repository.resolveSessionId(null))
            repository.clear(sessionId)
            advanceUntilIdle()

            assertEquals(0, repository.observeActiveCalls("").first().size)
        }

    @Test
    fun searchFiltersByPath() =
        runTest {
            val repository = createRepository(ProbeCaptureLimits(), this)

            repository.insertPending(sampleCall("1", path = "/foo"))
            repository.insertPending(sampleCall("2", path = "/bar", method = "POST"))
            advanceUntilIdle()

            val calls = repository.observeActiveCalls("foo").first()
            assertEquals(1, calls.size)
            assertEquals("/foo", calls[0].path)
        }
}
