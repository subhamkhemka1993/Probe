@file:OptIn(ExperimentalTime::class)

package com.dev.probe.session

import com.dev.probe.db.NetworkCallDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Owns the current/previous [DebugSession] lifecycle.
 *
 * A session lasts for one **OS process**: first [ensureInitialSession] in a process starts a
 * fresh current (promoting any persisted current from a prior process to previous). Background
 * without kill keeps the same session. At most two sessions are retained.
 */
internal class DebugSessionManager(
    private val sessionDao: DebugSessionDao,
    private val callDao: NetworkCallDao,
) {
    private val _active = MutableStateFlow(PENDING_SESSION)

    /** True after the first successful [ensureInitialSession] in this process instance. */
    private var bootstrappedThisProcess: Boolean = false

    /**
     * Guards the bootstrap-check-and-create sequence in [ensureInitialSession]. Every request's
     * [com.dev.probe.network.NetworkDebugClientPlugin] now calls [ensureInitialSession], so
     * concurrent callers before the first bootstrap completes must not both race into
     * [promoteAndStartCurrent]/[createSession] — that would create two `current` rows and each
     * cleanup would delete the other's just-created session and its captured calls.
     */
    private val bootstrapMutex = Mutex()

    fun activeSession(): StateFlow<DebugSession> = _active.asStateFlow()

    fun availableSessions(): Flow<List<DebugSession>> = sessionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Process bootstrap: first call in a process promotes any prior-process `current` to
     * `previous` and creates a new `current`. Subsequent calls in the same process are
     * idempotent and return the in-memory active session. Single-flight via [bootstrapMutex]:
     * concurrent callers queue up and each sees the already-bootstrapped fast path once the
     * first one finishes, instead of racing into duplicate session creation.
     */
    suspend fun ensureInitialSession(): DebugSession =
        bootstrapMutex.withLock {
            if (bootstrappedThisProcess) {
                val active = _active.value
                if (active.id != PENDING_SESSION.id) return@withLock active
            }

            val existingCurrent = sessionDao.getByRole(SESSION_ROLE_CURRENT)?.toDomain()
            val session =
                if (existingCurrent != null) {
                    promoteAndStartCurrent()
                } else {
                    createSession(role = SessionRole.CURRENT).also { _active.value = it }
                }
            bootstrappedThisProcess = true
            session
        }

    /**
     * Promotes the current session to "previous" and starts a fresh "current" session.
     * Used on process bootstrap when a prior process left a current row in Room.
     */
    private suspend fun promoteAndStartCurrent(): DebugSession {
        val old =
            sessionDao.getByRole(SESSION_ROLE_CURRENT) ?: return createSession(role = SessionRole.CURRENT).also {
                _active.value = it
            }
        sessionDao.updateRole(
            id = old.id,
            role = SESSION_ROLE_PREVIOUS,
            endedAtMillis = Clock.System.now().toEpochMilliseconds(),
        )

        val newSession = createSession(role = SessionRole.CURRENT)
        _active.value = newSession

        val keepIds = listOf(newSession.id, old.id)
        callDao.deleteSessionsNotIn(keepIds)
        sessionDao.deleteSessionsNotIn(keepIds)
        return newSession
    }

    /** Resets all session state, e.g. after app data has been wiped. */
    suspend fun onClearData() {
        callDao.clearAll()
        sessionDao.deleteAll()
        _active.value = PENDING_SESSION
        bootstrappedThisProcess = false
        ensureInitialSession()
    }

    private suspend fun createSession(role: SessionRole): DebugSession {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val ordinal = sessionDao.getAll().size + 1
        val session =
            DebugSession(
                id = "session-$nowMillis",
                label = "Session $ordinal",
                startedAtMillis = nowMillis,
                endedAtMillis = null,
                role = role,
            )
        sessionDao.insert(session.toEntity())
        return session
    }

    private companion object {
        val PENDING_SESSION =
            DebugSession(
                id = "pending",
                label = "Pending",
                startedAtMillis = 0L,
                endedAtMillis = null,
                role = SessionRole.CURRENT,
            )
    }
}
