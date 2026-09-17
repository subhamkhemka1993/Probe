package com.dev.probe.devactions

import com.dev.probe.api.ProbeHostCallbacks
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime

internal actual suspend fun clearAppData(ctx: ProbePlatformContext): ClearDataResult {
    val clearedItems = mutableListOf("Probe session data", "Debug preferences")
    ProbeRuntime.services().preferencesStore.clear()

    ProbeHostCallbacks.onClearScopedData?.invoke()?.let(clearedItems::addAll)

    return ClearDataResult.PartialClear(clearedItems)
}

internal actual val clearAppDataDisclaimer: String =
    "iOS has no full app reset. This clears Probe's captured network history and debug " +
        "preferences, plus anything the host app registers to clear. Other app storage " +
        "(Keychain, UserDefaults, files) is not affected."
