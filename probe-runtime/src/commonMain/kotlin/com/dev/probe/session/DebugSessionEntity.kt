package com.dev.probe.session

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_sessions")
internal data class DebugSessionEntity(
    @PrimaryKey val id: String,
    val label: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val role: String, // "current" | "previous"
)
