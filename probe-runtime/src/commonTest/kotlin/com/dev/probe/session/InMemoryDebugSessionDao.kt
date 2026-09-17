package com.dev.probe.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Pure in-memory [DebugSessionDao] fake shared by tests that don't need a real Room database.
 * [throwOnNextInsert] lets a test simulate a single storage failure (e.g. a Room error) on the
 * next [insert] call; it self-resets to `false` immediately after throwing once.
 */
internal class InMemoryDebugSessionDao : DebugSessionDao {
    private val sessions = MutableStateFlow<List<DebugSessionEntity>>(emptyList())

    var throwOnNextInsert: Boolean = false

    override suspend fun insert(session: DebugSessionEntity) {
        if (throwOnNextInsert) {
            throwOnNextInsert = false
            error("Simulated DAO insert failure")
        }
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun getByRole(role: String): DebugSessionEntity? = sessions.value.firstOrNull { it.role == role }

    override suspend fun getAll(): List<DebugSessionEntity> = sessions.value

    override fun observeAll(): Flow<List<DebugSessionEntity>> = sessions

    override suspend fun updateRole(
        id: String,
        role: String,
        endedAtMillis: Long?,
    ) {
        sessions.value =
            sessions.value.map {
                if (it.id == id) it.copy(role = role, endedAtMillis = endedAtMillis) else it
            }
    }

    override suspend fun deleteOrphans() {
        sessions.value =
            sessions.value.filter {
                it.role == SESSION_ROLE_CURRENT || it.role == SESSION_ROLE_PREVIOUS
            }
    }

    override suspend fun deleteSessionsNotIn(sessionIds: List<String>) {
        sessions.value = sessions.value.filter { it.id in sessionIds }
    }

    override suspend fun deleteAll() {
        sessions.value = emptyList()
    }
}
