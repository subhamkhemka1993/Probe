package com.dev.probe.browser

internal data class NetworkBrowserConfig(
    val port: Int = 8765,
    val bindPolicy: BindPolicy = BindPolicy.LAN,
    val sessionTtlHours: Int = 8,
    val autoStopOnBackground: Boolean = true,
)
