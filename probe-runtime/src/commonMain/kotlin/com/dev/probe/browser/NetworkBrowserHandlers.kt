package com.dev.probe.browser

import com.dev.probe.export.ExportFormat
import com.dev.probe.export.SessionExporter
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.network.model.NetworkCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class NetworkBrowserHandlers(
    private val repository: NetworkDebugRepository,
    private val maxEntries: Int,
) {
    suspend fun listCalls(
        search: String,
        limit: Int,
        session: String? = null,
    ): NetworkCallsResponse {
        val safeLimit = limit.coerceIn(0, maxEntries)
        val sessionId =
            repository.resolveSessionId(session)
                ?: return NetworkCallsResponse(calls = emptyList())
        return NetworkCallsResponse(
            calls = repository.observeCalls(sessionId, search).firstLimited(safeLimit),
        )
    }

    suspend fun getCall(id: String): NetworkCallDto? = repository.getById(id)?.toDto()

    suspend fun clearCalls(sessionQuery: String? = null): ClearResponse {
        repository.resolveSessionId(sessionQuery)?.let { repository.clear(it) }
        return ClearResponse(cleared = true)
    }

    suspend fun buildCurl(id: String): CurlResponse? = repository.getById(id)?.let(::buildCurl)

    fun buildCurl(call: NetworkCall): CurlResponse =
        CurlResponse(
            curl =
                com.dev.probe.network
                    .buildCurl(call),
        )

    suspend fun exportSession(
        format: String,
        sessionId: String,
    ): String? {
        val exportFormat = format.toExportFormatOrNull() ?: return null
        val resolvedSessionId = repository.resolveSessionId(sessionId) ?: return null
        val calls = repository.observeCalls(resolvedSessionId, "").first().take(maxEntries)
        return SessionExporter.export(calls, exportFormat)
    }
}

private fun String.toExportFormatOrNull(): ExportFormat? =
    when (lowercase()) {
        "json" -> ExportFormat.JSON
        "har" -> ExportFormat.HAR
        "curl" -> ExportFormat.CURL_BUNDLE
        else -> null
    }

private suspend fun Flow<List<NetworkCall>>.firstLimited(limit: Int): List<NetworkCallDto> = first().take(limit).map { it.toDto() }
