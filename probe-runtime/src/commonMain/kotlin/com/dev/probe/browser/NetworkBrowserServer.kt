@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.dev.probe.browser

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.authorization
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class NetworkBrowserServer(
    private val config: NetworkBrowserConfig,
    private val handlers: NetworkBrowserHandlers,
    private val staticAssets: BrowserStaticAssets,
    private val wsSessions: BrowserWebSocketSessions,
    private val deviceName: () -> String = { DEFAULT_DEVICE_NAME },
) {
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var session: BrowserSession? = null

    val port: Int get() = config.port

    fun currentSession(): BrowserSession? = session

    /**
     * Synchronous entry point matching [BrowserServer.start]. When an existing (expired) session
     * must be rotated out, this bridges into the now-suspend [stop] with [runBlocking] rather than
     * making [start] itself suspend — that would cascade the suspend requirement through
     * [BrowserServer]/`RealBrowserServer` and every non-suspend caller of
     * [NetworkBrowserController.start]. This preserves [start]'s pre-existing blocking contract for
     * that narrow rotation path; the primary shutdown path (`NetworkOutputController.applyMode` ->
     * [NetworkBrowserController.stop] -> [stop]) is the one made truly non-blocking.
     */
    fun start(
        onStarted: (BrowserSession) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val existingEngine = engine
        val existingSession = session
        if (existingEngine != null && existingSession != null && existingSession.isValid(nowMillis)) {
            onStarted(existingSession)
            return
        }
        if (existingEngine != null) {
            runBlocking { stop() }
        }

        val newSession = createSession(config.sessionTtlHours)
        var startedEngine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

        try {
            startedEngine =
                embeddedServer(
                    factory = CIO,
                    host = config.bindPolicy.host,
                    port = config.port,
                ) {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(CORS) {
                        anyHost()
                    }
                    install(WebSockets)

                    routing {
                        get("/") {
                            call.respondText(staticAssets.indexHtml(), ContentType.Text.Html)
                        }
                        get("/assets/{path...}") {
                            when (call.parameters.getAll("path")?.joinToString("/")) {
                                "styles.css" ->
                                    call.respondText(
                                        staticAssets.stylesCss(),
                                        ContentType.Text.CSS,
                                    )
                                "app.js" ->
                                    call.respondText(
                                        staticAssets.appJs(),
                                        ContentType.Text.JavaScript,
                                    )
                                else -> call.respond(HttpStatusCode.NotFound)
                            }
                        }

                        route("/api/v1") {
                            install(apiAuthPlugin())

                            get("/health") {
                                call.respond(HealthResponse("ok", deviceName()))
                            }
                            get("/calls") {
                                val search = call.request.queryParameters["search"].orEmpty()
                                val limit =
                                    call.request.queryParameters["limit"]
                                        ?.toIntOrNull()
                                        ?: DEFAULT_LIST_LIMIT
                                val session = call.request.queryParameters["session"]
                                call.respond(handlers.listCalls(search, limit, session))
                            }
                            get("/calls/{id}") {
                                val id = call.parameters["id"]
                                val dto = id?.let { handlers.getCall(it) }
                                if (dto == null) {
                                    call.respond(HttpStatusCode.NotFound)
                                } else {
                                    call.respond(dto)
                                }
                            }
                            delete("/calls") {
                                val session = call.request.queryParameters["session"]
                                call.respond(handlers.clearCalls(session))
                            }
                            get("/calls/{id}/curl") {
                                val id = call.parameters["id"]
                                val response = id?.let { handlers.buildCurl(it) }
                                if (response == null) {
                                    call.respond(HttpStatusCode.NotFound)
                                } else {
                                    call.respond(response)
                                }
                            }
                            get("/export") {
                                val format = call.request.queryParameters["format"].orEmpty()
                                val session = call.request.queryParameters["session"] ?: "current"
                                val exported = handlers.exportSession(format, session)
                                if (exported == null) {
                                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_export_request"))
                                } else {
                                    call.respondText(exported, format.exportContentType())
                                }
                            }
                        }

                        webSocket("/ws/v1/calls") {
                            if (!call.isAuthorized()) {
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
                                return@webSocket
                            }

                            wsSessions.register(this)
                            try {
                                for (ignored in incoming) {
                                    // The browser client only receives server-pushed call updates.
                                }
                            } finally {
                                wsSessions.unregister(this)
                            }
                        }
                    }
                }.start(wait = false)

            engine = startedEngine
            session = newSession
            onStarted(newSession)
        } catch (throwable: Throwable) {
            runCatching {
                startedEngine?.stop(1_000, 2_000)
            }
            session = null
            engine = null
            onError(throwable)
        }
    }

    suspend fun stop() {
        wsSessions.closeAll()
        withContext(Dispatchers.IO) {
            engine?.stop(1_000, 2_000)
        }
        engine = null
        session = null
    }

    fun broadcastCallUpdated(dto: NetworkCallDto) {
        wsSessions.broadcast(WsCallUpdatedEvent(call = dto))
    }

    /**
     * Route-scoped replacement for the deprecated `Route.intercept(ApplicationCallPipeline.Call)`.
     * [io.ktor.server.application.PluginBuilder.onCall] cannot be used here: its `finish()` is
     * `internal`, so it cannot stop the pipeline before the guarded route handler runs, which would
     * make an unauthorized call hit `call.respond()` twice ([call.authorize] already responds) and
     * throw. Reaching into [io.ktor.server.application.RouteScopedPluginBuilder.route] to call the
     * inherited (non-deprecated) [io.ktor.util.pipeline.PipelineContext.finish] keeps the original
     * short-circuiting behavior.
     */
    private fun apiAuthPlugin() =
        createRouteScopedPlugin("ApiAuth") {
            checkNotNull(route) { "apiAuthPlugin must be installed on a route" }
                .intercept(ApplicationCallPipeline.Call) {
                    if (!call.authorize()) {
                        finish()
                    }
                }
        }

    private suspend fun ApplicationCall.authorize(): Boolean {
        if (isAuthorized()) return true

        respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
        return false
    }

    private fun ApplicationCall.isAuthorized(): Boolean =
        NetworkBrowserAuth.isAuthorized(
            session = session,
            bearerToken = request.authorization(),
            queryToken = request.queryParameters["token"],
            nowMillis = Clock.System.now().toEpochMilliseconds(),
        )

    private fun createSession(ttlHours: Int): BrowserSession {
        val createdAtMillis = Clock.System.now().toEpochMilliseconds()
        return BrowserSession(
            token = Uuid.random().toString(),
            createdAtMillis = createdAtMillis,
            expiresAtMillis = createdAtMillis + ttlHours.hours.inWholeMilliseconds,
        )
    }

    @Serializable
    private data class ErrorResponse(
        val error: String,
    )

    private companion object {
        const val DEFAULT_LIST_LIMIT = 250
        const val DEFAULT_DEVICE_NAME = "device"
    }
}

private val BindPolicy.host: String
    get() =
        when (this) {
            BindPolicy.LAN -> "0.0.0.0"
            BindPolicy.LOOPBACK -> "127.0.0.1"
        }

private fun String.exportContentType(): ContentType =
    when (lowercase()) {
        "curl" -> ContentType.Text.Plain
        else -> ContentType.Application.Json
    }
