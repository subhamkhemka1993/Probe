package com.dev.probe.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Host integration point for exposing an observable preference/settings snapshot to Probe's
 * DataStore inspector plugin. Call [register] once, at the same call site where you construct
 * the backing store — same precedent as [ProbeHttpCapture.setHook].
 */
object ProbeDataStoreCapture {
    private val _registrations = MutableStateFlow<Map<String, RegisteredStore>>(emptyMap())

    /**
     * Public (not `internal`) because the caller (`:probe-runtime`'s `DataStoreInspectorPluginUi`)
     * lives in a separate Gradle module from this object — Kotlin's `internal` visibility is
     * module-scoped, not package-scoped. Same precedent as [ProbeHttpCapture.setHook]. Exposed as
     * a [StateFlow] (not just [snapshot]) so the inspector UI can react to a resource registering
     * or unregistering after the panel is already open.
     */
    val registrations: StateFlow<Map<String, RegisteredStore>> = _registrations.asStateFlow()

    /**
     * Registers [snapshot] under [name] for live display in the DataStore inspector panel.
     *
     * **Security warning:** [redactor] defaults to plain [Any.toString], which dumps every field
     * of the registered value into the debug panel verbatim. If the registered type carries
     * sensitive data (auth tokens, PII, etc.), you MUST pass a [redactor] that masks those
     * fields — the default is unsafe for any such type. Probe has no way to know which fields
     * are sensitive; only the caller does.
     */
    fun register(name: String, snapshot: Flow<Any?>, redactor: (Any?) -> String = { it.toString() }) {
        _registrations.update { it + (name to RegisteredStore(snapshot, redactor)) }
    }

    fun unregister(name: String) {
        _registrations.update { it - name }
    }

    /** Point-in-time read of [registrations], for callers that don't need to observe changes. */
    fun snapshot(): Map<String, RegisteredStore> = _registrations.value
}

class RegisteredStore(val flow: Flow<Any?>, val redactor: (Any?) -> String)
