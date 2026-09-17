@file:OptIn(ExperimentalTime::class)

package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.session.DebugSessionManager
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.save
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Configuration for [NetworkDebugClientPlugin].
 *
 * [repository], [scope], and [sessionManager] must be supplied by [NetworkDebugHook]; the plugin
 * becomes a no-op if any is left unset so it is safe to install defensively.
 */
internal class NetworkDebugPluginConfig {
    var repository: NetworkDebugRepository? = null
    var scope: CoroutineScope? = null
    var sessionManager: DebugSessionManager? = null
    var config: ProbeCaptureLimits = ProbeCaptureLimits()
}

/**
 * Captures HTTP traffic for the in-app network inspector (Chucker-like, always on in debug).
 *
 * **Performance:** the request hot path is `proceed()` + [save] only (required so callers can
 * still read bodies) — except for a binary response, where [save] is skipped entirely so a large
 * download is never buffered into memory just to be captured as a placeholder. Metadata is
 * written as a pending row synchronously; body parsing and the final store update run on [scope]
 * so JSON decode / string work stays off the caller thread.
 *
 * **Capacity:** [NetworkDebugRepository.insertPending] enforces a count cap (250) only; there is
 * no time-based eviction.
 *
 * **Cold start:** [sessionManager]`.ensureInitialSession()` is awaited as the first step of every
 * [Send] interception (not launched separately) so the very first request after process start
 * never captures under the session manager's pending sentinel. Past the first request it is a
 * cheap idempotent read. Guarded by `runCatching` like [NetworkDebugRepository.insertPending]
 * below it: a session-bootstrap failure (e.g. a Room I/O error) must not fail the real request,
 * it just leaves the capture (if any) tagged with whatever session id was already active.
 */
internal val NetworkDebugClientPlugin: ClientPlugin<NetworkDebugPluginConfig> =
    createClientPlugin("NetworkDebugPlugin", ::NetworkDebugPluginConfig) {
        val repository = pluginConfig.repository ?: return@createClientPlugin
        val scope = pluginConfig.scope ?: return@createClientPlugin
        val sessionManager = pluginConfig.sessionManager ?: return@createClientPlugin
        val debugConfig = pluginConfig.config

        @OptIn(ExperimentalUuidApi::class)
        fun newCallId(): String = Uuid.random().toString()

        on(Send) { request ->
            runCatching { sessionManager.ensureInitialSession() }.rethrowIfCancelled()

            val callId = newCallId()
            val startedAt = Clock.System.now().toEpochMilliseconds()

            runCatching { repository.insertPending(buildPendingCall(request, callId, startedAt)) }
                .rethrowIfCancelled()

            try {
                val origin = proceed(request)
                val contentType = origin.response.headers[HttpHeaders.ContentType]
                val responseCall = if (isBinaryContentType(contentType)) origin else origin.save()
                scope.launch {
                    completeCapture(
                        repository = repository,
                        debugConfig = debugConfig,
                        callId = callId,
                        startedAt = startedAt,
                        request = request,
                        saved = responseCall,
                    )
                }
                responseCall
            } catch (cause: CancellationException) {
                scope.launch {
                    failCapture(
                        repository = repository,
                        callId = callId,
                        startedAt = startedAt,
                        error = null,
                    )
                }
                throw cause
            } catch (cause: Throwable) {
                scope.launch {
                    failCapture(
                        repository = repository,
                        callId = callId,
                        startedAt = startedAt,
                        error = cause.message ?: cause::class.simpleName ?: "Unknown error",
                    )
                }
                throw cause
            }
        }
    }

/**
 * Rethrows a [CancellationException] captured by a `runCatching` capture guard, so a request
 * coroutine cancelled while suspended inside that guard stops immediately instead of running the
 * rest of the interception on a dead coroutine. Every other failure stays swallowed — capture work
 * must never fail the real request. Matches the explicit `catch (cause: CancellationException)`
 * rethrow around `proceed()`.
 */
private fun Result<*>.rethrowIfCancelled() {
    val cause = exceptionOrNull()
    if (cause is CancellationException) throw cause
}

private fun buildPendingCall(request: HttpRequestBuilder, callId: String, startedAt: Long): NetworkCall {
    val url = request.url.build()
    return NetworkCall(
        id = callId,
        timestampMillis = startedAt,
        method = request.method.value,
        url = url.toString(),
        host = url.host,
        path = url.encodedPath,
        query = url.encodedQuery.ifEmpty { null },
        requestHeaders = redactHeaders(request.captureHeaders()),
        requestBody = null,
        responseStatus = null,
        responseHeaders = null,
        responseBody = null,
        durationMs = null,
        error = null,
        isComplete = false,
    )
}

private suspend fun completeCapture(
    repository: NetworkDebugRepository,
    debugConfig: ProbeCaptureLimits,
    callId: String,
    startedAt: Long,
    request: HttpRequestBuilder,
    saved: HttpClientCall,
) {
    val contentType = saved.response.headers[HttpHeaders.ContentType]
    val responseBodyText =
        if (isBinaryContentType(contentType)) {
            "[binary body omitted]"
        } else {
            runCatching { saved.response.bodyAsText() }.getOrElse { "[body unavailable]" }
        }
    val durationMs = Clock.System.now().toEpochMilliseconds() - startedAt

    repository.update(callId) {
        it.copy(
            requestBody = truncateBody(extractRequestBodyText(request.body), debugConfig.maxBodyBytes),
            responseStatus = saved.response.status.value,
            responseHeaders = redactHeaders(saved.response.headers.toStringMap()),
            responseBody = truncateBody(responseBodyText, debugConfig.maxBodyBytes),
            durationMs = durationMs,
            error = null,
            isComplete = true,
        )
    }
}

private suspend fun failCapture(repository: NetworkDebugRepository, callId: String, startedAt: Long, error: String?) {
    val durationMs = Clock.System.now().toEpochMilliseconds() - startedAt
    repository.update(callId) {
        it.copy(
            durationMs = durationMs,
            error = error,
            isComplete = true,
        )
    }
}

private fun HttpRequestBuilder.captureHeaders(): Map<String, String> {
    val merged = LinkedHashMap<String, String>()
    (body as? OutgoingContent)?.let { content ->
        content.contentType?.let { merged[HttpHeaders.ContentType] = it.toString() }
        content.contentLength?.let { merged[HttpHeaders.ContentLength] = it.toString() }
    }
    headers.entries().forEach { (key, values) -> merged[key] = values.joinToString("; ") }
    return merged
}

private fun Headers.toStringMap(): Map<String, String> = entries().associate { (key, values) -> key to values.joinToString("; ") }

private val BINARY_CONTENT_TYPE_PREFIXES = listOf("image/", "audio/", "video/", "font/")
private val BINARY_CONTENT_TYPES =
    setOf(
        "application/octet-stream",
        "application/pdf",
        "application/zip",
        "application/gzip",
        "application/x-protobuf",
    )

private fun isBinaryContentType(contentType: String?): Boolean {
    if (contentType == null) return false
    val normalized = contentType.substringBefore(';').trim().lowercase()
    return BINARY_CONTENT_TYPE_PREFIXES.any { normalized.startsWith(it) } || normalized in BINARY_CONTENT_TYPES
}
