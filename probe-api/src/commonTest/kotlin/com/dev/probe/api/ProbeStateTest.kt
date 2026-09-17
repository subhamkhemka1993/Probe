package com.dev.probe.api

import kotlin.test.Test
import kotlin.test.assertEquals

class ProbeStateTest {
    @Test
    fun defaultCallbacksAreNoOps() {
        // No setCallbacks() call — must not throw.
        ProbeState.onAppBackgrounded()
        ProbeState.onAppForegrounded()
    }

    @Test
    fun setCallbacks_routesToInstalledImplementation() {
        var backgroundedCount = 0
        var foregroundedCount = 0
        ProbeState.setCallbacks(
            object : ProbeState.Callbacks {
                override fun onAppBackgrounded() {
                    backgroundedCount++
                }

                override fun onAppForegrounded() {
                    foregroundedCount++
                }
            },
        )

        ProbeState.onAppBackgrounded()
        ProbeState.onAppForegrounded()

        assertEquals(1, backgroundedCount)
        assertEquals(1, foregroundedCount)

        ProbeState.clearCallbacks()
    }

    @Test
    fun clearCallbacks_restoresNoOp() {
        ProbeState.setCallbacks(
            object : ProbeState.Callbacks {
                override fun onAppBackgrounded() = error("should not be called after clear")

                override fun onAppForegrounded() = error("should not be called after clear")
            },
        )
        ProbeState.clearCallbacks()

        // Must not throw.
        ProbeState.onAppBackgrounded()
        ProbeState.onAppForegrounded()
    }
}
