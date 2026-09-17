package com.dev.probe.prefs

import com.dev.probe.NetworkOutputMode

internal data class DebugPreferences(
    val networkOutputMode: NetworkOutputMode = NetworkOutputMode.INSPECTOR,
)
