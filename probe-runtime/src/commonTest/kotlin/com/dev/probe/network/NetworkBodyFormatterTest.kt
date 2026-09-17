package com.dev.probe.network

import com.dev.probe.network.ui.NetworkBodyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkBodyFormatterTest {
    @Test
    fun prepare_returnsEmptyPlaceholderForNull() {
        assertEquals("(empty)", NetworkBodyFormatter.prepare(null).displayText)
    }

    @Test
    fun prepare_returnsEmptyPlaceholderForBlank() {
        assertEquals("(empty)", NetworkBodyFormatter.prepare("   ").displayText)
    }

    @Test
    fun prepare_prettyPrintsJsonForDisplay() {
        val input = """{"name":"zeb","active":true}"""
        val result = NetworkBodyFormatter.prepare(input, contentType = "application/json")
        assertTrue(result.displayText.contains("\n"))
        assertEquals(input, result.fullText)
    }

    @Test
    fun prepare_leavesPlainTextUnchanged() {
        val input = "plain text body"
        val result = NetworkBodyFormatter.prepare(input, contentType = "text/plain")
        assertEquals(input, result.displayText)
        assertEquals(input, result.fullText)
    }

    @Test
    fun prepare_truncatesLargeDisplayText() {
        val input = "x".repeat(20_000)
        val result = NetworkBodyFormatter.prepare(input)
        assertTrue(result.displayText.length < input.length)
        assertTrue(result.displayText.contains("truncated"))
        assertEquals(input, result.fullText)
    }

    @Test
    fun formatForCopy_prettyPrintsJsonObject() {
        val input = """{"name":"zeb","active":true,"count":3}"""
        val result = NetworkBodyFormatter.formatForCopy(input)
        assertEquals(
            """
            {
              "name": "zeb",
              "active": true,
              "count": 3
            }
            """.trimIndent(),
            result.trim(),
        )
    }

    @Test
    fun formatForCopy_returnsOriginalForNonJson() {
        val input = "plain text body"
        assertEquals(input, NetworkBodyFormatter.formatForCopy(input))
    }

    @Test
    fun formatForCopy_returnsOriginalForInvalidJson() {
        val input = "{not valid json"
        assertEquals(input, NetworkBodyFormatter.formatForCopy(input))
    }
}
