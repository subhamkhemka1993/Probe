package com.dev.probe.browser

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.INET_ADDRSTRLEN
import platform.posix.sockaddr_in

@OptIn(ExperimentalForeignApi::class)
internal actual class LocalAddressProvider actual constructor() : BrowserAddressProvider {
    actual override fun wifiIpAddress(): String? = memScoped {
        val interfaces = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(interfaces.ptr) != 0) return@memScoped null

        try {
            var currentInterface = interfaces.value
            while (currentInterface != null) {
                val interfaceData = currentInterface.pointed
                val interfaceName = interfaceData.ifa_name?.toKString()
                val socketAddress = interfaceData.ifa_addr?.pointed

                if (interfaceName == WIFI_INTERFACE_NAME &&
                    socketAddress != null &&
                    socketAddress.sa_family.toInt() == AF_INET
                ) {
                    val internetAddress = interfaceData.ifa_addr!!
                        .reinterpret<sockaddr_in>()
                        .pointed
                    val address = internetAddress.ipv4String()
                    if (address != null) return@memScoped address
                }

                currentInterface = interfaceData.ifa_next
            }

            null
        } finally {
            freeifaddrs(interfaces.value)
        }
    }

    actual override fun isEmulator(): Boolean = false

    actual override fun isSimulator(): Boolean =
        NSProcessInfo.processInfo.environment[SIMULATOR_DEVICE_NAME] != null

    actual override fun deviceName(): String = UIDevice.currentDevice.name

    private companion object {
        const val WIFI_INTERFACE_NAME = "en0"
        const val SIMULATOR_DEVICE_NAME = "SIMULATOR_DEVICE_NAME"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sockaddr_in.ipv4String(): String? {
    val buffer = ByteArray(INET_ADDRSTRLEN)
    return buffer.usePinned { pinned ->
        val result = inet_ntop(
            AF_INET,
            sin_addr.ptr,
            pinned.addressOf(0),
            INET_ADDRSTRLEN.convert(),
        )
        if (result == null) null else pinned.get().toKString()
    }
}
