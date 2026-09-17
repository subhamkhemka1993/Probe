package com.dev.probe.network

import kotlin.test.Test
import kotlin.test.assertTrue

class BodyTruncatorTest {
    @Test
    fun truncatesBodyOverMaxBytes() {
        val body = "a".repeat(300_000)
        val result = truncateBody(body, maxBytes = 250_000)
        assertTrue(result.endsWith("[truncated at 250KB]"))
        assertTrue(result.length < body.length)
    }

    /**
     * 1000 emoji chars, 4 UTF-8 bytes each = 4000 bytes but only 1000 chars/2000 UTF-16 units; a
     * char-based `take(n)` would keep far more bytes than the cap. `maxBytes` is deliberately not
     * a multiple of 4 so the cut point lands mid-character, exercising [truncateBody]'s
     * continuation-byte backoff rather than happening to land on a character boundary.
     */
    @Test
    fun truncatesByUtf8ByteLengthNotCharLength() {
        val body = "🎉".repeat(1000)
        val maxBytes = 999
        val result = truncateBody(body, maxBytes)

        val suffixStart = result.indexOf("… [truncated at")
        assertTrue(suffixStart >= 0, "expected a truncation suffix")
        val truncatedContent = result.substring(0, suffixStart)
        assertTrue(
            truncatedContent.encodeToByteArray().size <= maxBytes,
            "truncated content must not exceed the UTF-8 byte cap",
        )
    }
}
