package com.dev.probe.devactions

import android.app.ActivityManager
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.api.ProbeNotifier
import com.dev.probe.api.ProbeNotifierState
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.browser.BrowserAddressProvider
import com.dev.probe.browser.BrowserConnectionInfo
import com.dev.probe.browser.BrowserServer
import com.dev.probe.browser.BrowserSession
import com.dev.probe.browser.NetworkBrowserConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.browser.NetworkCallDto
import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.internal.CaptureNotifierBridge
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import com.dev.probe.session.SessionRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [clearAppData] and [resetProbeRuntimeState] are tested independently here rather than through
 * [com.dev.probe.api.ProbeRuntime]/[com.dev.probe.internal.ProbeGraphFactory]: the
 * real graph's Room database uses `BundledSQLiteDriver`, which needs a native library this host
 * test environment doesn't have on `java.library.path`. In-memory fakes exercise the same
 * behaviour without that dependency.
 */
@RunWith(RobolectricTestRunner::class)
class ClearAppDataTest {

    private val application by lazy { ApplicationProvider.getApplicationContext<Application>() }
    private val platform by lazy { ProbePlatformContext(application) }

    @Test
    fun clearAppData_triggersFullResetViaActivityManager() = runTest {
        val result = clearAppData(platform)

        assertIs<ClearDataResult.FullResetTriggered>(result)
        val shadowActivityManager = Shadows.shadowOf(
            application.getSystemService(ActivityManager::class.java),
        )
        assertTrue(shadowActivityManager.isApplicationUserDataCleared())
    }

    @Test
    fun resetProbeRuntimeState_endsSessionStopsBrowserAndHidesNotifier() = runTest {
        val callDao = InMemoryNetworkCallDao()
        val sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), callDao)
        sessionManager.ensureInitialSession()

        val repository = NetworkDebugRepository(
            dao = callDao,
            config = ProbeCaptureLimits(),
            scope = backgroundScope,
            sessionManager = sessionManager,
        )
        val browserController = NetworkBrowserController(
            repository = repository,
            config = NetworkBrowserConfig(),
            addressProvider = FakeAddressProvider(),
            scope = backgroundScope,
            isEnabled = { true },
            serverFactory = { FakeBrowserServer() },
        )
        browserController.start()

        val notifier = RecordingNotifier(platform)
        val notifierBridge = CaptureNotifierBridge(
            repository = repository,
            preferencesStore = DebugPreferencesStore(InMemoryPreferencesDataStore()),
            browserController = browserController,
            notifier = notifier,
            scope = backgroundScope,
        )

        resetProbeRuntimeState(sessionManager, browserController, notifierBridge)

        assertEquals(1, sessionManager.availableSessions().first().size)
        assertEquals(SessionRole.CURRENT, sessionManager.activeSession().value.role)
        assertIs<BrowserConnectionInfo.Stopped>(browserController.connectionInfo.value)
        assertTrue(notifier.hideCalled)
    }

    private class RecordingNotifier(platform: ProbePlatformContext) : ProbeNotifier(platform) {
        var hideCalled = false

        override fun show(state: ProbeNotifierState) = Unit

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
        ) {
            onStarted(BrowserSession(token = "token-1", createdAtMillis = 0L, expiresAtMillis = Long.MAX_VALUE))
        }

        override suspend fun stop() = Unit

        override fun broadcastCallUpdated(dto: NetworkCallDto) = Unit
    }

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
            calls.value = calls.value.filterNot { it.id == call.id } + call
        }

        override suspend fun getById(id: String): NetworkCallEntity? =
            calls.value.firstOrNull { it.id == id }

        override fun observeSearch(sessionId: String, query: String, limit: Int): Flow<List<NetworkCallEntity>> =
            calls.map { currentCalls ->
                currentCalls
                    .filter { it.sessionId == sessionId }
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

        override suspend fun enforceCountCapForSession(sessionId: String, maxEntries: Int) = Unit
    }
}
