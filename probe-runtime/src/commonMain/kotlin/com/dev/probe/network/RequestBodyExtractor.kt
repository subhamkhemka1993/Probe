package com.dev.probe.network

import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent

/**
 * Best-effort extraction of outbound request body text for the network inspector.
 * JSON and form payloads are typically sent as [TextContent] after ContentNegotiation serializes them.
 */
internal fun extractRequestBodyText(body: Any?): String? =
    when (body) {
        null -> null
        is TextContent -> body.text
        is OutgoingContent.ByteArrayContent -> runCatching { body.bytes().decodeToString() }.getOrNull()
        is OutgoingContent.NoContent -> null
        is OutgoingContent -> "[stream body omitted]"
        else -> null
    }
