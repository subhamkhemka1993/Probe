@file:OptIn(ExperimentalTime::class)

package com.dev.probe.internal

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.api.HttpClientDebugHook
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.api.ProbeHttpCapture
import com.dev.probe.api.ProbeLogSink
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.createProbeNotifier
import com.dev.probe.browser.LocalAddressProvider
import com.dev.probe.browser.NetworkBrowserConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.datastore.DataStoreInspectorPluginUi
import com.dev.probe.dbinspector.DatabaseInspectorPluginUi
import com.dev.probe.logs.LogEntry
import com.dev.probe.logs.LogInspectorPluginUi
import com.dev.probe.logs.LogRingBuffer
import com.dev.probe.network.NetworkDebugHook
import com.dev.probe.network.NetworkDebugPluginUi
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.policy.NetworkOutputController
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope

internal const val PROBE_SELF_DATABASE_NAME = "Probe"

internal object ProbeGraphFactory {
    fun create(config: ProbeConfig, platform: ProbePlatformContext, scope: CoroutineScope): ProbeServices {
        // Captured once rather than re-evaluated at each call site below: config.isEnabled is
        // documented to be safely backed by dynamic/remote state, so re-reading it repeatedly
        // across this otherwise-atomic construction risks a torn initialization (e.g. the
        // database getting registered under one value and the log writer under another) if the
        // flag flips mid-call.
        val isEnabled = config.isEnabled()

        val database = createDatabase(platform)
        if (isEnabled) {
            ProbeDatabaseCapture.register(PROBE_SELF_DATABASE_NAME, database)
        }
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
            if (isEnabled) {
                NetworkDebugHook(repository, debugConfig, sessionManager, scope)
            } else {
                NoOpHttpClientDebugHook
            }
        ProbeHttpCapture.setHook(hook)
        ProbePlatformHolder.init(platform)
        if (isEnabled) outputController.scheduleRestore()

        val notifier = createProbeNotifier(platform)
        val notifierBridge =
            CaptureNotifierBridge(
                repository = repository,
                preferencesStore = preferencesStore,
                browserController = browserController,
                notifier = notifier,
                scope = scope,
            )
        if (isEnabled) notifierBridge.start()

        val logRingBuffer = LogRingBuffer(capacity = debugConfig.maxLogEntries)
        if (isEnabled) {
            ProbeLogSink.setWriter { severity, tag, message, throwable ->
                logRingBuffer.add(LogEntry(severity, tag, message, throwable, Clock.System.now().toEpochMilliseconds()))
            }
        }

        val plugins =
            listOf(
                NetworkDebugPluginUi(repository = repository, sessionManager = sessionManager),
                DataStoreInspectorPluginUi(),
                DatabaseInspectorPluginUi(),
                LogInspectorPluginUi(ringBuffer = logRingBuffer),
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
