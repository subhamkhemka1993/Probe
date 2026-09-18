package com.dev.probe.api

import kotlin.concurrent.Volatile

/**
 * Host integration point for exception/crash capture. Same settable-hook pattern as
 * [ProbeHttpCapture]/[ProbeLogSink] — this is an event stream (a crash doesn't exist to "register"
 * ahead of time), not a named registry.
 */
object ProbeCrashCapture {
    fun interface Reporter {
        fun report(throwable: Throwable, threadName: String, isFatal: Boolean)
    }

    private object NoOpReporter : Reporter {
        override fun report(throwable: Throwable, threadName: String, isFatal: Boolean) = Unit
    }

    @Volatile
    private var reporter: Reporter = NoOpReporter

    fun setReporter(reporter: Reporter) {
        this.reporter = reporter
    }

    fun clearReporter() {
        reporter = NoOpReporter
    }

    /** Host-driven, non-fatal — safe to call from anywhere, any thread. */
    fun reportCaught(throwable: Throwable, threadName: String = currentThreadName()) {
        reporter.report(throwable, threadName, isFatal = false)
    }

    /**
     * `:probe-runtime`'s automatic uncaught-exception hook calls this. Public (not `internal`)
     * because `:probe-runtime` is a separate Gradle module from this object — Kotlin's `internal`
     * visibility is module-scoped, not package-scoped. Same precedent as [ProbeHttpCapture.setHook].
     */
    fun reportFatal(throwable: Throwable, threadName: String) {
        reporter.report(throwable, threadName, isFatal = true)
    }
}
