package com.dev.probe.export

import com.dev.probe.network.buildCurl
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.network.redactHeaders
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val exportJson =
    Json {
        encodeDefaults = true
    }

internal object SessionExporter {
    fun export(calls: List<NetworkCall>, format: ExportFormat): String = when (format) {
        ExportFormat.JSON -> exportJson.encodeToString(calls.map { it.toExportDto() })
        ExportFormat.HAR -> HarWriter.write(calls)
        ExportFormat.CURL_BUNDLE -> calls.joinToString("\n\n") { buildCurl(it) }
    }
}

@Serializable
internal data class ExportCallDto(
    val id: String,
    val timestampIso: String,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val query: String?,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseStatus: Int?,
    val responseHeaders: Map<String, String>?,
    val responseBody: String?,
    val durationMs: Long?,
    val error: String?,
    val isComplete: Boolean,
)

internal fun NetworkCall.toExportDto(): ExportCallDto = ExportCallDto(
    id = id,
    timestampIso = Instant.fromEpochMilliseconds(timestampMillis).toString(),
    method = method,
    url = url,
    host = host,
    path = path,
    query = query,
    requestHeaders = redactHeaders(requestHeaders),
    requestBody = requestBody,
    responseStatus = responseStatus,
    responseHeaders = responseHeaders?.let(::redactHeaders),
    responseBody = responseBody,
    durationMs = durationMs,
    error = error,
    isComplete = isComplete,
)
