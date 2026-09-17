package com.dev.probe.network

import androidx.room.Room
import com.dev.probe.db.ProbeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.robolectric.RuntimeEnvironment

internal actual fun createTestProbeDatabase(): ProbeDatabase {
    val context = RuntimeEnvironment.getApplication()
    return Room
        .inMemoryDatabaseBuilder<ProbeDatabase>(
            context = context,
        ).setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
