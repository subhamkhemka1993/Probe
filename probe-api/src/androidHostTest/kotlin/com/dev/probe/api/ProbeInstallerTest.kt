package com.dev.probe.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ProbeInstallerTest {

    private val testPlatformContext by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeInstaller.clearHook()
    }

    @Test
    fun install_withoutRegisteredHook_isSilentNoOp() {
        // No assertion beyond "does not throw" — there's nothing else observable.
        ProbeInstaller.install(ProbeConfig(isEnabled = { true }), testPlatformContext)
    }

    @Test
    fun install_afterRegister_invokesTheRegisteredHook() {
        var installedWith: ProbeConfig? = null
        ProbeInstaller.register(
            object : ProbeInstaller.Hook {
                override fun install(config: ProbeConfig, platform: ProbePlatformContext) {
                    installedWith = config
                }
            },
        )
        val config = ProbeConfig(isEnabled = { true })

        ProbeInstaller.install(config, testPlatformContext)

        assertEquals(config, installedWith)
    }
}
