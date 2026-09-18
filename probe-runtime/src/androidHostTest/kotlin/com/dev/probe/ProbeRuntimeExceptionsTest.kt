package com.dev.probe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeCrashCapture
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProbeRuntimeExceptionsTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeRuntime.shutdown()
    }

    @Test
    fun shutdown_restoresOriginalUncaughtHandler() {
        val original = Thread.getDefaultUncaughtExceptionHandler()

        ProbeRuntime.initialize(ProbeConfig(isEnabled = { true }), platform, scope)
        ProbeRuntime.shutdown()

        assertEquals(original, Thread.getDefaultUncaughtExceptionHandler())
    }

    /** No reporter installed post-shutdown — must not throw and must not reach a stale
     * torn-down [com.dev.probe.exceptions.CrashLogStore]. */
    @Test
    fun shutdown_clearsCrashReporter() {
        ProbeRuntime.initialize(ProbeConfig(isEnabled = { true }), platform, scope)
        ProbeRuntime.shutdown()

        ProbeCrashCapture.reportCaught(RuntimeException("post-shutdown"))
    }
}
