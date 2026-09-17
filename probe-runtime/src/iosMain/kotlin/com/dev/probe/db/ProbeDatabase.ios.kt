package com.dev.probe.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal fun getDatabaseBuilder(): RoomDatabase.Builder<ProbeDatabase> {
    val dbFilePath = applicationSupportDirectory() + "/$Z_DEBUG_DB_NAME"
    return Room.databaseBuilder<ProbeDatabase>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): String {
    val appSupportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )

    return requireNotNull(appSupportDirectory?.path)
}
