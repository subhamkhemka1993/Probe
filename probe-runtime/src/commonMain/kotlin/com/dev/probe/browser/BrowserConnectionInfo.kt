package com.dev.probe.browser

internal sealed interface BrowserConnectionInfo {
    data object Stopped : BrowserConnectionInfo

    data class Running(
        val port: Int,
        val token: String,
        val wifiUrl: String,
        val emulatorUrl: String?,
        val simulatorUrl: String?,
    ) : BrowserConnectionInfo

    data class Error(
        val message: String,
    ) : BrowserConnectionInfo
}
