package com.dev.probe.api

/**
 * Marker contract for a pluggable debug inspector (network, prefs, DataStore, DB, ...). Kept
 * separate from the Compose-based `ProbePlugin` (which extends this in `:probe-runtime`) so a future
 * headless inspector could implement just this without pulling Compose into `:probe-api`.
 */
interface ProbeInspector {
    val id: String
    val label: String
}
