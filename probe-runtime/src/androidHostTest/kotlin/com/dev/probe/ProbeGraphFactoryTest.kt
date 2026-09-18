package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeCrashCapture
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbeLogSink
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.PROBE_SELF_DATABASE_NAME
import com.dev.probe.internal.ProbeGraphFactory
import com.dev.probe.internal.ProbePlatformHolder
import com.dev.probe.internal.ProbeServices
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProbeGraphFactoryTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }
    private var originalUncaughtHandler: Thread.UncaughtExceptionHandler? = null
    private var createdServices: ProbeServices? = null

    @Before
    fun setUp() {
        originalUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    /**
     * `create(isEnabled = true)` installs a real, process-wide uncaught-exception handler, points
     * the global [ProbeCrashCapture] reporter at this test's (about-to-be-torn-down)
     * sessionManager/crashLogStore, and launches an indefinitely-running session-prune collector
     * on `scope` — unlike [com.dev.probe.api.ProbeRuntime.shutdown], `create()` has no
     * corresponding teardown call, so this test must undo all three itself.
     * `uninstallCrashHook()` covers the handler and the collector Job together; the handler
     * restore below is a belt-and-braces fallback for tests that construct a [ProbeServices]
     * without capturing it (there are none currently, but nothing stops a future test from doing
     * so).
     */
    @After
    fun tearDown() {
        ProbeHttpCapture.setHook(NoOpHttpClientDebugHook)
        ProbePlatformHolder.clear()
        ProbeDatabaseCapture.unregister(PROBE_SELF_DATABASE_NAME)
        ProbeLogSink.clearWriter()
        createdServices?.uninstallCrashHook?.invoke()
        createdServices = null
        Thread.setDefaultUncaughtExceptionHandler(originalUncaughtHandler)
        ProbeCrashCapture.clearReporter()
    }

    @Test
    fun create_whenDisabled_returnsServices() {
        val services =
            ProbeGraphFactory.create(
                config = ProbeConfig(isEnabled = { false }),
                platform = platform,
                scope = scope,
            ).also { createdServices = it }
        assertNotNull(services.networkDebugRepository)
        assertFalse(services.outputController.restoreScheduled)
        assertEquals(5, services.plugins.size)
    }

    @Test
    fun create_whenEnabled_returnsServices() {
        val services =
            ProbeGraphFactory.create(
                config = ProbeConfig(isEnabled = { true }),
                platform = platform,
                scope = scope,
            ).also { createdServices = it }
        assertNotNull(services.networkDebugRepository)
        assertNotNull(services.outputController)
        assertNotNull(services.preferencesStore)
        assertNotNull(services.browserController)
        assertNotNull(services.sessionManager)
        assertEquals(5, services.plugins.size)
        assertEquals(listOf("network", "datastore", "database", "logs", "exceptions"), services.plugins.map { it.id })
    }

    /** [ProbeGraphFactory.create] must overwrite a pre-existing [ProbeLogSink] writer with its
     * own real one, so a write afterward no longer reaches the sentinel installed before it. */
    @Test
    fun create_whenEnabled_registersDatabaseAndInstallsLogWriter() {
        var sentinelInvoked = false
        ProbeLogSink.setWriter { _, _, _, _ -> sentinelInvoked = true }

        createdServices = ProbeGraphFactory.create(
            config = ProbeConfig(isEnabled = { true }),
            platform = platform,
            scope = scope,
        )

        assertTrue(ProbeDatabaseCapture.snapshot().containsKey(PROBE_SELF_DATABASE_NAME))

        ProbeLogSink.write("INFO", "tag", "hello", null)
        assertFalse(sentinelInvoked)
    }

    /** If [ProbeGraphFactory.create] installed its own real writer despite `isEnabled = false`,
     * a write afterward would route to that writer instead of the sentinel installed below, and
     * `sentinelInvoked` would stay false. */
    @Test
    fun create_whenDisabled_doesNotRegisterDatabaseOrInstallLogWriter() {
        var sentinelInvoked = false
        ProbeLogSink.setWriter { _, _, _, _ -> sentinelInvoked = true }

        createdServices = ProbeGraphFactory.create(
            config = ProbeConfig(isEnabled = { false }),
            platform = platform,
            scope = scope,
        )

        assertFalse(ProbeDatabaseCapture.snapshot().containsKey(PROBE_SELF_DATABASE_NAME))

        ProbeLogSink.write("INFO", "tag", "message", null)
        assertTrue(sentinelInvoked)
    }

    @Test
    fun create_whenEnabled_schedulesRestore() {
        val services =
            ProbeGraphFactory.create(
                config = ProbeConfig(isEnabled = { true }),
                platform = platform,
                scope = scope,
            ).also { createdServices = it }
        assertTrue(services.outputController.restoreScheduled)
    }
}
