package com.dev.probe.browser

import com.dev.probe.network.model.NetworkCall
import kotlinx.datetime.Instant

internal fun NetworkCall.toDto(): NetworkCallDto = NetworkCallDto(
    id = id,
    timestampIso = Instant.fromEpochMilliseconds(timestampMillis).toString(),
    method = method,
    url = url,
    host = host,
    path = path,
    query = query,
    requestHeaders = requestHeaders,
    requestBody = requestBody,
    responseStatus = responseStatus,
    responseHeaders = responseHeaders,
    responseBody = responseBody,
    durationMs = durationMs,
    error = error,
    isComplete = isComplete,
)
