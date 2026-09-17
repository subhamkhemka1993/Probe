package com.dev.probe.api

import io.ktor.client.HttpClientConfig
import kotlin.concurrent.Volatile

/**
 * Host integration point for Ktor [io.ktor.client.HttpClient] debug capture.
 * Call [installCapture] from your client builder; [ProbeRuntime.initialize] registers the active hook at startup.
 */
object ProbeHttpCapture {

    @Volatile
    private var hook: HttpClientDebugHook = NoOpHttpClientDebugHook

    /**
     * Public (not `internal`) because the caller (`:probe-runtime`'s `ProbeGraphFactory`/
     * `ProbeRuntime`) lives in a separate Gradle module from this object — Kotlin's `internal`
     * visibility is module-scoped, not package-scoped.
     */
    fun setHook(hook: HttpClientDebugHook) {
        this.hook = hook
    }

    fun HttpClientConfig<*>.installCapture() {
        with(hook) { install() }
    }
}
