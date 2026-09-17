package com.dev.probe.internal

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.api.HttpClientDebugHook
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.createProbeNotifier
import com.dev.probe.browser.LocalAddressProvider
import com.dev.probe.browser.NetworkBrowserConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.network.NetworkDebugHook
import com.dev.probe.network.NetworkDebugPluginUi
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.policy.NetworkOutputController
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager
import kotlinx.coroutines.CoroutineScope

internal object ProbeGraphFactory {
    fun create(config: ProbeConfig, platform: ProbePlatformContext, scope: CoroutineScope): ProbeServices {
        val database = createDatabase(platform)
        val dao = database.networkCallDao()
        val sessionManager = DebugSessionManager(database.debugSessionDao(), dao)
        val debugConfig = ProbeCaptureLimits()
        val dataStore = createPreferencesDataStore(platform)
        val preferencesStore = DebugPreferencesStore(dataStore)
        val repository = NetworkDebugRepository(dao, debugConfig, scope, sessionManager)
        val browserController =
            NetworkBrowserController(
                repository = repository,
                config = NetworkBrowserConfig(),
                addressProvider = LocalAddressProvider(),
                scope = scope,
                isEnabled = config.isEnabled,
                maxEntries = debugConfig.maxEntries,
            )
        val outputController =
            NetworkOutputController(
                prefs = preferencesStore,
                browserController = browserController,
                scope = scope,
            )

        val hook: HttpClientDebugHook =
            if (config.isEnabled()) {
                NetworkDebugHook(repository, debugConfig, sessionManager, scope)
            } else {
                NoOpHttpClientDebugHook
            }
        ProbeHttpCapture.setHook(hook)
        ProbePlatformHolder.init(platform)
        if (config.isEnabled()) outputController.scheduleRestore()

        val notifier = createProbeNotifier(platform)
        val notifierBridge =
            CaptureNotifierBridge(
                repository = repository,
                preferencesStore = preferencesStore,
                browserController = browserController,
                notifier = notifier,
                scope = scope,
            )
        if (config.isEnabled()) notifierBridge.start()

        val plugins =
            listOf(
                NetworkDebugPluginUi(repository = repository, sessionManager = sessionManager),
            )

        return ProbeServices(
            config = config,
            networkDebugRepository = repository,
            browserController = browserController,
            outputController = outputController,
            preferencesStore = preferencesStore,
            sessionManager = sessionManager,
            plugins = plugins,
            notifierBridge = notifierBridge,
        )
    }
}
