package com.dev.probe.api

/** Entry screen requested when opening the Probe platform shell. */
internal enum class ProbeStartScreen { Hub, Inspector }

/** Intent extra / launch arg key carrying the [ProbeStartScreen] name. */
internal const val PROBE_START_SCREEN = "probe.start_screen"

/**
 * Opens the Probe platform shell — a dedicated Activity on Android, a full-screen presented
 * view controller on iOS — hosting [com.dev.probe.ui.ProbeApp] at the requested screen.
 */
expect object ProbeLauncher {
    fun openHub(ctx: ProbePlatformContext)
    fun openInspector(ctx: ProbePlatformContext)
}
