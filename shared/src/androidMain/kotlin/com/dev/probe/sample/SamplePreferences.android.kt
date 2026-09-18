package com.dev.probe.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dev.probe.api.ProbePlatformContext
import okio.Path.Companion.toPath

actual fun createSampleDataStore(platform: ProbePlatformContext): DataStore<Preferences> {
    val appContext = platform.context
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            appContext.filesDir
                .resolve("datastore/$SAMPLE_PREFERENCES_FILE_NAME")
                .absolutePath
                .toPath()
        },
    )
}
