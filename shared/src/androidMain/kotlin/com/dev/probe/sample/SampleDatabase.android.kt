package com.dev.probe.sample

import androidx.room.Room
import androidx.room.RoomDatabase
import com.dev.probe.api.ProbePlatformContext

internal actual fun sampleDatabaseBuilder(platform: ProbePlatformContext): RoomDatabase.Builder<SampleDatabase> {
    val appContext = platform.context
    val dbFile = appContext.getDatabasePath(SAMPLE_DB_NAME)
    return Room.databaseBuilder<SampleDatabase>(context = appContext, name = dbFile.absolutePath)
}
