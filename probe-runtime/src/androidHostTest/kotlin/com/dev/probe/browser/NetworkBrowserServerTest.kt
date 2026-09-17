@file:OptIn(ExperimentalTime::class)

package com.dev.probe.browser

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.network.createTestProbeDatabase
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.session.DebugSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class NetworkBrowserServerTest {
    @Test
    fun healthRequiresAuth() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                startServer(server)
                val response = client.get("http://127.0.0.1:${server.port}/api/v1/health")

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun healthWithTokenReturnsOk() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/health") {
                        bearerAuth(session.token)
                    }
                val health = Json.decodeFromString<HealthResponse>(response.bodyAsText())

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("ok", health.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun healthReportsPlatformDeviceName() =
        runTest {
            val testServer = createTestBrowserServer(deviceName = { "Pixel 8 Test" })
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/health") {
                        bearerAuth(session.token)
                    }
                val health = Json.decodeFromString<HealthResponse>(response.bodyAsText())

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Pixel 8 Test", health.device)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun listCallsWithToken() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls") {
                        bearerAuth(session.token)
                    }
                val calls = Json.decodeFromString<NetworkCallsResponse>(response.bodyAsText())

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(1, calls.calls.size)
                assertEquals("call-1", calls.calls.single().id)
                assertEquals("/markets", calls.calls.single().path)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun listCallsWithNegativeLimitReturnsEmptyList() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls?limit=-1") {
                        bearerAuth(session.token)
                    }
                val calls = Json.decodeFromString<NetworkCallsResponse>(response.bodyAsText())

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(0, calls.calls.size)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun listCallsScopesBySessionQueryParam() =
        runTest {
            val database = createTestProbeDatabase()
            val process1Manager = DebugSessionManager(database.debugSessionDao(), database.networkCallDao())
            val process1Repository =
                NetworkDebugRepository(
                    dao = database.networkCallDao(),
                    config = ProbeCaptureLimits(),
                    scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
                    sessionManager = process1Manager,
                )
            process1Manager.ensureInitialSession()
            process1Repository.insertPending(sampleCall(id = "call-old", path = "/legacy"))

            // Simulate process death + relaunch: new manager/repository on the same Room DB.
            val process2Manager = DebugSessionManager(database.debugSessionDao(), database.networkCallDao())
            val process2Repository =
                NetworkDebugRepository(
                    dao = database.networkCallDao(),
                    config = ProbeCaptureLimits(),
                    scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
                    sessionManager = process2Manager,
                )
            process2Manager.ensureInitialSession()
            process2Repository.insertPending(sampleCall(id = "call-new", path = "/fresh"))

            val server =
                NetworkBrowserServer(
                    config = NetworkBrowserConfig(port = freePort(), bindPolicy = BindPolicy.LOOPBACK),
                    handlers = NetworkBrowserHandlers(process2Repository, maxEntries = 250),
                    staticAssets = BrowserStaticAssets(),
                    wsSessions = BrowserWebSocketSessions(),
                )
            val client = HttpClient(CIO)

            try {
                val session = startServer(server)
                val currentResponse =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls?session=current") {
                        bearerAuth(session.token)
                    }
                val previousResponse =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls?session=previous") {
                        bearerAuth(session.token)
                    }
                val currentCalls = Json.decodeFromString<NetworkCallsResponse>(currentResponse.bodyAsText())
                val previousCalls = Json.decodeFromString<NetworkCallsResponse>(previousResponse.bodyAsText())

                assertEquals(listOf("call-new"), currentCalls.calls.map { it.id })
                assertEquals(listOf("call-old"), previousCalls.calls.map { it.id })
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun listCallsWithoutPreviousSessionReturnsEmptyList() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls?session=previous") {
                        bearerAuth(session.token)
                    }
                val calls = Json.decodeFromString<NetworkCallsResponse>(response.bodyAsText())

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(0, calls.calls.size)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun exportRequiresAuth() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                startServer(server)
                val response = client.get("http://127.0.0.1:${server.port}/api/v1/export?format=json")

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun exportWithJsonFormatReturnsRedactedCallList() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/export?format=json&session=current") {
                        bearerAuth(session.token)
                    }
                val body = response.bodyAsText()

                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(body.contains("\"id\":\"call-1\""))
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun exportWithHarFormatReturnsHarLog() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/export?format=har") {
                        bearerAuth(session.token)
                    }
                val body = response.bodyAsText()

                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(body.contains("\"version\":\"1.2\""))
                assertTrue(body.contains("\"entries\""))
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun exportWithUnknownFormatReturnsBadRequest() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/export?format=xml") {
                        bearerAuth(session.token)
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun exportWithoutPreviousSessionReturnsBadRequest() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)
            testServer.repository.insertPending(sampleCall(id = "call-1", path = "/markets"))

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/export?format=json&session=previous") {
                        bearerAuth(session.token)
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun startRotatesExpiredSession() =
        runTest {
            val testServer = createTestBrowserServer(sessionTtlHours = 0)
            val server = testServer.server

            try {
                val firstSession = startServer(server)
                val secondSession = startServer(server)

                assertNotEquals(firstSession.token, secondSession.token)
            } finally {
                server.stop()
            }
        }

    @Test
    fun startCleansUpEngineWhenOnStartedThrows() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val failure = CompletableDeferred<Throwable>()

            try {
                server.start(
                    onStarted = { throw IllegalStateException("callback failed") },
                    onError = { failure.complete(it) },
                )

                assertEquals("callback failed", failure.await().message)
                assertNotNull(startServer(server))
            } finally {
                server.stop()
            }
        }

    @Test
    fun getMissingCallReturnsNotFound() =
        runTest {
            val testServer = createTestBrowserServer()
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                val session = startServer(server)
                val response =
                    client.get("http://127.0.0.1:${server.port}/api/v1/calls/missing") {
                        bearerAuth(session.token)
                    }

                assertEquals(HttpStatusCode.NotFound, response.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    @Test
    fun stopIsSuspendAndIdempotent() =
        runTest(timeout = 3.seconds) {
            val testServer = createTestBrowserServer()
            val server = testServer.server

            startServer(server)

            server.stop()
            server.stop()

            assertNull(server.currentSession())
        }

    @Test
    fun bundledSpaAssetsServeRealContent() =
        runTest {
            val testServer = createTestBrowserServer(staticAssets = BrowserStaticAssets(::loadBrowserResourceForHostTest))
            val server = testServer.server
            val client = HttpClient(CIO)

            try {
                startServer(server)
                val indexResponse = client.get("http://127.0.0.1:${server.port}/")
                val stylesResponse = client.get("http://127.0.0.1:${server.port}/assets/styles.css")
                val scriptResponse = client.get("http://127.0.0.1:${server.port}/assets/app.js")
                val missingResponse = client.get("http://127.0.0.1:${server.port}/assets/missing.txt")
                val indexBody = indexResponse.bodyAsText()
                val stylesBody = stylesResponse.bodyAsText()
                val scriptBody = scriptResponse.bodyAsText()

                assertEquals(HttpStatusCode.OK, indexResponse.status)
                assertTrue(indexBody.contains("Probe Network"))
                assertFalse(indexBody.contains("stub", ignoreCase = true))
                assertEquals(HttpStatusCode.OK, stylesResponse.status)
                assertTrue(stylesBody.contains("method-badge"))
                assertFalse(stylesBody.contains("stub", ignoreCase = true))
                assertEquals(HttpStatusCode.OK, scriptResponse.status)
                assertTrue(scriptBody.contains("call_updated"))
                assertFalse(scriptBody.contains("stub", ignoreCase = true))
                assertEquals(HttpStatusCode.NotFound, missingResponse.status)
            } finally {
                server.stop()
                client.close()
            }
        }

    private fun createTestBrowserServer(
        sessionTtlHours: Int = 8,
        staticAssets: BrowserStaticAssets = BrowserStaticAssets(),
        deviceName: () -> String = { "device" },
    ): TestBrowserServer {
        val database = createTestProbeDatabase()
        val repository =
            NetworkDebugRepository(
                dao = database.networkCallDao(),
                config = ProbeCaptureLimits(),
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
                sessionManager = DebugSessionManager(database.debugSessionDao(), database.networkCallDao()),
            )
        val server =
            NetworkBrowserServer(
                config =
                    NetworkBrowserConfig(
                        port = freePort(),
                        bindPolicy = BindPolicy.LOOPBACK,
                        sessionTtlHours = sessionTtlHours,
                    ),
                handlers = NetworkBrowserHandlers(repository, maxEntries = 250),
                staticAssets = staticAssets,
                wsSessions = BrowserWebSocketSessions(),
                deviceName = deviceName,
            )
        return TestBrowserServer(server, repository)
    }

    private fun loadBrowserResourceForHostTest(path: String): String {
        val rootDir = File(checkNotNull(System.getProperty("user.dir")))
        val candidates =
            listOfNotNull(
                File(rootDir, "probe-runtime/src/commonMain/composeResources/$path"),
                rootDir.parentFile?.let { File(it, "probe-runtime/src/commonMain/composeResources/$path") },
            )
        return candidates.first { it.exists() }.readText()
    }

    private suspend fun startServer(server: NetworkBrowserServer): BrowserSession {
        val started = CompletableDeferred<BrowserSession>()
        val failed = CompletableDeferred<Throwable>()
        server.start(
            onStarted = { started.complete(it) },
            onError = { failed.complete(it) },
        )
        return kotlinx.coroutines.selects
            .select {
                started.onAwait { it }
                failed.onAwait { throw it }
            }.also {
                assertNotNull(server.currentSession())
            }
    }

    private fun sampleCall(
        id: String,
        path: String,
    ) = NetworkCall(
        id = id,
        timestampMillis = Clock.System.now().toEpochMilliseconds(),
        method = "GET",
        url = "https://api.example.com$path",
        host = "api.example.com",
        path = path,
        query = null,
        requestHeaders = emptyMap(),
        requestBody = null,
        responseStatus = 200,
        responseHeaders = emptyMap(),
        responseBody = """{"ok":true}""",
        durationMs = 42L,
        error = null,
        isComplete = true,
    )

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private data class TestBrowserServer(
        val server: NetworkBrowserServer,
        val repository: NetworkDebugRepository,
    )
}
