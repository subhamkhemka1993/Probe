package com.dev.probe.api

import kotlin.test.Test
import kotlin.test.assertEquals

class ProbeLogSinkTest {
    @Test
    fun defaultWriterIsNoOp() {
        // Must not throw with no writer installed.
        ProbeLogSink.write("INFO", "tag", "message", null)
    }

    @Test
    fun setWriterRoutesToTheInstalledImplementation() {
        var received: String? = null
        ProbeLogSink.setWriter { _, _, message, _ -> received = message }

        ProbeLogSink.write("INFO", "tag", "hello", null)

        assertEquals("hello", received)
        ProbeLogSink.clearWriter()
    }

    /** After [ProbeLogSink.clearWriter], a subsequent write must not throw. */
    @Test
    fun clearWriterRestoresTheNoOpDefault() {
        ProbeLogSink.setWriter { _, _, _, _ -> error("must not be called after clear") }
        ProbeLogSink.clearWriter()

        ProbeLogSink.write("INFO", "tag", "message", null)
    }
}
