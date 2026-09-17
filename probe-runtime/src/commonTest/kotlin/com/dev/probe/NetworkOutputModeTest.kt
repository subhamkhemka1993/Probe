package com.dev.probe

import com.dev.probe.prefs.DebugPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkOutputModeTest {
    @Test fun only_inspector_and_browser_modes_exist() {
        assertEquals(setOf("INSPECTOR", "BROWSER"), NetworkOutputMode.entries.map { it.name }.toSet())
    }

    @Test fun default_mode_is_inspector() {
        assertEquals(NetworkOutputMode.INSPECTOR, DebugPreferences().networkOutputMode)
    }
}
