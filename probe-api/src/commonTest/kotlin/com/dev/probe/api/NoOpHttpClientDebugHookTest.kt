package com.dev.probe.api

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import kotlin.test.Test

class NoOpHttpClientDebugHookTest {
    @Test
    fun installIsNoOp() {
        with(NoOpHttpClientDebugHook) {
            HttpClientConfig<HttpClientEngineConfig>().install()
        }
    }
}
