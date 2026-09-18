package com.dev.probe.sample

import com.dev.probe.api.ProbeLogSink

/** Stand-in for whatever logging library a real host app already uses. */
object SampleLogger {
    fun i(tag: String, message: String) = ProbeLogSink.write("INFO", tag, message, null)
}
