@file:OptIn(ExperimentalCoroutinesApi::class)

package com.dev.probe.policy

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.NetworkOutputMode
import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.browser.BrowserAddressProvider
import com.dev.probe.browser.BrowserServer
import com.dev.probe.browser.BrowserSession
import com.dev.probe.browser.NetworkBrowserConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.browser.NetworkCallDto
import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class NetworkOutputControllerTest {
    @Test
    fun applyModePersistsModeAndTogglesBrowserController() =
        runTest {
            val prefs = createPreferencesStore()
            val fakeServer = FakeBrowserServer()
            val outputController =
                createOutputController(
                    prefs = prefs,
                    fakeServer = fakeServer,
                    scope = this,
                )

            outputController.applyMode(NetworkOutputMode.BROWSER)

            assertTrue(fakeServer.isRunning)
            assertEquals(NetworkOutputMode.BROWSER, prefs.preferences.first().networkOutputMode)

            outputController.applyMode(NetworkOutputMode.INSPECTOR)

            assertFalse(fakeServer.isRunning)
            assertEquals(NetworkOutputMode.INSPECTOR, prefs.preferences.first().networkOutputMode)
        }

    @Test
    fun restorePersistedModeStartsBrowserWhenBrowserModeWasSaved() =
        runTest {
            val prefs = createPreferencesStore()
            val fakeServer = FakeBrowserServer()
            val outputController =
                createOutputController(
                    prefs = prefs,
                    fakeServer = fakeServer,
                    scope = this,
                )
            prefs.setNetworkOutputMode(NetworkOutputMode.BROWSER)

            try {
                outputController.restorePersistedMode()

                assertTrue(fakeServer.isRunning)
            } finally {
                outputController.applyMode(NetworkOutputMode.INSPECTOR)
            }
        }

    @Test
    fun applyModeCancelsPendingRestoreBeforeApplyingNewMode() =
        runTest {
            val prefs = createPreferencesStore()
            val fakeServer = FakeBrowserServer()
            val outputController =
                createOutputController(
                    prefs = prefs,
                    fakeServer = fakeServer,
                    scope = this,
                )
            prefs.setNetworkOutputMode(NetworkOutputMode.BROWSER)

            outputController.scheduleRestore()
            outputController.applyMode(NetworkOutputMode.INSPECTOR)
            advanceUntilIdle()

            assertFalse(fakeServer.isRunning)
            assertEquals(NetworkOutputMode.INSPECTOR, prefs.preferences.first().networkOutputMode)
        }

    @Test
    fun scheduleRestoreMarksRestoreScheduled() =
        runTest {
            val prefs = createPreferencesStore()
            val fakeServer = FakeBrowserServer()
            val outputController =
                createOutputController(
                    prefs = prefs,
                    fakeServer = fakeServer,
                    scope = this,
                )

            assertFalse(outputController.restoreScheduled)
            outputController.scheduleRestore()

            assertTrue(outputController.restoreScheduled)
        }

    private fun createOutputController(
        prefs: DebugPreferencesStore,
        fakeServer: FakeBrowserServer,
        scope: CoroutineScope,
    ): NetworkOutputController {
        val callDao = InMemoryNetworkCallDao()
        val repository =
            NetworkDebugRepository(
                dao = callDao,
                config = ProbeCaptureLimits(),
                scope = scope,
                sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), callDao),
            )
        val browserController =
            NetworkBrowserController(
                repository = repository,
                config = NetworkBrowserConfig(),
                addressProvider = FakeAddressProvider(),
                scope = scope,
                isEnabled = { true },
                serverFactory = { fakeServer },
            )
        return NetworkOutputController(
            prefs = prefs,
            browserController = browserController,
            scope = scope,
        )
    }

    private fun createPreferencesStore(): DebugPreferencesStore {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.filesDir.resolve("datastore/${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        val dataStore =
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { file.absolutePath.toPath() },
            )
        return DebugPreferencesStore(dataStore)
    }

    private class FakeAddressProvider : BrowserAddressProvider {
        override fun wifiIpAddress(): String = "192.168.1.10"

        override fun isEmulator(): Boolean = true

        override fun isSimulator(): Boolean = true

        override fun deviceName(): String = "Fake Device"
    }

    private class FakeBrowserServer : BrowserServer {
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

        override fun broadcastCallUpdated(dto: NetworkCallDto) = Unit
    }

    private class InMemoryNetworkCallDao : NetworkCallDao {
        private val calls = MutableStateFlow<List<NetworkCallEntity>>(emptyList())

        override suspend fun insert(call: NetworkCallEntity) {
            calls.value = calls.value
                .filterNot { it.id == call.id } + call
        }

        override suspend fun getById(id: String): NetworkCallEntity? = calls.value.firstOrNull { it.id == id }

        override fun observeSearch(
            sessionId: String,
            query: String,
            limit: Int,
        ): Flow<List<NetworkCallEntity>> =
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

        override suspend fun enforceCountCapForSession(
            sessionId: String,
            maxEntries: Int,
        ) {
            val keepIds =
                calls.value
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
