package com.dev.probe.exceptions

/**
 * Installs an automatic fatal-exception capture hook, chaining to whatever handler was already
 * installed. Returns a lambda that restores the previous handler — called from
 * `ProbeRuntime.shutdown()`.
 */
internal expect fun installUncaughtExceptionHook(onUncaught: (throwable: Throwable, threadName: String) -> Unit): () -> Unit
