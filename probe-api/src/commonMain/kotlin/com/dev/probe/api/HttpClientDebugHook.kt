package com.dev.probe.api

import io.ktor.client.HttpClientConfig

interface HttpClientDebugHook {
    fun HttpClientConfig<*>.install()
}

object NoOpHttpClientDebugHook : HttpClientDebugHook {
    override fun HttpClientConfig<*>.install() = Unit
}
