package com.dev.probe.api

import com.dev.probe.NetworkOutputMode

/** Snapshot driving the sticky/persistent [ProbeNotifier] surface. */
internal data class ProbeNotifierState(
    val requestCount: Int,
    val lastStatusCode: Int?,
    val outputMode: NetworkOutputMode,
    val browserUrl: String?,
)

/**
 * Sticky (Android) / persistent (iOS) notification that is the sole entry point into the Probe
 * platform shell while capture is active. Declared `open` so tests can substitute a recording
 * subclass without touching real notification APIs.
 */
internal expect open class ProbeNotifier(
    platformContext: ProbePlatformContext,
) {
    open fun show(state: ProbeNotifierState)

    open fun hide()
}

/** Platform factory creating the [ProbeNotifier] for the current [ProbePlatformContext]. */
internal expect fun createProbeNotifier(platform: ProbePlatformContext): ProbeNotifier
