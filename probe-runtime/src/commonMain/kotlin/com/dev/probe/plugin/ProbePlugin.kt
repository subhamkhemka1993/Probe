package com.dev.probe.plugin

import androidx.compose.runtime.Composable
import com.dev.probe.api.ProbeInspector

interface ProbePlugin : ProbeInspector {
    override val id: String
    val displayName: String
    override val label: String get() = displayName
    val description: String get() = ""

    @Composable
    fun PanelContent(onClose: () -> Unit)
}
