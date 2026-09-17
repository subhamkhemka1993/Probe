package com.dev.probe.network

import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.NetworkCallEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Pure in-memory [NetworkCallDao] fake shared by tests that don't need a real Room database.
 * [throwOnNextInsert] lets a test simulate a single storage failure (e.g. a Room error) on the
 * next [insert] call; it self-resets to `false` immediately after throwing once.
 */
internal class InMemoryNetworkCallDao : NetworkCallDao {
    private val calls = MutableStateFlow<List<NetworkCallEntity>>(emptyList())

    var throwOnNextInsert: Boolean = false

    override suspend fun insert(call: NetworkCallEntity) {
        if (throwOnNextInsert) {
            throwOnNextInsert = false
            error("Simulated DAO insert failure")
        }
        calls.value = calls.value.filterNot { it.id == call.id } + call
    }

    override suspend fun getById(id: String): NetworkCallEntity? = calls.value.firstOrNull { it.id == id }

    override fun observeSearch(sessionId: String, query: String, limit: Int): Flow<List<NetworkCallEntity>> = calls.map { entities ->
        entities
            .asSequence()
            .filter { it.sessionId == sessionId }
            .filter { query.isEmpty() || it.matchesSearch(query) }
            .sortedByDescending { it.timestampMillis }
            .take(limit)
            .toList()
    }

    override suspend fun clearAll() {
        calls.value = emptyList()
    }

    override suspend fun clearSession(sessionId: String) {
        calls.value = calls.value.filterNot { it.sessionId == sessionId }
    }

    override suspend fun deleteSessionsNotIn(sessionIds: List<String>) {
        calls.value = calls.value.filter { it.sessionId in sessionIds }
    }

    override suspend fun enforceCountCapForSession(sessionId: String, maxEntries: Int) {
        val sessionCalls =
            calls.value
                .filter { it.sessionId == sessionId }
                .sortedByDescending { it.timestampMillis }
        if (sessionCalls.size <= maxEntries) return
        val keepIds = sessionCalls.take(maxEntries).map { it.id }.toSet()
        calls.value = calls.value.filterNot { it.sessionId == sessionId && it.id !in keepIds }
    }

    /** Mirrors the LIKE-based filters in the real DAO's `observeSearch` query. */
    private fun NetworkCallEntity.matchesSearch(query: String): Boolean {
        val normalized = query.lowercase()
        return url.lowercase().contains(normalized) ||
            path.lowercase().contains(normalized) ||
            method.lowercase().contains(normalized) ||
            responseStatus?.toString()?.contains(normalized) == true
    }
}
