package com.dev.probe.exceptions

import com.dev.probe.api.ProbePlatformContext
import java.io.File

internal actual fun crashLogFilePath(platform: ProbePlatformContext): String =
    File(platform.context.filesDir, CRASH_LOG_FILE_NAME).absolutePath
