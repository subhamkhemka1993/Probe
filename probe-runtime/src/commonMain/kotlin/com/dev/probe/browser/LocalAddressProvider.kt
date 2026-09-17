package com.dev.probe.browser

internal interface BrowserAddressProvider {
    fun wifiIpAddress(): String?
    fun isEmulator(): Boolean
    fun isSimulator(): Boolean
    fun deviceName(): String
}

internal expect class LocalAddressProvider() : BrowserAddressProvider {
    override fun wifiIpAddress(): String?
    override fun isEmulator(): Boolean
    override fun isSimulator(): Boolean
    override fun deviceName(): String
}
