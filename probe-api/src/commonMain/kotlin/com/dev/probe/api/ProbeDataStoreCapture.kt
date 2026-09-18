package com.dev.probe.api

import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.Flow

/**
 * Host integration point for exposing an observable preference/settings snapshot to Probe's
 * DataStore inspector plugin. Call [register] once, at the same call site where you construct
 * the backing store — same precedent as [ProbeHttpCapture.setHook].
 */
object ProbeDataStoreCapture {
    @Volatile
    private var registrations: Map<String, RegisteredStore> = emptyMap()

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
        registrations = registrations + (name to RegisteredStore(snapshot, redactor))
    }

    fun unregister(name: String) {
        registrations = registrations - name
    }

    /**
     * Public (not `internal`) because the caller (`:probe-runtime`'s `DataStoreInspectorPluginUi`)
     * lives in a separate Gradle module from this object — Kotlin's `internal` visibility is
     * module-scoped, not package-scoped. Same precedent as [ProbeHttpCapture.setHook].
     */
    fun snapshot(): Map<String, RegisteredStore> = registrations
}

class RegisteredStore(val flow: Flow<Any?>, val redactor: (Any?) -> String)
