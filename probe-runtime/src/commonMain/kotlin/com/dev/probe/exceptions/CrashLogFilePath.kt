package com.dev.probe.exceptions

import com.dev.probe.api.ProbePlatformContext

internal const val CRASH_LOG_FILE_NAME = "probe_crashes.log"

internal expect fun crashLogFilePath(platform: ProbePlatformContext): String
