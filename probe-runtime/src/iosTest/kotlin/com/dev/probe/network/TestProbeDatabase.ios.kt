package com.dev.probe.network

import androidx.room.Room
import com.dev.probe.db.ProbeDatabase
import com.dev.probe.db.getProbeDatabase

internal actual fun createTestProbeDatabase(): ProbeDatabase = getProbeDatabase(
    Room.inMemoryDatabaseBuilder<ProbeDatabase>(),
)
