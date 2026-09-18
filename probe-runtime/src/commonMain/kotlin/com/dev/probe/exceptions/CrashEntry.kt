package com.dev.probe.exceptions

internal data class CrashEntry(
    val id: Long,
    val sessionId: String,
    val timestampMillis: Long,
    val threadName: String,
    val isFatal: Boolean,
    val exceptionClassName: String,
    val message: String?,
    val stackTrace: String,
)
