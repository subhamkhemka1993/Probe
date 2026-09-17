package com.dev.probe.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

internal data class ProbeTypography(
    val titleMedium: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val code: TextStyle,
) {
    companion object {
        fun fromMaterial3(typography: Typography = Typography()): ProbeTypography =
            ProbeTypography(
                titleMedium = typography.titleMedium,
                bodyMedium = typography.bodyMedium,
                bodySmall = typography.bodySmall,
                labelLarge = typography.labelLarge,
                labelMedium = typography.labelMedium,
                labelSmall = typography.labelSmall,
                code = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
    }
}

internal fun material3Typography(): Typography = Typography()
