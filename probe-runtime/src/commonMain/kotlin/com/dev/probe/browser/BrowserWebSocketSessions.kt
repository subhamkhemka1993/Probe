package com.dev.probe.browser

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BrowserWebSocketSessions(
    private val json: Json =
        Json {
            // app.js matches on type === "call_updated"; defaults are omitted otherwise.
            encodeDefaults = true
        },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val sessions = mutableSetOf<DefaultWebSocketServerSession>()

    suspend fun register(session: DefaultWebSocketServerSession) {
        mutex.withLock {
            sessions += session
        }
    }

    suspend fun unregister(session: DefaultWebSocketServerSession) {
        mutex.withLock {
            sessions -= session
        }
    }

    fun broadcast(event: WsCallUpdatedEvent) {
        scope.launch {
            val payload = json.encodeToString(event)
            snapshot().forEach { session ->
                runCatching {
                    session.send(Frame.Text(payload))
                }.onFailure {
                    unregister(session)
                }
            }
        }
    }

    suspend fun closeAll() {
        val sessionsToClose = drain()
        scope.launch {
            sessionsToClose.forEach { session ->
                runCatching {
                    withTimeout(CLOSE_TIMEOUT_MILLIS) {
                        session.close(CloseReason(CloseReason.Codes.NORMAL, "server stopped"))
                    }
                }
            }
        }
    }

    private suspend fun drain(): List<DefaultWebSocketServerSession> = mutex.withLock {
        sessions.toList().also {
            sessions.clear()
        }
    }

    private suspend fun snapshot(): List<DefaultWebSocketServerSession> = mutex.withLock { sessions.toList() }

    private companion object {
        const val CLOSE_TIMEOUT_MILLIS = 500L
    }
}
