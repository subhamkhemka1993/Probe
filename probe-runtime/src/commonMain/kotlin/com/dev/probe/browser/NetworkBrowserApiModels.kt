package com.dev.probe.browser

import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkCallsResponse(val calls: List<NetworkCallDto>)

@Serializable
internal data class NetworkCallDto(
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

@Serializable
internal data class HealthResponse(val status: String, val device: String)

@Serializable
internal data class CurlResponse(val curl: String)

@Serializable
internal data class ClearResponse(val cleared: Boolean)

@Serializable
internal data class WsCallUpdatedEvent(val type: String = "call_updated", val call: NetworkCallDto)
