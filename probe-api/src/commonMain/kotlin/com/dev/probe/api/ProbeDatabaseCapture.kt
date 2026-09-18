package com.dev.probe.api

import androidx.room.RoomDatabase
import kotlin.concurrent.Volatile

/**
 * Host integration point for exposing a Room database to Probe's Database inspector plugin.
 * Call [register] once, at the same call site where you build the database — same precedent as
 * [ProbeHttpCapture.setHook]. Registering costs nothing beyond holding a reference: the inspector
 * reads the database generically (table/column/row discovery at runtime), so no schema
 * declaration or DAO of any kind is required on the registered database's own class.
 */
object ProbeDatabaseCapture {
    @Volatile
    private var registrations: Map<String, RoomDatabase> = emptyMap()

    fun register(name: String, database: RoomDatabase) {
        registrations = registrations + (name to database)
    }

    fun unregister(name: String) {
        registrations = registrations - name
    }

    /**
     * Public (not `internal`) because the caller (`:probe-runtime`'s `DatabaseInspectorPluginUi`)
     * lives in a separate Gradle module from this object — Kotlin's `internal` visibility is
     * module-scoped, not package-scoped. Same precedent as [ProbeHttpCapture.setHook].
     */
    fun snapshot(): Map<String, RoomDatabase> = registrations
}
