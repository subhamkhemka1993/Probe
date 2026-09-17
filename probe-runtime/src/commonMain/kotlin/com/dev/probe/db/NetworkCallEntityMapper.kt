package com.dev.probe.db

import com.dev.probe.network.model.NetworkCall
import kotlinx.serialization.json.Json

private val headerJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

internal fun NetworkCallEntity.toDomain(): NetworkCall = NetworkCall(
    id = id,
    timestampMillis = timestampMillis,
    method = method,
    url = url,
    host = host,
    path = path,
    query = query,
    requestHeaders = headerJson.decodeFromString(requestHeadersJson),
    requestBody = requestBody,
    responseStatus = responseStatus,
    responseHeaders = responseHeadersJson?.let { headerJson.decodeFromString(it) },
    responseBody = responseBody,
    durationMs = durationMs,
    error = error,
    isComplete = isComplete,
)

internal fun NetworkCall.toEntity(sessionId: String): NetworkCallEntity = NetworkCallEntity(
    id = id,
    timestampMillis = timestampMillis,
    method = method,
    url = url,
    host = host,
    path = path,
    query = query,
    requestHeadersJson = headerJson.encodeToString(requestHeaders),
    requestBody = requestBody,
    responseStatus = responseStatus,
    responseHeadersJson = responseHeaders?.let { headerJson.encodeToString(it) },
    responseBody = responseBody,
    durationMs = durationMs,
    error = error,
    isComplete = isComplete,
    sessionId = sessionId,
)
