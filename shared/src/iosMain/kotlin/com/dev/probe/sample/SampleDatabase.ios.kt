package com.dev.probe.sample

import androidx.room.Room
import androidx.room.RoomDatabase
import com.dev.probe.api.ProbePlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual fun sampleDatabaseBuilder(platform: ProbePlatformContext): RoomDatabase.Builder<SampleDatabase> {
    val dbFilePath = applicationSupportDirectory() + "/$SAMPLE_DB_NAME"
    return Room.databaseBuilder<SampleDatabase>(name = dbFilePath)
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): String {
    val appSupportDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return requireNotNull(appSupportDirectory?.path)
}
