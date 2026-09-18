package com.dev.probe.api

import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Host integration point for exposing a Room database to Probe's Database inspector plugin.
 * Call [register] once, at the same call site where you build the database — same precedent as
 * [ProbeHttpCapture.setHook]. Registering costs nothing beyond holding a reference: the inspector
 * reads the database generically (table/column/row discovery at runtime), so no schema
 * declaration or DAO of any kind is required on the registered database's own class.
 */
object ProbeDatabaseCapture {
    private val _registrations = MutableStateFlow<Map<String, RoomDatabase>>(emptyMap())

    /**
     * Public (not `internal`) because the caller (`:probe-runtime`'s `DatabaseInspectorPluginUi`)
     * lives in a separate Gradle module from this object — Kotlin's `internal` visibility is
     * module-scoped, not package-scoped. Same precedent as [ProbeHttpCapture.setHook]. Exposed as
     * a [StateFlow] (not just [snapshot]) so the inspector UI can react to a database registering
     * or unregistering after the panel is already open.
     */
    val registrations: StateFlow<Map<String, RoomDatabase>> = _registrations.asStateFlow()

    fun register(name: String, database: RoomDatabase) {
        _registrations.update { it + (name to database) }
    }

    fun unregister(name: String) {
        _registrations.update { it - name }
    }

    /** Point-in-time read of [registrations], for callers that don't need to observe changes. */
    fun snapshot(): Map<String, RoomDatabase> = _registrations.value
}
