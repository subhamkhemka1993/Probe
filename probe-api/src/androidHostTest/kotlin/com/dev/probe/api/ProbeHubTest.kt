package com.dev.probe.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProbeHubTest {
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        ProbeHub.clearHook()
    }

    @Test
    fun isEnabled_defaultsToFalse() {
        assertFalse(ProbeHub.isEnabled())
    }

    @Test
    fun openHub_defaultsToNoOp() {
        // Must not throw with no hook installed.
        ProbeHub.openHub(platform)
    }

    @Test
    fun setHook_delegatesToInstalledHook() {
        var openedWith: ProbePlatformContext? = null
        ProbeHub.setHook(
            object : ProbeHub.Hook {
                override fun isEnabled() = true

                override fun openHub(context: ProbePlatformContext) {
                    openedWith = context
                }
            },
        )

        assertEquals(true, ProbeHub.isEnabled())
        ProbeHub.openHub(platform)
        assertEquals(platform, openedWith)
    }

    @Test
    fun clearHook_restoresNoOpDefault() {
        ProbeHub.setHook(
            object : ProbeHub.Hook {
                override fun isEnabled() = true

                override fun openHub(context: ProbePlatformContext) = Unit
            },
        )
        ProbeHub.clearHook()

        assertFalse(ProbeHub.isEnabled())
    }
}
