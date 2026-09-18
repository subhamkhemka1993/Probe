package com.dev.probe.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dev.probe.api.ProbePlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createSampleDataStore(platform: ProbePlatformContext): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { sampleDataStoreFilePath().toPath() },
)

@OptIn(ExperimentalForeignApi::class)
private fun sampleDataStoreFilePath(): String {
    val appSupportDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return requireNotNull(appSupportDirectory?.path) + "/$SAMPLE_PREFERENCES_FILE_NAME"
}
