package com.dev.probe.api

import kotlin.concurrent.Volatile

/**
 * Host integration point for launching the debug shell and checking whether it's active.
 * `:probe-runtime`'s `ProbeRuntime.initialize`/`shutdown` install/clear the real [Hook]; same
 * settable-hook pattern as [ProbeHttpCapture]/[ProbeState].
 */
object ProbeHub {
    interface Hook {
        fun isEnabled(): Boolean

        fun openHub(context: ProbePlatformContext)
    }

    private object NoOpHook : Hook {
        override fun isEnabled() = false

        override fun openHub(context: ProbePlatformContext) = Unit
    }

    @Volatile
    private var hook: Hook = NoOpHook

    fun setHook(hook: Hook) {
        this.hook = hook
    }

    fun clearHook() {
        hook = NoOpHook
    }

    fun isEnabled(): Boolean = hook.isEnabled()

    fun openHub(context: ProbePlatformContext) = hook.openHub(context)
}
