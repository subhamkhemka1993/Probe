package com.dev.probe.policy

import com.dev.probe.NetworkOutputMode
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.prefs.DebugPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class NetworkOutputController(
    private val prefs: DebugPreferencesStore,
    private val browserController: NetworkBrowserController,
    private val scope: CoroutineScope,
) {
    internal var restoreScheduled: Boolean = false
        private set
    private var restoreJob: Job? = null

    suspend fun applyMode(mode: NetworkOutputMode) {
        cancelPendingWork()
        prefs.setNetworkOutputMode(mode)
        when (mode) {
            NetworkOutputMode.INSPECTOR -> browserController.stop()
            NetworkOutputMode.BROWSER -> browserController.start()
        }
    }

    fun scheduleRestore() {
        cancelPendingWork()
        restoreScheduled = true
        restoreJob = scope.launch {
            restorePersistedMode()
        }
    }

    fun cancelPendingWork() {
        restoreJob?.cancel()
        restoreJob = null
    }

    suspend fun restorePersistedMode() {
        val persistedMode = prefs.preferences.first().networkOutputMode
        currentCoroutineContext().ensureActive()
        when (persistedMode) {
            NetworkOutputMode.INSPECTOR -> browserController.stop()
            NetworkOutputMode.BROWSER -> browserController.start()
        }
    }
}
