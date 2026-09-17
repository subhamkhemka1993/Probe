package com.dev.probe.api

import android.content.Context

actual class ProbePlatformContext(context: Context) {
    // Public, not internal: `internal` is scoped per Gradle module, and `:probe-runtime` (a
    // separate module from `:probe-api`) needs to read this.
    val context: Context = context.applicationContext
}
