package com.dev.probe.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.dev.probe.api.ProbeThemeOverride
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProbeThemeTest {
    @Test
    fun default_uses_material_light_background() {
        val expected = lightColorScheme().background
        assertEquals(expected, ProbeThemeDefaults.colors().background)
    }

    @Test
    fun default_primary_matches_material_light() {
        val expected = lightColorScheme().primary
        assertEquals(expected, ProbeThemeDefaults.colors().primary)
    }

    @Test
    fun default_text_is_dark_on_light_background() {
        val colors = ProbeThemeDefaults.colors()
        val bgLuminance = colors.background.red + colors.background.green + colors.background.blue
        val textLuminance = colors.textPrimary.red + colors.textPrimary.green + colors.textPrimary.blue
        assertTrue(bgLuminance > textLuminance, "background should be lighter than text")
    }

    @Test
    fun override_merges_single_color() {
        val merged = ProbeThemeDefaults.colors(ProbeThemeOverride(primary = Color.Red))
        assertEquals(Color.Red, merged.primary)
    }
}
