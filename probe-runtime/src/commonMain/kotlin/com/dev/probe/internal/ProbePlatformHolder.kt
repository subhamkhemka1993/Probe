package com.dev.probe.internal

import com.dev.probe.api.ProbePlatformContext

internal object ProbePlatformHolder {
    private var platform: ProbePlatformContext? = null

    fun init(platform: ProbePlatformContext) {
        this.platform = platform
    }

    fun requirePlatform(): ProbePlatformContext =
        checkNotNull(platform) { "ProbeRuntime.initialize() must be called first" }

    fun clear() {
        platform = null
    }
}
