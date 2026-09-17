package com.dev.probe.session

import androidx.room.Room
import com.dev.probe.db.ProbeDatabase
import com.dev.probe.network.InMemoryNetworkCallDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class DebugSessionManagerTest {
    private fun createDatabase(): ProbeDatabase {
        val context = RuntimeEnvironment.getApplication()
        return Room
            .inMemoryDatabaseBuilder<ProbeDatabase>(context = context)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    private fun createManager(database: ProbeDatabase): DebugSessionManager =
        DebugSessionManager(database.debugSessionDao(), database.networkCallDao())

    @Test
    fun ensureInitialSessionCreatesCurrentSessionOnColdStart() = runTest {
        val database = createDatabase()
        val manager = createManager(database)

        val session = manager.ensureInitialSession()

        assertEquals(SessionRole.CURRENT, session.role)
        assertEquals(session, manager.activeSession().value)
        assertEquals(1, database.debugSessionDao().getAll().size)
    }

    @Test
    fun ensureInitialSessionIsIdempotentWithinProcess() = runTest {
        val database = createDatabase()
        val manager = createManager(database)

        val first = manager.ensureInitialSession()
        val second = manager.ensureInitialSession()

        assertEquals(first.id, second.id)
        assertEquals(1, database.debugSessionDao().getAll().size)
    }

    @Test
    fun ensureInitialSessionPromotesPriorProcessCurrentToPrevious() = runTest {
        val database = createDatabase()
        val process1 = createManager(database)
        val s1 = process1.ensureInitialSession()

        // Simulate process death + relaunch: new manager, same Room DB.
        val process2 = createManager(database)
        val s2 = process2.ensureInitialSession()

        assertNotEquals(s1.id, s2.id)
        assertEquals(SessionRole.CURRENT, s2.role)

        val previous = database.debugSessionDao().getByRole("previous")
        assertEquals(s1.id, previous?.id)
        assertTrue((previous?.endedAtMillis ?: 0L) > 0L)
    }

    @Test
    fun ensureInitialSessionKeepsAtMostTwoAcrossProcessBoots() = runTest {
        val database = createDatabase()
        val sessionDao = database.debugSessionDao()

        createManager(database).ensureInitialSession()
        createManager(database).ensureInitialSession()
        val s3 = createManager(database).ensureInitialSession()

        assertEquals(2, sessionDao.getAll().size)
        assertEquals(SessionRole.CURRENT, s3.role)
    }

    @Test
    fun ensureInitialSessionPrunesCallsFromEvictedSessions() = runTest {
        val database = createDatabase()
        val callDao = database.networkCallDao()

        val process1 = createManager(database)
        val s1 = process1.ensureInitialSession()
        callDao.insert(sampleCall(id = "call-1", sessionId = s1.id))

        val process2 = createManager(database)
        val s2 = process2.ensureInitialSession()
        callDao.insert(sampleCall(id = "call-2", sessionId = s2.id))

        createManager(database).ensureInitialSession()

        assertNull(callDao.getById("call-1"))
        assertNotEquals(null, callDao.getById("call-2"))
    }

    @Test
    fun onClearDataResetsSessionsAndCalls() = runTest {
        val database = createDatabase()
        val sessionDao = database.debugSessionDao()
        val callDao = database.networkCallDao()
        val manager = createManager(database)

        val s1 = manager.ensureInitialSession()
        callDao.insert(sampleCall(id = "call-1", sessionId = s1.id))
        // Simulate a second process so previous exists before clear.
        createManager(database).ensureInitialSession()

        manager.onClearData()

        assertEquals(1, sessionDao.getAll().size)
        assertEquals(SessionRole.CURRENT, manager.activeSession().value.role)
        assertNull(callDao.getById("call-1"))
    }

    private fun sampleCall(id: String, sessionId: String) = com.dev.probe.db.NetworkCallEntity(
        id = id,
        timestampMillis = 0L,
        method = "GET",
        url = "https://api.example.com/$id",
        host = "api.example.com",
        path = "/$id",
        query = null,
        requestHeadersJson = "{}",
        requestBody = null,
        responseStatus = 200,
        responseHeadersJson = null,
        responseBody = null,
        durationMs = 10L,
        error = null,
        isComplete = true,
        sessionId = sessionId,
    )

    /**
     * Every request's [com.dev.probe.network.NetworkDebugClientPlugin] now calls
     * [DebugSessionManager.ensureInitialSession], so two requests racing before the first
     * bootstrap resolves invoke it concurrently. [GatedGetByRoleDebugSessionDao] holds both
     * callers' [DebugSessionDao.getByRole] read open on [gate] until released, forcing them to
     * observe the same pre-bootstrap state simultaneously (mirrors the plugin-level race in
     * [com.dev.probe.network.NetworkDebugPluginTest]). Without single-flight guarding,
     * both would race into [DebugSessionManager.promoteAndStartCurrent], creating two `current`
     * rows where the second one's cleanup deletes the first's.
     */
    @Test
    fun ensureInitialSessionIsSingleFlightUnderConcurrentCallers() = runTest {
        val delegateSessionDao = InMemoryDebugSessionDao()
        delegateSessionDao.insert(
            DebugSessionEntity(
                id = "prior-session",
                label = "Prior",
                startedAtMillis = 0L,
                endedAtMillis = null,
                role = SESSION_ROLE_CURRENT,
            ),
        )
        val gate = CompletableDeferred<Unit>()
        val gatedSessionDao = GatedGetByRoleDebugSessionDao(delegateSessionDao, gate)
        val callDao = InMemoryNetworkCallDao()
        val manager = DebugSessionManager(gatedSessionDao, callDao)

        val first = async { manager.ensureInitialSession() }
        val second = async { manager.ensureInitialSession() }
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        val s1 = first.await()
        val s2 = second.await()

        assertEquals(s1.id, s2.id, "concurrent callers must converge on the same bootstrapped session")
        assertEquals(
            1,
            gatedSessionDao.currentInsertCount,
            "concurrent bootstraps must not create two current sessions",
        )

        val activeId = manager.activeSession().value.id
        assertTrue(
            delegateSessionDao.getAll().any { it.id == activeId },
            "the active session must not have been deleted by a concurrent bootstrap's cleanup",
        )
    }
}

/**
 * [DebugSessionDao] wrapper that suspends every [getByRole] call on [gate] and counts [insert]
 * calls for `current`-role sessions, letting a test force two concurrent
 * [DebugSessionManager.ensureInitialSession] callers to observe the same pre-bootstrap state and
 * verify at most one of them ever creates a new session.
 */
private class GatedGetByRoleDebugSessionDao(private val delegate: DebugSessionDao, private val gate: CompletableDeferred<Unit>) :
    DebugSessionDao by delegate {
    var currentInsertCount: Int = 0
        private set

    override suspend fun getByRole(role: String): DebugSessionEntity? {
        gate.await()
        return delegate.getByRole(role)
    }

    override suspend fun insert(session: DebugSessionEntity) {
        if (session.role == SESSION_ROLE_CURRENT) currentInsertCount++
        delegate.insert(session)
    }
}
