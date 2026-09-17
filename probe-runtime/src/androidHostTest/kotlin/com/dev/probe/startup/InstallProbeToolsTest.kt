package com.dev.probe.startup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InstallProbeToolsTest {
    private val platformContext by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeRuntime.shutdown()
    }

    @Test
    fun installProbeTools_initializesProbeRuntime() {
        installProbeTools(ProbeConfig(isEnabled = { true }), platformContext)

        assertTrue(ProbeRuntime.isEnabled())
    }
}
