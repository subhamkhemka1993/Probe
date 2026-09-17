package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.db.NetworkCallDao
import com.dev.probe.db.toDomain
import com.dev.probe.db.toEntity
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.SessionRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class NetworkDebugRepository(
    private val dao: NetworkCallDao,
    private val config: ProbeCaptureLimits,
    private val scope: CoroutineScope,
    private val sessionManager: DebugSessionManager,
) {
    fun observeCalls(sessionId: String, searchQuery: String): Flow<List<NetworkCall>> =
        dao.observeSearch(sessionId, searchQuery, config.maxEntries).map { entities ->
            entities.map { it.toDomain() }
        }

    /** Session-agnostic view for callers (UI, browser bridge) that always want the current session. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveCalls(searchQuery: String): Flow<List<NetworkCall>> =
        sessionManager.activeSession().flatMapLatest { session -> observeCalls(session.id, searchQuery) }

    suspend fun insertPending(call: NetworkCall) {
        val sessionId = sessionManager.activeSession().value.id
        dao.insert(call.toEntity(sessionId))
        scope.launch { dao.enforceCountCapForSession(sessionId, config.maxEntries) }
    }

    suspend fun update(id: String, transform: (NetworkCall) -> NetworkCall) {
        val entity = dao.getById(id) ?: return
        dao.insert(transform(entity.toDomain()).toEntity(entity.sessionId))
    }

    suspend fun getById(id: String): NetworkCall? = dao.getById(id)?.toDomain()

    /** Clears only [sessionId]'s captured calls — never every session (that's [DebugSessionManager.onClearData]'s job). */
    suspend fun clear(sessionId: String) = dao.clearSession(sessionId)

    /**
     * Resolves a `session` query value ("current" | "previous") to the backing session id, for
     * callers outside the session package (e.g. the browser API). Falls back to the active
     * session for missing/unknown values; returns `null` when "previous" is requested but no
     * previous session exists yet.
     */
    suspend fun resolveSessionId(sessionQuery: String?): String? = when (sessionQuery?.lowercase()) {
        SESSION_QUERY_PREVIOUS ->
            sessionManager
                .availableSessions()
                .first()
                .firstOrNull { it.role == SessionRole.PREVIOUS }
                ?.id
        else -> sessionManager.activeSession().value.id
    }

    private companion object {
        const val SESSION_QUERY_PREVIOUS = "previous"
    }
}
