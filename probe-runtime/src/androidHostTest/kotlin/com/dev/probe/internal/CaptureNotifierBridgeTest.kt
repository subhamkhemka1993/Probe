@file:OptIn(ExperimentalCoroutinesApi::class)

package com.dev.probe.internal

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.NetworkOutputMode
import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.api.ProbeNotifier
import com.dev.probe.api.ProbeNotifierState
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.browser.BrowserAddressProvider
import com.dev.probe.browser.BrowserServer
import com.dev.probe.browser.BrowserSession
import com.dev.probe.browser.NetworkBrowserConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.browser.NetworkCallDto
import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Uses fully in-memory fakes for the repository DAO, prefs [DataStore] and browser server so the
 * whole [combine][kotlinx.coroutines.flow.combine] pipeline is driven purely by the test's virtual
 * clock — no real disk/IO async gap for [advanceTimeBy] to race against.
 */
@OptIn(ExperimentalTime::class)
@RunWith(RobolectricTestRunner::class)
internal class CaptureNotifierBridgeTest {

    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }
    private val nowMillis = Clock.System.now().toEpochMilliseconds()

    @Test
    fun mapsCallListToNotifierStateOnFirstSampleTick() = runTest {
        val repository = createRepository(this)
        repository.insertPending(sampleCall(id = "call-1", timestampMillis = nowMillis, status = 200))

        val notifier = RecordingNotifier(platform)
        val bridge = CaptureNotifierBridge(
            repository = repository,
            preferencesStore = DebugPreferencesStore(InMemoryPreferencesDataStore()),
            browserController = createBrowserController(repository, this),
            notifier = notifier,
            scope = backgroundScope,
        )

        bridge.start()
        advanceTimeBy(SAMPLE_SETTLE_MS)

        assertEquals(1, notifier.lastState?.requestCount)
        assertEquals(200, notifier.lastState?.lastStatusCode)
        assertEquals(NetworkOutputMode.INSPECTOR, notifier.lastState?.outputMode)
        assertNull(notifier.lastState?.browserUrl)

        bridge.stop()
        assertTrue(notifier.hideCalled)
    }

    /**
     * [sample] ticks on a fixed schedule from when the pipeline starts (here: 3000ms, 6000ms,
     * ...), not a per-emission reset window — `call-1` and `call-2` both land inside the same
     * tick interval (the one ending at 6000ms), so only one notification, carrying both calls'
     * state, fires at that boundary.
     */
    @Test
    fun coalescesUpdatesWithinOneSampleTickIntoOneShow() = runTest {
        val repository = createRepository(this)
        val notifier = RecordingNotifier(platform)
        val bridge = CaptureNotifierBridge(
            repository = repository,
            preferencesStore = DebugPreferencesStore(InMemoryPreferencesDataStore()),
            browserController = createBrowserController(repository, this),
            notifier = notifier,
            scope = backgroundScope,
        )

        bridge.start()
        advanceTimeBy(SAMPLE_SETTLE_MS) // t=3100: crosses the first tick (empty state) — absorbed below.
        notifier.showCount = 0

        repository.insertPending(sampleCall(id = "call-1", timestampMillis = nowMillis, status = 200))
        advanceTimeBy(RAPID_UPDATE_GAP_MS) // t=3200
        repository.insertPending(sampleCall(id = "call-2", timestampMillis = nowMillis + 1, status = 404))

        advanceTimeBy(TICK_MARGIN_MS) // t=3300: still short of the next tick at t=6000.
        assertEquals(0, notifier.showCount, "both calls landed inside the same tick interval; must stay conflated")

        advanceTimeBy(SAMPLE_WINDOW_MS) // t=6300: past the second tick at t=6000.
        assertEquals(1, notifier.showCount)
        assertEquals(2, notifier.lastState?.requestCount)
        assertEquals(404, notifier.lastState?.lastStatusCode)
    }

    /**
     * A `debounce()`-based pipeline resets its timer on every emission and can go silent
     * indefinitely under traffic faster than the window — this proves the actual `sample()`
     * pipeline keeps surfacing periodic updates across several tick boundaries instead.
     */
    @Test
    fun sustainedTrafficStillProducesPeriodicUpdates() = runTest {
        val repository = createRepository(this)
        val notifier = RecordingNotifier(platform)
        val bridge = CaptureNotifierBridge(
            repository = repository,
            preferencesStore = DebugPreferencesStore(InMemoryPreferencesDataStore()),
            browserController = createBrowserController(repository, this),
            notifier = notifier,
            scope = backgroundScope,
        )

        bridge.start()
        advanceTimeBy(SAMPLE_SETTLE_MS) // crosses the first tick (empty state) — absorbed below.
        notifier.showCount = 0

        // Continuous traffic, spaced faster than the tick period, spanning several tick boundaries.
        repeat(SUSTAINED_TRAFFIC_UPDATE_COUNT) { index ->
            repository.insertPending(
                sampleCall(id = "call-$index", timestampMillis = nowMillis + index, status = 200),
            )
            advanceTimeBy(RAPID_UPDATE_GAP_MS)
        }
        advanceTimeBy(SAMPLE_WINDOW_MS) // let the tick straddling the loop's end also fire.

        assertTrue(
            notifier.showCount >= MIN_EXPECTED_UPDATES_ACROSS_WINDOWS,
            "sustained traffic across multiple ${SAMPLE_WINDOW_MS}ms windows must keep producing " +
                "updates, got only ${notifier.showCount}",
        )
    }

    private companion object {
        /** Mirrors [CaptureNotifierBridge]'s throttle window; kept local since that constant is private. */
        const val SAMPLE_WINDOW_MS = 3_000L
        const val SAMPLE_SETTLE_MS = SAMPLE_WINDOW_MS + 100
        const val RAPID_UPDATE_GAP_MS = 100L
        const val TICK_MARGIN_MS = 100L

        /** 100 * 100ms = 10s of continuous traffic — comfortably spans three 3000ms tick periods. */
        const val SUSTAINED_TRAFFIC_UPDATE_COUNT = 100
        const val MIN_EXPECTED_UPDATES_ACROSS_WINDOWS = 3
    }

    private fun createRepository(scope: CoroutineScope): NetworkDebugRepository {
        val callDao = InMemoryNetworkCallDao()
        return NetworkDebugRepository(
            dao = callDao,
            config = ProbeCaptureLimits(),
            scope = scope,
            sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), callDao),
        )
    }

    private fun createBrowserController(
        repository: NetworkDebugRepository,
        scope: CoroutineScope,
    ): NetworkBrowserController = NetworkBrowserController(
        repository = repository,
        config = NetworkBrowserConfig(),
        addressProvider = FakeAddressProvider(),
        scope = scope,
        isEnabled = { true },
        serverFactory = { FakeBrowserServer() },
    )

    private fun sampleCall(id: String, timestampMillis: Long, status: Int) = NetworkCall(
        id = id,
        timestampMillis = timestampMillis,
        method = "GET",
        url = "https://api.example.com/$id",
        host = "api.example.com",
        path = "/$id",
        query = null,
        requestHeaders = emptyMap(),
        requestBody = null,
        responseStatus = status,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10L,
        error = null,
        isComplete = true,
    )

    private class RecordingNotifier(platform: ProbePlatformContext) : ProbeNotifier(platform) {
        var lastState: ProbeNotifierState? = null
        var showCount = 0
        var hideCalled = false

        override fun show(state: ProbeNotifierState) {
            lastState = state
            showCount += 1
        }

        override fun hide() {
            hideCalled = true
        }
    }

    private class FakeAddressProvider : BrowserAddressProvider {
        override fun wifiIpAddress(): String = "192.168.1.10"

        override fun isEmulator(): Boolean = true

        override fun isSimulator(): Boolean = true

        override fun deviceName(): String = "Fake Device"
    }

    private class FakeBrowserServer : BrowserServer {
        override fun start(
            onStarted: (BrowserSession) -> Unit,
            onError: (Throwable) -> Unit,
        ) = Unit

        override suspend fun stop() = Unit

        override fun broadcastCallUpdated(dto: NetworkCallDto) = Unit
    }

    /** Synchronous in-memory [DataStore] so prefs reads never introduce a real async gap. */
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
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
