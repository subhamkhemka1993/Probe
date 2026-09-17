package com.dev.probe.session

internal data class DebugSession(
    val id: String,
    val label: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val role: SessionRole,
)

internal enum class SessionRole { CURRENT, PREVIOUS }

internal const val SESSION_ROLE_CURRENT = "current"
internal const val SESSION_ROLE_PREVIOUS = "previous"

internal fun DebugSessionEntity.toDomain(): DebugSession =
    DebugSession(
        id = id,
        label = label,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        role = if (role == SESSION_ROLE_CURRENT) SessionRole.CURRENT else SessionRole.PREVIOUS,
    )

internal fun DebugSession.toEntity(): DebugSessionEntity =
    DebugSessionEntity(
        id = id,
        label = label,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        role = if (role == SessionRole.CURRENT) SESSION_ROLE_CURRENT else SESSION_ROLE_PREVIOUS,
    )
