package com.dev.probe.api

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProbeCrashCaptureTest {
    // Blanket teardown for ProbeCrashCapture's global reporter state, rather than relying on each
    // individual test's own try/finally — a future test added here without matching discipline
    // would otherwise leak a stale reporter into whichever test in this JVM runs next.
    @AfterTest
    fun tearDown() {
        ProbeCrashCapture.clearReporter()
    }

    @Test
    fun reportCaught_beforeSetReporter_isSilentNoOp() {
        // Must not throw — this is the default no-op contract every other probe-api hook has.
        ProbeCrashCapture.reportCaught(RuntimeException("boom"))
    }

    @Test
    fun reportCaught_afterSetReporter_forwardsAsNonFatal() {
        var seen: Triple<Throwable, String, Boolean>? = null
        ProbeCrashCapture.setReporter { throwable, threadName, isFatal ->
            seen = Triple(throwable, threadName, isFatal)
        }

        val error = RuntimeException("boom")
        ProbeCrashCapture.reportCaught(error, threadName = "worker-1")

        assertEquals(error, seen?.first)
        assertEquals("worker-1", seen?.second)
        assertFalse(seen?.third ?: true)
    }
}
