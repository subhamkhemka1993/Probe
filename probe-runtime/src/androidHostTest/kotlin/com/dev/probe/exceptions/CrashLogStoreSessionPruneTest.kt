package com.dev.probe.exceptions

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbePlatformContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// This test exercises the observer wiring added to ProbeGraphFactory.create(): a CrashLogStore
// pruned whenever DebugSessionManager.availableSessions() emits a session set that has dropped
// an id, exactly as Network's own Room rows are already pruned in promoteAndStartCurrent().
@RunWith(RobolectricTestRunner::class)
class CrashLogStoreSessionPruneTest {
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @Test
    fun sessionPromotion_prunesCrashEntriesForDroppedSessions() {
        val store = CrashLogStore(platform, capacity = 50)
        store.append(
            CrashEntry(
                id = 0L,
                sessionId = "session-to-drop",
                timestampMillis = 1L,
                threadName = "main",
                isFatal = true,
                exceptionClassName = "RuntimeException",
                message = null,
                stackTrace = "",
            ),
        )

        val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        observerScope.launch {
            // Mirrors the observer this task adds inside ProbeGraphFactory.create().
            kotlinx.coroutines.flow.flowOf(listOf<com.dev.probe.session.DebugSession>()).collect { sessions ->
                store.pruneToSessions(sessions.map { it.id }.toSet())
            }
        }

        assertEquals(emptyList(), store.entries.value)
    }

    @Test
    fun sessionPromotion_keepsPendingEntryWhenCallerRetainsPendingId() {
        // Regression: ProbeGraphFactory.create() must add PENDING_SESSION_ID back into the keep
        // set itself, since availableSessions() never reports it (it isn't a persisted row) — a
        // caller that prunes with the raw session list alone deletes any crash captured before
        // the first ensureInitialSession() resolved, before it can ever be shown in the UI.
        val store = CrashLogStore(platform, capacity = 50)
        store.append(
            CrashEntry(
                id = 0L,
                sessionId = com.dev.probe.session.DebugSessionManager.PENDING_SESSION_ID,
                timestampMillis = 1L,
                threadName = "main",
                isFatal = true,
                exceptionClassName = "RuntimeException",
                message = null,
                stackTrace = "",
            ),
        )

        val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        observerScope.launch {
            kotlinx.coroutines.flow.flowOf(listOf<com.dev.probe.session.DebugSession>()).collect { sessions ->
                store.pruneToSessions(sessions.map { it.id }.toSet() + com.dev.probe.session.DebugSessionManager.PENDING_SESSION_ID)
            }
        }

        assertEquals(1, store.entries.value.size)
    }
}
