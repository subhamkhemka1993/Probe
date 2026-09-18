package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHub
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import com.dev.probe.browser.BrowserConnectionInfo
import com.dev.probe.internal.ProbePlatformHolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProbeRuntimeTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }
    private val config = ProbeConfig(isEnabled = { true })

    /**
     * [scope] is a real (non-virtual-time) scope; [ProbeRuntime.shutdown] only clears
     * `ProbeRuntime`'s own state, not this scope's job — cancel it too so nothing it launched
     * (e.g. via `scheduleRestore()`) can keep running past this test method and throw
     * asynchronously during a later, unrelated test.
     */
    @After
    fun tearDown() {
        ProbeRuntime.shutdown()
        scope.cancel()
        ProbeHub.clearHook()
    }

    @Test
    fun services_throwsBeforeInitialize() {
        assertFailsWith<IllegalStateException> {
            ProbeRuntime.services()
        }
    }

    @Test
    fun initialize_providesServices() {
        ProbeRuntime.initialize(config, platform, scope)
        val services = ProbeRuntime.services()
        assertNotNull(services.networkDebugRepository)
        assertNotNull(services.outputController)
        assertNotNull(services.preferencesStore)
    }

    @Test
    fun doubleInitialize_isIdempotent() {
        ProbeRuntime.initialize(config, platform, scope)
        val first = ProbeRuntime.services()
        ProbeRuntime.initialize(config, platform, scope)
        val second = ProbeRuntime.services()
        assertSame(first, second)
    }

    @Test
    fun onAppBackgrounded_beforeInitialize_doesNotThrow() {
        ProbeRuntime.onAppBackgrounded()
    }

    @Test
    fun onAppBackgrounded_withoutRunningBrowser_isNoOp() {
        ProbeRuntime.initialize(config, platform, scope)

        ProbeRuntime.onAppBackgrounded()

        assertEquals(
            BrowserConnectionInfo.Stopped,
            ProbeRuntime
                .services()
                .browserController.connectionInfo.value,
        )
    }

    @Test
    fun onAppForegrounded_beforeInitialize_doesNotThrow() {
        ProbeRuntime.onAppForegrounded()
    }

    @Test
    fun onAppForegrounded_withoutRunningBrowser_isNoOp() {
        ProbeRuntime.initialize(config, platform, scope)

        ProbeRuntime.onAppForegrounded()

        assertEquals(
            BrowserConnectionInfo.Stopped,
            ProbeRuntime
                .services()
                .browserController.connectionInfo.value,
        )
    }

    @Test
    fun isEnabled_beforeInitialize_returnsFalse() {
        assertEquals(false, ProbeRuntime.isEnabled())
    }

    @Test
    fun isEnabled_afterInitialize_reflectsConfig() {
        ProbeRuntime.initialize(ProbeConfig(isEnabled = { true }), platform, scope)
        assertEquals(true, ProbeRuntime.isEnabled())
    }

    @Test
    fun isEnabled_afterInitialize_withDisabledConfig_returnsFalse() {
        ProbeRuntime.initialize(ProbeConfig(isEnabled = { false }), platform, scope)
        assertEquals(false, ProbeRuntime.isEnabled())
    }

    @Test
    fun shutdown_clearsState() {
        ProbeRuntime.initialize(config, platform, scope)
        ProbeRuntime.shutdown()
        assertFailsWith<IllegalStateException> {
            ProbeRuntime.services()
        }
        assertFailsWith<IllegalStateException> {
            ProbePlatformHolder.requirePlatform()
        }
    }

    /** [ProbeHub.openHub] delegates to `ProbeLauncher.openHub` — verifying it doesn't throw is
     * the practical check available at this layer. */
    @Test
    fun initialize_installsProbeHubHook() {
        ProbeRuntime.initialize(config, platform, scope)

        assertTrue(ProbeHub.isEnabled())
        ProbeHub.openHub(platform)
    }

    @Test
    fun shutdown_clearsProbeHubHook() {
        ProbeRuntime.initialize(config, platform, scope)
        ProbeRuntime.shutdown()

        assertFalse(ProbeHub.isEnabled())
    }
}
