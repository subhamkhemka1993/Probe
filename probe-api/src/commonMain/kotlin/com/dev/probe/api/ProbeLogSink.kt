package com.dev.probe.api

import kotlin.concurrent.Volatile

/**
 * Host integration point for the debug shell's log capture. A host wires one [Writer] from
 * whatever logging library it uses, once, at its own logging setup call site, then calls [write]
 * directly for every log line — same settable-hook shape as [ProbeState]. Probe never references
 * any particular logging library.
 */
object ProbeLogSink {
    fun interface Writer {
        fun write(severity: String, tag: String, message: String, throwable: Throwable?)
    }

    private object NoOpWriter : Writer {
        override fun write(severity: String, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Volatile
    private var writer: Writer = NoOpWriter

    fun setWriter(writer: Writer) {
        this.writer = writer
    }

    fun clearWriter() {
        writer = NoOpWriter
    }

    fun write(severity: String, tag: String, message: String, throwable: Throwable?) = writer.write(severity, tag, message, throwable)
}
