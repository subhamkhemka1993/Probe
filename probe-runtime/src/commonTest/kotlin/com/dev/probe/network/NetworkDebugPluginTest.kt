package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.session.DebugSessionDao
import com.dev.probe.session.DebugSessionEntity
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.InMemoryDebugSessionDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Exercises [NetworkDebugClientPlugin] through a real Ktor request pipeline: a [MockEngine]-backed
 * [HttpClient] with the plugin installed against a real [NetworkDebugRepository] backed by an
 * [InMemoryNetworkCallDao] whose failure behavior each test controls.
 */
internal class NetworkDebugPluginTest {
    private fun createRepository(
        dao: InMemoryNetworkCallDao,
        scope: CoroutineScope,
        config: ProbeCaptureLimits = ProbeCaptureLimits(),
        sessionManager: DebugSessionManager = DebugSessionManager(InMemoryDebugSessionDao(), dao),
    ): NetworkDebugRepository = NetworkDebugRepository(
        dao = dao,
        config = config,
        scope = scope,
        sessionManager = sessionManager,
    )

    private fun createClient(
        repository: NetworkDebugRepository,
        scope: CoroutineScope,
        sessionManager: DebugSessionManager,
        engine: MockEngine = MockEngine { respond("ok", HttpStatusCode.OK) },
    ): HttpClient = HttpClient(engine) {
        install(NetworkDebugClientPlugin) {
            this.repository = repository
            this.scope = scope
            this.sessionManager = sessionManager
        }
    }

    @Test
    fun requestSucceedsEvenWhenInsertPendingThrows() = runTest {
        val dao = InMemoryNetworkCallDao().apply { throwOnNextInsert = true }
        val sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), dao)
        val repository = createRepository(dao, scope = this, sessionManager = sessionManager)
        val client = createClient(repository, scope = this, sessionManager = sessionManager)

        val response: HttpResponse = client.get("https://api.example.com/test")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Reproduces the cold-start capture race: [DebugSessionManager.ensureInitialSession] is held
     * open on [sessionInsertGate] while the very first request is fired, mirroring the window
     * between [NetworkDebugHook.install] and the plugin's first [io.ktor.client.plugins.api.Send]
     * interception. The gate lets the test deterministically observe the state of the stored row
     * while the session bootstrap is still in flight, then release it to let bootstrap finish.
     */
    @Test
    fun firstRequestAfterColdStartGetsRealSessionId() = runTest {
        val dao = InMemoryNetworkCallDao()
        val sessionInsertGate = CompletableDeferred<Unit>()
        val sessionManager =
            DebugSessionManager(
                FirstInsertGatedDebugSessionDao(InMemoryDebugSessionDao(), sessionInsertGate),
                dao,
            )
        val repository = createRepository(dao, scope = this, sessionManager = sessionManager)
        val hook =
            NetworkDebugHook(
                repository = repository,
                debugConfig = ProbeCaptureLimits(),
                sessionManager = sessionManager,
                scope = this,
            )
        val client =
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }) {
                with(hook) { install() }
            }

        val requestJob = launch { client.get("https://api.example.com/test") }
        runCurrent()

        sessionInsertGate.complete(Unit)
        advanceUntilIdle()
        requestJob.join()

        val resolvedSessionId = sessionManager.activeSession().value.id
        val pendingRows = dao.observeSearch(sessionId = "pending", query = "", limit = 10).first()
        val resolvedRows = dao.observeSearch(sessionId = resolvedSessionId, query = "", limit = 10).first()

        assertTrue(pendingRows.isEmpty(), "captured call must not be stored under the pending session sentinel")
        assertEquals(1, resolvedRows.size)
    }

    /**
     * Mirrors [requestSucceedsEvenWhenInsertPendingThrows]: the `sessionManager.ensureInitialSession()`
     * await added ahead of [NetworkDebugRepository.insertPending] must be guarded the same way — a
     * session-bootstrap storage failure (simulated via [InMemoryDebugSessionDao.throwOnNextInsert])
     * must not propagate out of `on(Send)` and fail the real HTTP request.
     */
    @Test
    fun requestSucceedsEvenWhenSessionBootstrapThrows() = runTest {
        val dao = InMemoryNetworkCallDao()
        val sessionManager =
            DebugSessionManager(
                InMemoryDebugSessionDao().apply { throwOnNextInsert = true },
                dao,
            )
        val repository = createRepository(dao, scope = this, sessionManager = sessionManager)
        val client = createClient(repository, scope = this, sessionManager = sessionManager)

        val response: HttpResponse = client.get("https://api.example.com/test")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Proves the binary content-type check runs in `on(Send)` ahead of [io.ktor.client.call.save]
     * rather than after — inside `completeCapture` — where it would already be too late. Ktor's core
     * `SaveBody` plugin (installed on every [HttpClient]) already eagerly buffers the body of any
     * non-streaming call (`client.get(...)`) regardless of what any [Send]-hook plugin does, so this
     * test uses the streaming call form (`prepareGet(...).execute { }`) — the one calling convention
     * where Ktor's own auto-save is skipped ([io.ktor.client.request.HttpRequestBuilder]'s internal
     * `skipSaveBody()`) and the plugin's own `on(Send)` ordering is what actually decides whether the
     * body is read. The mocked response body is a [ByteChannel] closed via [ByteChannel.cancel] with a
     * cause, so *any* read attempt throws immediately, including the eager whole-body buffering
     * [io.ktor.client.call.save] performs. The request completing successfully, with the row captured
     * as `"[binary body omitted]"`, is only possible if that body was never read.
     */
    @Test
    fun binaryResponseIsNotBufferedBeforeContentTypeCheck() = runTest {
        val dao = InMemoryNetworkCallDao()
        val sessionManager = DebugSessionManager(InMemoryDebugSessionDao(), dao)
        val repository = createRepository(dao, scope = this, sessionManager = sessionManager)
        val unreadableBody =
            ByteChannel().apply {
                cancel(IllegalStateException("binary response body must never be read by the debug plugin"))
            }
        val engine =
            MockEngine {
                respond(
                    content = unreadableBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
                )
            }
        val client = createClient(repository, scope = this, sessionManager = sessionManager, engine = engine)

        val status = client.prepareGet("https://api.example.com/file.pdf").execute { response -> response.status }
        advanceUntilIdle()

        assertEquals(HttpStatusCode.OK, status)
        val sessionId = sessionManager.activeSession().value.id
        val rows = dao.observeSearch(sessionId = sessionId, query = "", limit = 10).first()
        assertEquals(1, rows.size)
        assertEquals("[binary body omitted]", rows.first().responseBody)
    }

    /**
     * `runCatching { sessionManager.ensureInitialSession() }` must rethrow a
     * [CancellationException], not swallow it — otherwise a request cancelled while suspended
     * there would continue on into [NetworkDebugRepository.insertPending] on an already-cancelled
     * coroutine and (since this in-memory DAO's `insert` never actually suspends) write a pending
     * row despite the caller having already given up on the request.
     */
    @Test
    fun cancellingWhileSessionBootstrapIsInFlightPropagatesCancellation() = runTest {
        val dao = InMemoryNetworkCallDao()
        val sessionGate = CompletableDeferred<Unit>()
        val sessionManager =
            DebugSessionManager(
                FirstInsertGatedDebugSessionDao(InMemoryDebugSessionDao(), sessionGate),
                dao,
            )
        val repository = createRepository(dao, scope = this, sessionManager = sessionManager)
        val hook =
            NetworkDebugHook(
                repository = repository,
                debugConfig = ProbeCaptureLimits(),
                sessionManager = sessionManager,
                scope = this,
            )
        val client =
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }) {
                with(hook) { install() }
            }

        val requestJob = launch { client.get("https://api.example.com/test") }
        runCurrent() // let the request suspend inside ensureInitialSession(), waiting on sessionGate

        requestJob.cancel()
        advanceUntilIdle()

        assertTrue(requestJob.isCancelled)
        val pendingRows = dao.observeSearch(sessionId = "pending", query = "", limit = 10).first()
        assertTrue(pendingRows.isEmpty(), "a cancelled request must never store a pending capture row")
    }
}

/**
 * [DebugSessionDao] wrapper whose first [insert] suspends on [gate], letting a test hold
 * [DebugSessionManager.ensureInitialSession] open long enough to observe requests racing ahead of it.
 */
private class FirstInsertGatedDebugSessionDao(private val delegate: DebugSessionDao, private val gate: CompletableDeferred<Unit>) :
    DebugSessionDao by delegate {
    private var hasGatedFirstInsert = false

    override suspend fun insert(session: DebugSessionEntity) {
        if (!hasGatedFirstInsert) {
            hasGatedFirstInsert = true
            gate.await()
        }
        delegate.insert(session)
    }
}
