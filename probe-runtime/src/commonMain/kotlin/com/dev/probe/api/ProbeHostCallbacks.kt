package com.dev.probe.api

/**
 * Optional host-provided hook for clearing app-specific storage that Probe does not own — e.g.
 * auth tokens, feature-flag caches, or other DataStore/Keychain-backed state. Host apps set this
 * once during bootstrap.
 *
 * iOS's "clear app data" dev action invokes this because iOS has no OS-level equivalent to
 * Android's `ActivityManager.clearApplicationUserData()`; the returned labels describe what was
 * cleared and are shown to the user alongside Probe's own scoped clear result.
 */
object ProbeHostCallbacks {
    var onClearScopedData: (suspend () -> List<String>)? = null
}
