package com.dev.probe.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dev.probe.NetworkOutputMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal const val DEBUG_PREFERENCES_FILE_NAME = "debug_preferences.preferences_pb"

private val NetworkOutputModeKey = stringPreferencesKey("network_output_mode")

internal class DebugPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<DebugPreferences> =
        dataStore.data.map { prefs ->
            DebugPreferences(
                networkOutputMode =
                    prefs[NetworkOutputModeKey]
                        ?.let(::parseNetworkOutputMode)
                        ?: NetworkOutputMode.INSPECTOR,
            )
        }

    suspend fun setNetworkOutputMode(mode: NetworkOutputMode) {
        dataStore.edit { prefs ->
            prefs[NetworkOutputModeKey] = mode.name
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.clear() }
    }
}

private fun parseNetworkOutputMode(stored: String): NetworkOutputMode? =
    when (stored) {
        "ZTOOL", "CONSOLE" -> NetworkOutputMode.INSPECTOR
        else -> runCatching { NetworkOutputMode.valueOf(stored) }.getOrNull()
    }
