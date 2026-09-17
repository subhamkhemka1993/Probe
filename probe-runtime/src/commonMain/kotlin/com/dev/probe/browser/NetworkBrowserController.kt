package com.dev.probe.browser

import com.dev.probe.network.NetworkDebugRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface BrowserServer {
    fun start(
        onStarted: (BrowserSession) -> Unit,
        onError: (Throwable) -> Unit,
    )

    suspend fun stop()

    fun broadcastCallUpdated(dto: NetworkCallDto)
}

internal class NetworkBrowserController(
    private val repository: NetworkDebugRepository,
    private val config: NetworkBrowserConfig,
    private val addressProvider: BrowserAddressProvider,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val serverFactory: () -> BrowserServer = {
        RealBrowserServer(
            config = config,
            repository = repository,
            maxEntries = maxEntries,
            deviceName = addressProvider::deviceName,
        )
    },
) {
    private val server by lazy { serverFactory() }
    private val _connectionInfo = MutableStateFlow<BrowserConnectionInfo>(BrowserConnectionInfo.Stopped)

    val connectionInfo: StateFlow<BrowserConnectionInfo> = _connectionInfo.asStateFlow()

    private var observeJob: Job? = null
    private var lastSnapshot: Map<String, NetworkCallDto> = emptyMap()

    fun start() {
        if (!isEnabled()) return

        cancelObservation(resetSnapshot = false)
        server.start(
            onStarted = { session ->
                publishRunning(session)
                startObservation()
            },
            onError = { throwable ->
                _connectionInfo.value =
                    BrowserConnectionInfo.Error(
                        throwable.message ?: DEFAULT_ERROR_MESSAGE,
                    )
            },
        )
    }

    suspend fun stop() {
        if (!isEnabled()) return

        cancelObservation(resetSnapshot = true)
        server.stop()
        _connectionInfo.value = BrowserConnectionInfo.Stopped
    }

    /**
     * Called from the app's process-lifetime lifecycle observer on backgrounding. Stops the
     * server when [NetworkBrowserConfig.autoStopOnBackground] is enabled, closing the
     * unauthenticated-LAN exposure window while the app isn't in the foreground.
     *
     * Fire-and-forget by design: this runs on the app's main/UI lifecycle callback, and `stop()`
     * can take up to a few seconds (`engine.stop(...)`'s grace period) — blocking that thread
     * would freeze the UI on every backgrounding. Launching on [scope] keeps the call itself
     * synchronous while the real work happens off-thread.
     */
    fun onLifecyclePause() {
        if (config.autoStopOnBackground && _connectionInfo.value is BrowserConnectionInfo.Running) {
            scope.launch { stop() }
        }
    }

    private fun startObservation() {
        observeJob =
            scope.launch {
                repository.observeActiveCalls("").collectLatest { calls ->
                    val nextSnapshot =
                        calls.associate { call ->
                            val dto = call.toDto()
                            call.id to dto
                        }

                    nextSnapshot.forEach { (id, dto) ->
                        if (lastSnapshot[id] != dto) {
                            server.broadcastCallUpdated(dto)
                        }
                    }
                    lastSnapshot = nextSnapshot
                }
            }
    }

    private fun cancelObservation(resetSnapshot: Boolean) {
        observeJob?.cancel()
        observeJob = null
        if (resetSnapshot) {
            lastSnapshot = emptyMap()
        }
    }

    private fun publishRunning(session: BrowserSession) {
        val tokenQuery = "?token=${session.token}"
        val wifiIpAddress = addressProvider.wifiIpAddress() ?: FALLBACK_IP_ADDRESS

        _connectionInfo.value =
            BrowserConnectionInfo.Running(
                port = config.port,
                token = session.token,
                wifiUrl = "http://$wifiIpAddress:${config.port}/$tokenQuery",
                emulatorUrl =
                    if (addressProvider.isEmulator()) {
                        "http://$ANDROID_EMULATOR_HOST:${config.port}/$tokenQuery"
                    } else {
                        null
                    },
                simulatorUrl =
                    if (addressProvider.isSimulator()) {
                        "http://$LOCALHOST:${config.port}/$tokenQuery"
                    } else {
                        null
                    },
            )
    }

    private class RealBrowserServer(
        config: NetworkBrowserConfig,
        repository: NetworkDebugRepository,
        maxEntries: Int,
        deviceName: () -> String,
    ) : BrowserServer {
        private val delegate =
            NetworkBrowserServer(
                config = config,
                handlers = NetworkBrowserHandlers(repository, maxEntries),
                staticAssets = BrowserStaticAssets(),
                wsSessions = BrowserWebSocketSessions(),
                deviceName = deviceName,
            )

        override fun start(
            onStarted: (BrowserSession) -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            delegate.start(onStarted, onError)
        }

        override suspend fun stop() {
            delegate.stop()
        }

        override fun broadcastCallUpdated(dto: NetworkCallDto) {
            delegate.broadcastCallUpdated(dto)
        }
    }

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 250
        const val DEFAULT_ERROR_MESSAGE = "bind failed"
        const val FALLBACK_IP_ADDRESS = "0.0.0.0"
        const val ANDROID_EMULATOR_HOST = "10.0.2.2"
        const val LOCALHOST = "127.0.0.1"
    }
}
