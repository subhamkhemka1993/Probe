package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.ProbeGraphFactory
import com.dev.probe.internal.ProbePlatformHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertEquals(1, services.plugins.size)
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
        assertEquals(1, services.plugins.size)
        assertEquals("network", services.plugins.first().id)
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
