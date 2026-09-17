package com.dev.probe.internal

import com.dev.probe.api.ProbeConfig
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.network.NetworkDebugRepository
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.policy.NetworkOutputController
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.session.DebugSessionManager

internal data class ProbeServices(
    val config: ProbeConfig,
    val networkDebugRepository: NetworkDebugRepository,
    val browserController: NetworkBrowserController,
    val outputController: NetworkOutputController,
    val preferencesStore: DebugPreferencesStore,
    val sessionManager: DebugSessionManager,
    val plugins: List<ProbePlugin>,
    val notifierBridge: CaptureNotifierBridge? = null,
)
