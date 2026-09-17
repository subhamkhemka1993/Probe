package com.dev.probe.startup

import android.app.Application
import androidx.startup.AppInitializer
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbeInstaller
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import com.dev.probe.api.ProbeConfig
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ProbeStartupInitializerTest {

    @After
    fun tearDown() {
        ProbeRuntime.shutdown()
        ProbeInstaller.clearHook()
    }

    @Test
    fun create_registersAnInstallHookThatReachesProbeRuntime() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        AppInitializer.getInstance(context).initializeComponent(ProbeStartupInitializer::class.java)

        // The Initializer only registered a hook — nothing has called ProbeRuntime.initialize
        // yet. Calling install() here plays the role a host app's own install call plays in
        // production; observing ProbeRuntime.isEnabled() flip to true (driven by the config
        // we pass) is the only way to confirm the Initializer's hook actually reached
        // ProbeRuntime.initialize rather than being silently dropped.
        ProbeInstaller.install(ProbeConfig(isEnabled = { true }), ProbePlatformContext(context))

        assertTrue(ProbeRuntime.isEnabled())
    }
}
