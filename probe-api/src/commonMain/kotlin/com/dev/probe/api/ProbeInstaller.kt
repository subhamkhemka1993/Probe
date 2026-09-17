package com.dev.probe.api

import kotlin.concurrent.Volatile

/**
 * Host integration point that triggers the debug tool's one-time initialization. Split from
 * [ProbeHub]/[ProbeHttpCapture]/[ProbeState] deliberately: those are called from arbitrary
 * app code that must behave safely with zero setup, so they default to a no-op [Hook][Any].
 * This one has no meaningful no-op behavior to fall back to — before [register] is called
 * (which only happens when `:probe-runtime` is actually on the classpath), [install] is a silent
 * no-op by construction (`hook` is `null`), not because a [Hook] implementation says so.
 */
object ProbeInstaller {
    interface Hook {
        fun install(
            config: ProbeConfig,
            platform: ProbePlatformContext,
        )
    }

    @Volatile
    private var hook: Hook? = null

    fun register(hook: Hook) {
        this.hook = hook
    }

    /** Test-only reset — production code never un-registers. */
    fun clearHook() {
        hook = null
    }

    fun install(
        config: ProbeConfig,
        platform: ProbePlatformContext,
    ) {
        hook?.install(config, platform)
    }
}
