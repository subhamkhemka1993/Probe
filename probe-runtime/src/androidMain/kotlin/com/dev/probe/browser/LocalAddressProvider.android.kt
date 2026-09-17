package com.dev.probe.browser

import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

internal actual class LocalAddressProvider actual constructor() : BrowserAddressProvider {
    actual override fun wifiIpAddress(): String? {
        val candidates =
            runCatching {
                NetworkInterface
                    .getNetworkInterfaces()
                    .asSequence()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { networkInterface ->
                        networkInterface.inetAddresses.asSequence()
                    }.filterIsInstance<Inet4Address>()
                    .filterNot { it.isLoopbackAddress }
                    .toList()
            }.getOrDefault(emptyList())

        return candidates.firstOrNull { it.isSiteLocalAddress }?.hostAddress
            ?: candidates.firstOrNull()?.hostAddress
    }

    actual override fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()

        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            fingerprint.contains("vbox", ignoreCase = true) ||
            fingerprint.contains("test-keys", ignoreCase = true) ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "google_sdk" ||
            product.contains("sdk_gphone", ignoreCase = true) ||
            hardware.contains("ranchu", ignoreCase = true) ||
            hardware.contains("goldfish", ignoreCase = true)
    }

    actual override fun isSimulator(): Boolean = false

    actual override fun deviceName(): String = Build.MODEL
}
