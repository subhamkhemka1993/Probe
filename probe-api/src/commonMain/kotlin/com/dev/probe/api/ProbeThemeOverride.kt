package com.dev.probe.api

import androidx.compose.ui.graphics.Color

/**
 * Optional palette override for the debug shell's own UI, so it can match your host app's brand
 * instead of its default theme. Every field is optional — anything left `null` falls back to the
 * library's default color.
 */
data class ProbeThemeOverride(
    val primary: Color? = null,
    val background: Color? = null,
    val surface: Color? = null,
    val textPrimary: Color? = null,
    val textSecondary: Color? = null,
    val success: Color? = null,
    val warning: Color? = null,
    val error: Color? = null,
)
