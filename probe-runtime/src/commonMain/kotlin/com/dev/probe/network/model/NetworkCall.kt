package com.dev.probe.network.model

internal data class NetworkCall(
    val id: String,
    val timestampMillis: Long,
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
