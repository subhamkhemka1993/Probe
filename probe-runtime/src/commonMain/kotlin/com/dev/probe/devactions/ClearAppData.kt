package com.dev.probe.devactions

import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.internal.CaptureNotifierBridge
import com.dev.probe.session.DebugSessionManager

/** Outcome of a [clearAppData] invocation, surfaced by the "App data" dev action UI. */
internal sealed class ClearDataResult {
    /** Android: `ActivityManager.clearApplicationUserData()` succeeded — the process dies shortly after. */
    data object FullResetTriggered : ClearDataResult()

    /** iOS: no OS-level full reset exists; lists what was actually cleared. */
    data class PartialClear(val clearedItems: List<String>) : ClearDataResult()

    data class Failed(val reason: String) : ClearDataResult()
}

/**
 * Clears app data for the current platform. Android triggers a full process reset via
 * `ActivityManager.clearApplicationUserData()`. iOS has no OS-level equivalent, so it performs a
 * scoped clear of Probe's own storage plus anything the host registers via
 * [com.dev.probe.api.ProbeHostCallbacks.onClearScopedData].
 *
 * Callers run [resetProbeRuntimeState] first so the active session, browser server, and sticky
 * notifier are always left consistent, regardless of whether the platform can also reset host
 * storage.
 */
internal expect suspend fun clearAppData(ctx: ProbePlatformContext): ClearDataResult

/** Confirmation copy for the "App data" dev action — iOS can only offer a scoped clear. */
internal expect val clearAppDataDisclaimer: String

/** Ends the active session, stops the browser server, and hides the sticky notifier. */
internal suspend fun resetProbeRuntimeState(
    sessionManager: DebugSessionManager,
    browserController: NetworkBrowserController,
    notifierBridge: CaptureNotifierBridge?,
) {
    sessionManager.onClearData()
    browserController.stop()
    notifierBridge?.stop()
}
