package com.dev.probe.startup

import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Direct (non-Koin) entry point for platforms without an App-Startup-style auto-init hook
 * (iOS). The caller owns nothing about the returned scope's lifetime beyond holding a
 * reference if it ever wants to cancel it — in practice this lives for the process lifetime,
 * same as the Koin-owned scope it replaces.
 */
fun installProbeTools(config: ProbeConfig, platform: ProbePlatformContext): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    ProbeRuntime.initialize(config = config, platform = platform, scope = scope)
    return scope
}
