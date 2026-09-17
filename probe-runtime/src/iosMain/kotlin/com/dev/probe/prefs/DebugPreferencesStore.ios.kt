package com.dev.probe.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal fun createDebugPreferencesDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { debugPreferencesFilePath().toPath() },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun debugPreferencesFilePath(): String {
    val appSupportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )

    return requireNotNull(appSupportDirectory?.path) + "/$DEBUG_PREFERENCES_FILE_NAME"
}
