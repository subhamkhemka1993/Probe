package com.dev.probe.network

import com.dev.probe.network.model.NetworkCall

internal fun buildCurl(call: NetworkCall): String {
    val parts = mutableListOf("curl -X ${call.method}")
    call.requestHeaders
        .filter { (_, value) -> value.isNotEmpty() }
        .forEach { (key, value) ->
            parts += "-H '${escapeSingleQuotes("$key: $value")}'"
        }
    call.requestBody?.takeIf { it.isNotEmpty() }?.let { body ->
        parts += "-d '${escapeSingleQuotes(body)}'"
    }
    parts += "'${escapeSingleQuotes(call.url)}'"
    return parts.joinToString(" \\\n  ")
}

private fun escapeSingleQuotes(value: String): String =
    value.replace("'", "'\"'\"'")
