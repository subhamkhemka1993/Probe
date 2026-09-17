package com.dev.probe.network

import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestBodyExtractorTest {
    @Test
    fun extractsTextContentJsonBody() {
        val body = TextContent("""{"key":"value"}""", ContentType.Application.Json)
        assertEquals("""{"key":"value"}""", extractRequestBodyText(body))
    }

    @Test
    fun returnsNullForNullBody() {
        assertNull(extractRequestBodyText(null))
    }
}
