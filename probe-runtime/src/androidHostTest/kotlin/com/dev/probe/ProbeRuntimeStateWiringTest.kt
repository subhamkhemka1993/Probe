package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import com.dev.probe.api.ProbeState
import com.dev.probe.internal.ProbePlatformHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ProbeRuntimeStateWiringTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeRuntime.shutdown()
        ProbeHttpCapture.setHook(NoOpHttpClientDebugHook)
        ProbePlatformHolder.clear()
    }

    @Test
    fun shutdown_clearsProbeStateCallbacks() {
        ProbeRuntime.initialize(ProbeConfig(isEnabled = { true }), platform, scope)
        ProbeRuntime.shutdown()

        // Must be a silent no-op post-shutdown, not a stale forward into torn-down services.
        ProbeState.onAppBackgrounded()
        ProbeState.onAppForegrounded()
    }

    @Test
    fun initialize_installsCallbacksThatDelegateToProbeRuntime() {
        // isEnabled = false so ProbeGraphFactory.create does NOT eagerly call
        // outputController.scheduleRestore() itself — restoreScheduled starts false, isolating
        // the effect of the ProbeState-routed call below from create()'s own side effects.
        ProbeRuntime.initialize(ProbeConfig(isEnabled = { false }), platform, scope)
        val outputController = ProbeRuntime.services().outputController
        assertFalse(outputController.restoreScheduled)

        // Routed through ProbeState, not called directly on ProbeRuntime — proving
        // initialize() actually installed a real (non-default) Callbacks instance that forwards
        // to ProbeRuntime.onAppForegrounded(), rather than merely not throwing.
        ProbeState.onAppForegrounded()

        assertTrue(outputController.restoreScheduled)
    }
}
