@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package com.dev.probe.browser

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class NetworkBrowserControllerTest {

    @Test
    fun startPublishesRunningConnectionInfo() = runTest {
        val fakeServer = FakeBrowserServer()
        val controller = createController(
            fakeServer = fakeServer,
            scope = this,
        )

        controller.start()

        val running = assertIs<BrowserConnectionInfo.Running>(controller.connectionInfo.value)
        assertEquals(8765, running.port)
        assertEquals("token-1", running.token)
        assertEquals("http://192.168.1.10:8765/?token=token-1", running.wifiUrl)
        assertEquals("http://10.0.2.2:8765/?token=token-1", running.emulatorUrl)
        assertEquals("http://127.0.0.1:8765/?token=token-1", running.simulatorUrl)

        controller.stop()

        assertEquals(BrowserConnectionInfo.Stopped, controller.connectionInfo.value)
        assertFalse(fakeServer.isRunning)
    }

    @Test
    fun stopRotatesTokenOnNextStart() = runTest {
        val controller = createController(
            fakeServer = FakeBrowserServer(),
            scope = this,
        )

        controller.start()
        val first = (controller.connectionInfo.value as BrowserConnectionInfo.Running).token

        controller.stop()
        controller.start()
        val second = (controller.connectionInfo.value as BrowserConnectionInfo.Running).token

        assertTrue(first != second)

        controller.stop()
    }

    @Test
    fun startIsNoOpWhenDisabled() = runTest {
        val fakeServer = FakeBrowserServer()
        val controller = createController(
            fakeServer = fakeServer,
            scope = this,
            isEnabled = { false },
        )

        controller.start()

        assertEquals(BrowserConnectionInfo.Stopped, controller.connectionInfo.value)
        assertFalse(fakeServer.isRunning)
    }

    @Test
    fun stopIsNoOpWhenDisabledAndNeverConstructsTheServer() = runTest {
        var factoryInvocations = 0
        val controller = NetworkBrowserController(
            repository = createRepository(this),
            config = NetworkBrowserConfig(),
            addressProvider = FakeAddressProvider(),
            scope = this,
            isEnabled = { false },
            serverFactory = {
                factoryInvocations += 1
                FakeBrowserServer()
            },
        )

        controller.stop()

        assertEquals(
            0,
            factoryInvocations,
            "stop() on a disabled controller must never construct the lazy server",
        )
    }

    @Test
    fun repositoryUpdatesBroadcastChangedCalls() = runTest {
        val fakeServer = FakeBrowserServer()
        val repository = createRepository(this)
        val controller = createController(
            repository = repository,
            fakeServer = fakeServer,
            scope = this,
        )

        controller.start()
        repository.insertPending(sampleCall(id = "call-1", path = "/markets"))
        advanceUntilIdle()

        assertEquals(listOf("call-1"), fakeServer.broadcastedCalls.map { it.id })

        controller.stop()
        repository.insertPending(sampleCall(id = "call-2", path = "/orders"))
        advanceUntilIdle()

        assertEquals(listOf("call-1"), fakeServer.broadcastedCalls.map { it.id })
    }

    @Test
    fun stopsWhenBackgroundedAndAutoStopEnabled() = runTest {
        val fakeServer = FakeBrowserServer()
        val controller = createController(
            fakeServer = fakeServer,
            scope = this,
            config = NetworkBrowserConfig(autoStopOnBackground = true),
        )

        controller.start()
        assertTrue(fakeServer.isRunning)

        controller.onLifecyclePause()
        advanceUntilIdle() // onLifecyclePause is fire-and-forget: it launches stop() rather than awaiting it.

        assertFalse(fakeServer.isRunning)
        assertEquals(BrowserConnectionInfo.Stopped, controller.connectionInfo.value)
    }

    @Test
    fun simulatorUrlIsNullOnRealDevice() = runTest {
        val fakeServer = FakeBrowserServer()
        val controller = createController(
            fakeServer = fakeServer,
            scope = this,
            addressProvider = FakeAddressProvider(isSimulator = false),
        )

        controller.start()

        val running = assertIs<BrowserConnectionInfo.Running>(controller.connectionInfo.value)
        assertNull(running.simulatorUrl)

        controller.stop()
    }

    @Test
    fun staysRunningWhenAutoStopDisabled() = runTest {
        val fakeServer = FakeBrowserServer()
        val controller = createController(
            fakeServer = fakeServer,
            scope = this,
            config = NetworkBrowserConfig(autoStopOnBackground = false),
        )

        controller.start()
        assertTrue(fakeServer.isRunning)

        controller.onLifecyclePause()

        assertTrue(fakeServer.isRunning)
        assertIs<BrowserConnectionInfo.Running>(controller.connectionInfo.value)

        controller.stop()
    }

    private fun createController(
        repository: NetworkDebugRepository = createRepository(CoroutineScope(kotlin.coroutines.EmptyCoroutineContext)),
        fakeServer: FakeBrowserServer,
        scope: CoroutineScope,
        isEnabled: () -> Boolean = { true },
        config: NetworkBrowserConfig = NetworkBrowserConfig(),
        addressProvider: BrowserAddressProvider = FakeAddressProvider(),
    ): NetworkBrowserController = NetworkBrowserController(
        repository = repository,
        config = config,
        addressProvider = addressProvider,
        scope = scope,
        isEnabled = isEnabled,
        serverFactory = { fakeServer },
    )

    private fun createRepository(scope: CoroutineScope): NetworkDebugRepository {
        val callDao = InMemoryNetworkCallDao()
        return NetworkDebugRepository(
            dao = callDao,
            config = ProbeCaptureLimits(),
            scope = scope,
            sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), callDao),
        )
    }

    private fun sampleCall(
        id: String,
        path: String,
    ) = NetworkCall(
        id = id,
        timestampMillis = Clock.System.now().toEpochMilliseconds(),
        method = "GET",
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

    private class FakeAddressProvider(
        isEmulator: Boolean = true,
        isSimulator: Boolean = true,
    ) : BrowserAddressProvider {
        private val emulator = isEmulator
        private val simulator = isSimulator

        override fun wifiIpAddress(): String = "192.168.1.10"

        override fun isEmulator(): Boolean = emulator

        override fun isSimulator(): Boolean = simulator

        override fun deviceName(): String = "Fake Device"
    }

    private class FakeBrowserServer : BrowserServer {
        val broadcastedCalls = mutableListOf<NetworkCallDto>()
        var isRunning = false
            private set
        private var startCount = 0

        override fun start(
            onStarted: (BrowserSession) -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            startCount += 1
            isRunning = true
            onStarted(
                BrowserSession(
                    token = "token-$startCount",
                    createdAtMillis = startCount.toLong(),
                    expiresAtMillis = Long.MAX_VALUE,
                ),
            )
        }

        override suspend fun stop() {
            isRunning = false
        }

        override fun broadcastCallUpdated(dto: NetworkCallDto) {
            broadcastedCalls += dto
        }
    }

    private class InMemoryNetworkCallDao : NetworkCallDao {
        private val calls = MutableStateFlow<List<NetworkCallEntity>>(emptyList())

        override suspend fun insert(call: NetworkCallEntity) {
            calls.value = calls.value
                .filterNot { it.id == call.id } + call
        }

        override suspend fun getById(id: String): NetworkCallEntity? =
            calls.value.firstOrNull { it.id == id }

        override fun observeSearch(sessionId: String, query: String, limit: Int): Flow<List<NetworkCallEntity>> =
            calls.map { currentCalls ->
                currentCalls
                    .filter { it.sessionId == sessionId }
                    .filter { call -> query.isBlank() || call.matches(query) }
                    .sortedByDescending { it.timestampMillis }
                    .take(limit)
            }

        override suspend fun clearAll() {
            calls.value = emptyList()
        }

        override suspend fun clearSession(sessionId: String) {
            calls.value = calls.value.filterNot { it.sessionId == sessionId }
        }

        override suspend fun deleteSessionsNotIn(sessionIds: List<String>) {
            calls.value = calls.value.filter { it.sessionId in sessionIds }
        }

        override suspend fun enforceCountCapForSession(sessionId: String, maxEntries: Int) {
            val keepIds = calls.value
                .filter { it.sessionId == sessionId }
                .sortedByDescending { it.timestampMillis }
                .take(maxEntries)
                .map { it.id }
                .toSet()
            calls.value = calls.value.filterNot { it.sessionId == sessionId && it.id !in keepIds }
        }

        private fun NetworkCallEntity.matches(query: String): Boolean =
            url.contains(query) ||
                path.contains(query) ||
                method.contains(query) ||
                responseStatus?.toString()?.contains(query) == true
    }
}
