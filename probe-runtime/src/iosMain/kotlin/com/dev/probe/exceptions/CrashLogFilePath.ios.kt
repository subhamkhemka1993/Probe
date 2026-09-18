package com.dev.probe.exceptions

import com.dev.probe.api.ProbePlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun crashLogFilePath(platform: ProbePlatformContext): String {
    val appSupportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(appSupportDirectory?.path) + "/$CRASH_LOG_FILE_NAME"
}
