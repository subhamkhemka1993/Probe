package com.dev.probe.ui

import com.dev.probe.browser.BrowserConnectionInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserConnectionCardTest {

    @Test
    fun copyLinkUrl_returnsWifiUrl_onEmulatorFixture() {
        val connectionInfo = runningFixture(
            wifiUrl = "http://10.0.2.16:8765/?token=sample-token",
            emulatorUrl = "http://10.0.2.2:8765/?token=sample-token",
        )

        assertEquals(connectionInfo.wifiUrl, connectionInfo.copyLinkUrl())
    }

    @Test
    fun copyLinkUrl_returnsWifiUrl_onPhysicalDeviceFixture() {
        val connectionInfo = runningFixture(
            wifiUrl = "http://192.168.1.12:8765/?token=sample-token",
            emulatorUrl = null,
        )

        assertEquals(connectionInfo.wifiUrl, connectionInfo.copyLinkUrl())
    }

    @Test
    fun emulatorDesktopUrl_buildsLoopbackUrlFromPortAndToken() {
        assertEquals(
            "http://127.0.0.1:8765/?token=sample-token",
            emulatorDesktopUrl(port = 8765, token = "sample-token"),
        )
    }

    @Test
    fun emulatorDesktopUrl_buildsLoopbackUrlForADifferentPortAndToken() {
        assertEquals(
            "http://127.0.0.1:9090/?token=another-token",
            emulatorDesktopUrl(port = 9090, token = "another-token"),
        )
    }

    @Test
    fun showsSimulatorRow_isFalse_whenNeitherEmulatorNorSimulator() {
        val connectionInfo = runningFixture(emulatorUrl = null, simulatorUrl = null)

        assertFalse(connectionInfo.showsSimulatorRow())
    }

    @Test
    fun showsSimulatorRow_isTrue_whenOnlySimulatorUrlIsPresent() {
        val connectionInfo = runningFixture(
            emulatorUrl = null,
            simulatorUrl = "http://127.0.0.1:8765/?token=sample-token",
        )

        assertTrue(connectionInfo.showsSimulatorRow())
    }

    @Test
    fun showsSimulatorRow_isFalse_whenOnlyEmulatorUrlIsPresent() {
        val connectionInfo = runningFixture(
            emulatorUrl = "http://10.0.2.2:8765/?token=sample-token",
            simulatorUrl = null,
        )

        assertFalse(connectionInfo.showsSimulatorRow())
    }

    @Test
    fun showsSimulatorRow_isFalse_whenBothEmulatorAndSimulatorUrlsArePresent() {
        val connectionInfo = runningFixture(
            emulatorUrl = "http://10.0.2.2:8765/?token=sample-token",
            simulatorUrl = "http://127.0.0.1:8765/?token=sample-token",
        )

        assertFalse(connectionInfo.showsSimulatorRow())
    }

    private fun runningFixture(
        wifiUrl: String = "http://192.168.1.12:8765/?token=sample-token",
        emulatorUrl: String?,
        simulatorUrl: String? = "http://127.0.0.1:8765/?token=sample-token",
    ) = BrowserConnectionInfo.Running(
        port = 8765,
        token = "sample-token",
        wifiUrl = wifiUrl,
        emulatorUrl = emulatorUrl,
        simulatorUrl = simulatorUrl,
    )
}
