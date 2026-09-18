package com.dev.probe.exceptions

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.db.getDatabaseBuilder
import com.dev.probe.db.getProbeDatabase
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.SessionRole
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExceptionInspectorPluginUiTest {
    @Test
    fun id_isExceptions() {
        val platform = ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
        val database = getProbeDatabase(getDatabaseBuilder(platform.context))
        val sessionManager = DebugSessionManager(database.debugSessionDao(), database.networkCallDao())
        val plugin = ExceptionInspectorPluginUi(CrashLogStore(platform, capacity = 50), sessionManager)

        assertEquals("exceptions", plugin.id)
    }

    private fun entry(id: Long, sessionId: String) = CrashEntry(
        id = id,
        sessionId = sessionId,
        timestampMillis = id,
        threadName = "main",
        isFatal = true,
        exceptionClassName = "RuntimeException",
        message = null,
        stackTrace = "",
    )

    @Test
    fun visibleCrashEntries_current_showsOnlyActiveSessionAndPending() {
        val entries = listOf(
            entry(1, "session-old"),
            entry(2, "session-current"),
            entry(3, DebugSessionManager.PENDING_SESSION_ID),
        )

        val visible =
            visibleCrashEntries(entries, SessionRole.CURRENT, activeSessionId = "session-current", previousSessionId = "session-old")

        assertEquals(setOf(2L, 3L), visible.map { it.id }.toSet())
    }

    @Test
    fun visibleCrashEntries_previous_showsOnlyPreviousSessionAndNeverPending() {
        val entries = listOf(
            entry(1, "session-old"),
            entry(2, "session-current"),
            entry(3, DebugSessionManager.PENDING_SESSION_ID),
        )

        val visible =
            visibleCrashEntries(entries, SessionRole.PREVIOUS, activeSessionId = "session-current", previousSessionId = "session-old")

        assertEquals(setOf(1L), visible.map { it.id }.toSet())
    }

    @Test
    fun visibleCrashEntries_previous_withNoPreviousSession_showsNothing() {
        val entries = listOf(entry(1, "session-current"))

        val visible = visibleCrashEntries(entries, SessionRole.PREVIOUS, activeSessionId = "session-current", previousSessionId = null)

        assertEquals(emptyList(), visible)
    }
}
