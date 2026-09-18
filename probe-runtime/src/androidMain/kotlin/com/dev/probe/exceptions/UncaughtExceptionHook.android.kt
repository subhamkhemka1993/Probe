package com.dev.probe.exceptions

internal actual fun installUncaughtExceptionHook(onUncaught: (throwable: Throwable, threadName: String) -> Unit): () -> Unit {
    val previous = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler(
        Thread.UncaughtExceptionHandler { thread, throwable ->
            // A bug in our own capture path must never suppress the real (previous) handler.
            runCatching { onUncaught(throwable, thread.name) }
            previous?.uncaughtException(thread, throwable)
        },
    )

    return { Thread.setDefaultUncaughtExceptionHandler(previous) }
}
