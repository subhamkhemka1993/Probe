package com.dev.probe.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dev.probe.api.ProbePlatformContext

internal const val SAMPLE_PREFERENCES_FILE_NAME = "probe_sample.preferences_pb"
internal val SAMPLE_MESSAGE_KEY = stringPreferencesKey("last_message")

expect fun createSampleDataStore(platform: ProbePlatformContext): DataStore<Preferences>

/** Writes a demo value so the DataStore inspector has something to show once registered. */
suspend fun DataStore<Preferences>.writeSampleValue(message: String) {
    edit { it[SAMPLE_MESSAGE_KEY] = message }
}
