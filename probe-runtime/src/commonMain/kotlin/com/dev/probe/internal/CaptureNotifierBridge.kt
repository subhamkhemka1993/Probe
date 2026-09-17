package com.dev.probe.internal

import com.dev.probe.api.ProbeNotifier
import com.dev.probe.api.ProbeNotifierState
import com.dev.probe.browser.BrowserConnectionInfo
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.prefs.DebugPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * Observes capture state (repository, prefs, browser connection) and drives [ProbeNotifier]
 * updates, sampled on a fixed period so bursts of network traffic don't flood the notification
 * manager.
 */
internal class CaptureNotifierBridge(
    private val repository: NetworkDebugRepository,
    private val preferencesStore: DebugPreferencesStore,
    private val browserController: NetworkBrowserController,
    private val notifier: ProbeNotifier,
    private val scope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(
                repository.observeActiveCalls(""),
                preferencesStore.preferences,
                browserController.connectionInfo,
            ) { calls, prefs, conn ->
                ProbeNotifierState(
                    requestCount = calls.size,
                    lastStatusCode = calls.firstOrNull()?.responseStatus,
                    outputMode = prefs.networkOutputMode,
                    browserUrl = (conn as? BrowserConnectionInfo.Running)?.wifiUrl,
                )
            }.sample(NOTIFICATION_DEBOUNCE_MS)
                .collect { notifier.show(it) }
        }
    }

    fun stop() = notifier.hide()

    private companion object {
        /**
         * Fixed sampling period, not a reset-per-emission debounce window — [sample] ticks on
         * this schedule regardless of how often the combined state changes, so sustained traffic
         * faster than the period still surfaces periodic updates instead of going silent forever
         * (which a `debounce()`-based pipeline would, since its timer restarts on every emission).
         */
        const val NOTIFICATION_DEBOUNCE_MS = 3_000L
    }
}
