package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbeLogSink
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.PROBE_SELF_DATABASE_NAME
import com.dev.probe.internal.ProbeGraphFactory
import com.dev.probe.internal.ProbePlatformHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProbeGraphFactoryTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeHttpCapture.setHook(NoOpHttpClientDebugHook)
        ProbePlatformHolder.clear()
        ProbeDatabaseCapture.unregister(PROBE_SELF_DATABASE_NAME)
        ProbeLogSink.clearWriter()
    }

    @Test
    fun create_whenDisabled_returnsServices() {
        val services =
            ProbeGraphFactory.create(
                config = ProbeConfig(isEnabled = { false }),
                platform = platform,
                scope = scope,
            )
        assertNotNull(services.networkDebugRepository)
        assertFalse(services.outputController.restoreScheduled)
        assertEquals(4, services.plugins.size)
    }

    @Test
    fun create_whenEnabled_returnsServices() {
        val services =
            ProbeGraphFactory.create(
                config = ProbeConfig(isEnabled = { true }),
                platform = platform,
                scope = scope,
            )
        assertNotNull(services.networkDebugRepository)
        assertNotNull(services.outputController)
        assertNotNull(services.preferencesStore)
        assertNotNull(services.browserController)
        assertNotNull(services.sessionManager)
        assertEquals(4, services.plugins.size)
        assertEquals(listOf("network", "datastore", "database", "logs"), services.plugins.map { it.id })
    }

    @Test
    fun create_whenEnabled_registersDatabaseAndInstallsLogWriter() {
        var sentinelInvoked = false
        ProbeLogSink.setWriter { _, _, _, _ -> sentinelInvoked = true }

        ProbeGraphFactory.create(
            config = ProbeConfig(isEnabled = { true }),
            platform = platform,
            scope = scope,
        )

        assertTrue(ProbeDatabaseCapture.snapshot().containsKey(PROBE_SELF_DATABASE_NAME))

        // create() must have overwritten the pre-existing sentinel writer with its own real
        // one, so this call no longer reaches the sentinel.
        ProbeLogSink.write("INFO", "tag", "hello", null)
        assertFalse(sentinelInvoked)
    }

    @Test
    fun create_whenDisabled_doesNotRegisterDatabaseOrInstallLogWriter() {
        var sentinelInvoked = false
        ProbeLogSink.setWriter { _, _, _, _ -> sentinelInvoked = true }

        ProbeGraphFactory.create(
            config = ProbeConfig(isEnabled = { false }),
            platform = platform,
            scope = scope,
        )

        assertFalse(ProbeDatabaseCapture.snapshot().containsKey(PROBE_SELF_DATABASE_NAME))

        // If create() had installed its own real writer despite isEnabled=false, this call would
        // route to that writer instead of the sentinel installed above, and sentinelInvoked would
        // stay false.
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
            )
        assertTrue(services.outputController.restoreScheduled)
    }
}
