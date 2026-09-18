package com.dev.probe.exceptions

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSException
import platform.Foundation.NSGetUncaughtExceptionHandler
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSThread

/**
 * `NSSetUncaughtExceptionHandler` takes a C function pointer ([staticCFunction]), which on
 * Kotlin/Native cannot capture an arbitrary closure — the callback must reach through top-level
 * mutable state instead of capturing [installUncaughtExceptionHook]'s parameters directly. Only
 * Objective-C/Swift-visible `NSException`s reach this handler; signal-based traps
 * (SIGSEGV/SIGABRT/SIGILL), which is how most Kotlin/Native fatal crashes actually manifest, do
 * not. Documented as a partial-coverage limitation, not fixed here.
 */
@OptIn(ExperimentalForeignApi::class)
private object IosUncaughtExceptionHookState {
    var onUncaught: ((Throwable, String) -> Unit)? = null
    var previous: CPointer<CFunction<(NSException?) -> Unit>>? = null
}

@OptIn(ExperimentalForeignApi::class)
private fun handleUncaughtException(exception: NSException?) {
    runCatching {
        val threadName = NSThread.currentThread.name ?: "main"
        IosUncaughtExceptionHookState.onUncaught?.invoke(
            RuntimeException(exception?.reason ?: exception?.name ?: "Unknown NSException"),
            threadName,
        )
    }
    IosUncaughtExceptionHookState.previous?.let { it(exception) }
}

/**
 * The returned uninstall lambda captures `previousAtInstallTime` (a regular Kotlin closure,
 * unlike [handleUncaughtException] which is a [staticCFunction] and cannot capture) rather than
 * re-reading [IosUncaughtExceptionHookState.previous] at invoke time — that shared var is only
 * safe to treat as "the handler from before this install" between this point and the next
 * install call, since a second install (were one ever to happen without an intervening
 * uninstall) would overwrite it with a pointer to this handler itself.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun installUncaughtExceptionHook(onUncaught: (throwable: Throwable, threadName: String) -> Unit): () -> Unit {
    val previousAtInstallTime = NSGetUncaughtExceptionHandler()
    IosUncaughtExceptionHookState.previous = previousAtInstallTime
    IosUncaughtExceptionHookState.onUncaught = onUncaught

    NSSetUncaughtExceptionHandler(staticCFunction<NSException?, Unit>(::handleUncaughtException))

    return {
        IosUncaughtExceptionHookState.onUncaught = null
        NSSetUncaughtExceptionHandler(previousAtInstallTime)
    }
}
