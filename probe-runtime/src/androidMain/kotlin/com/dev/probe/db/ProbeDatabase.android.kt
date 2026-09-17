package com.dev.probe.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

internal fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<ProbeDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(Z_DEBUG_DB_NAME)

    return Room.databaseBuilder<ProbeDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
