@file:OptIn(ExperimentalTime::class)

package com.dev.probe.internal

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.api.HttpClientDebugHook
import com.dev.probe.api.NoOpHttpClientDebugHook
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeCrashCapture
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
import com.dev.probe.exceptions.CrashEntry
import com.dev.probe.exceptions.CrashLogStore
import com.dev.probe.exceptions.ExceptionInspectorPluginUi
import com.dev.probe.exceptions.installUncaughtExceptionHook
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
import kotlinx.coroutines.launch

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

        val crashLogStore = CrashLogStore(platform, capacity = debugConfig.maxExceptionEntries)
        var uninstallCrashHook: (() -> Unit)? = null
        if (isEnabled) {
            // Fire-and-forget: shrinks (does not close) the cold-start window where a crash before
            // the first network request resolves ensureInitialSession() would tag to the
            // PENDING_SESSION placeholder instead of a real session id. Once bootstrap resolves,
            // retag any such entry to the real session id it actually belongs to — otherwise it
            // would stay tagged "pending" and ExceptionInspectorPluginUi would keep showing it
            // under whatever session is current at *view* time, potentially many restarts later.
            scope.launch { crashLogStore.retagPending(sessionManager.ensureInitialSession().id) }
            ProbeCrashCapture.setReporter { throwable, threadName, isFatal ->
                crashLogStore.append(
                    CrashEntry(
                        id = 0L, // reassigned by CrashLogStore.append
                        sessionId = sessionManager.activeSession().value.id,
                        timestampMillis = Clock.System.now().toEpochMilliseconds(),
                        threadName = threadName,
                        isFatal = isFatal,
                        exceptionClassName = throwable::class.simpleName ?: "Throwable",
                        message = throwable.message,
                        stackTrace = throwable.stackTraceToString(),
                    ),
                )
            }
            val uninstallHook = installUncaughtExceptionHook { throwable, threadName ->
                ProbeCrashCapture.reportFatal(throwable, threadName)
            }
            // Unlike the one-shot ensureInitialSession() launch above, this collector runs for as
            // long as `scope` lives — which outlives ProbeRuntime.shutdown() itself, since `scope`
            // is host-owned. Its Job must be cancelled there explicitly (bundled into
            // uninstallCrashHook, ProbeRuntime's one crash-teardown hook) or it keeps calling
            // pruneToSessions on this now-orphaned crashLogStore forever, and a later
            // initialize() would stack a second one on top of it.
            val pruneJob = scope.launch {
                sessionManager.availableSessions().collect { sessions ->
                    // The PENDING_SESSION_ID sentinel never appears in `sessions` (it isn't a
                    // persisted row), so it must be added back explicitly here — otherwise a
                    // crash entry stamped pending during the cold-start window is deleted the
                    // moment the first real session exists, before anyone can ever see it.
                    crashLogStore.pruneToSessions(sessions.map { it.id }.toSet() + DebugSessionManager.PENDING_SESSION_ID)
                }
            }
            uninstallCrashHook = {
                uninstallHook()
                pruneJob.cancel()
            }
        }

        val plugins =
            listOf(
                NetworkDebugPluginUi(repository = repository, sessionManager = sessionManager),
                DataStoreInspectorPluginUi(),
                DatabaseInspectorPluginUi(),
                LogInspectorPluginUi(ringBuffer = logRingBuffer),
                ExceptionInspectorPluginUi(store = crashLogStore, sessionManager = sessionManager),
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
            crashLogStore = crashLogStore,
            uninstallCrashHook = uninstallCrashHook,
        )
    }
}
