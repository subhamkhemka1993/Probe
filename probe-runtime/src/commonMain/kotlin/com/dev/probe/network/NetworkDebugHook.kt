package com.dev.probe.network

import com.dev.probe.api.HttpClientDebugHook
import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.session.DebugSessionManager
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Wires [NetworkDebugClientPlugin] when registered via [ProbeHttpCapture.setHook].
 * When debug tools are disabled, [NoOpHttpClientDebugHook] is registered instead.
 */
internal class NetworkDebugHook(
    private val repository: NetworkDebugRepository,
    private val debugConfig: ProbeCaptureLimits,
    private val sessionManager: DebugSessionManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : HttpClientDebugHook {

    override fun HttpClientConfig<*>.install() {
        install(NetworkDebugClientPlugin) {
            this.repository = this@NetworkDebugHook.repository
            this.config = this@NetworkDebugHook.debugConfig
            this.scope = this@NetworkDebugHook.scope
            this.sessionManager = this@NetworkDebugHook.sessionManager
        }
    }
}
