package com.dev.probe.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal fun createDebugPreferencesDataStore(context: Context): DataStore<Preferences> {
    val appContext = context.applicationContext
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            appContext.filesDir
                .resolve("datastore/$DEBUG_PREFERENCES_FILE_NAME")
                .absolutePath
                .toPath()
        },
    )
}
