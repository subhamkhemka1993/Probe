package com.dev.probe.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_calls",
    indices = [Index(value = ["sessionId"], name = "idx_network_calls_session")],
)
internal data class NetworkCallEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val query: String?,
    val requestHeadersJson: String,
    val requestBody: String?,
    val responseStatus: Int?,
    val responseHeadersJson: String?,
    val responseBody: String?,
    val durationMs: Long?,
    val error: String?,
    val isComplete: Boolean,
    val sessionId: String,
)
