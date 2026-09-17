package com.dev.probe.network

private val SENSITIVE_HEADERS =
    setOf(
        "authorization",
        "sessiontoken",
        "cookie",
        "x-api-key",
        "x-auth-token",
    )

internal fun redactHeaders(headers: Map<String, String>): Map<String, String> = headers.mapValues { (key, value) ->
    if (key.lowercase() in SENSITIVE_HEADERS) "***" else value
}
