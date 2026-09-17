package com.dev.probe

import androidx.compose.ui.unit.dp

internal data class ProbeCaptureLimits(
    val maxEntries: Int = 250,
    val maxBodyBytes: Int = 250_000,
)

internal val DebugToolbarIconSize = 28.dp