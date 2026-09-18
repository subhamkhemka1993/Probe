package com.dev.probe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dev.probe.Probe
import com.dev.probe.ProbeScreen
import com.dev.probe.theme.LocalProbeColors

/**
 * Full-screen inspector shell for whichever plugin [Probe.selectedPluginId] names — shown when
 * [Probe.screen] is [ProbeScreen.INSPECTOR].
 */
@Composable
internal fun ProbeInspectorPanel(modifier: Modifier = Modifier) {
    val screen by Probe.screen.collectAsState()
    if (screen != ProbeScreen.INSPECTOR) return
    val colors = LocalProbeColors.current

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val plugin = Probe.plugins.firstOrNull { it.id == Probe.selectedPluginId } ?: Probe.plugins.firstOrNull()
        plugin?.PanelContent(onClose = { Probe.dismissAll() })
    }
}
