package com.dev.probe.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dev.probe.api.ProbeThemeOverride

internal object ProbeThemeDefaults {
    fun colors(override: ProbeThemeOverride? = null): ProbeColors {
        val defaults = defaultColors()
        if (override == null) return defaults
        return defaults.copy(
            primary = override.primary ?: defaults.primary,
            background = override.background ?: defaults.background,
            surface = override.surface ?: defaults.surface,
            textPrimary = override.textPrimary ?: defaults.textPrimary,
            textSecondary = override.textSecondary ?: defaults.textSecondary,
            success = override.success ?: defaults.success,
            warning = override.warning ?: defaults.warning,
            error = override.error ?: defaults.error,
        )
    }

    fun typography(): ProbeTypography = ProbeTypography.fromMaterial3(material3Typography())

    /** Material3 light palette — aligned with default Compose [lightColorScheme]. */
    private fun defaultColors(): ProbeColors {
        val scheme = lightColorScheme()
        return ProbeColors(
            background = scheme.background,
            surface = scheme.surfaceContainerLow,
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            textPrimary = scheme.onBackground,
            textSecondary = scheme.onSurfaceVariant,
            divider = scheme.outlineVariant,
            success = Color(0xFF2E7D32),
            warning = Color(0xFFE65100),
            error = scheme.error,
            methodGet = Color(0xFF2E7D32),
            methodPost = Color(0xFF1565C0),
            methodPut = Color(0xFFE65100),
            methodDelete = Color(0xFFC62828),
            codeBackground = scheme.surfaceContainerHighest,
        )
    }
}

internal val LocalProbeColors = staticCompositionLocalOf { ProbeThemeDefaults.colors() }
internal val LocalProbeTypography = staticCompositionLocalOf { ProbeThemeDefaults.typography() }

@Composable
internal fun ProbeTheme(override: ProbeThemeOverride? = null, content: @Composable () -> Unit) {
    val colors = remember(override) { ProbeThemeDefaults.colors(override) }
    val typography = remember { ProbeThemeDefaults.typography() }
    val materialTypography = remember { material3Typography() }
    val colorScheme =
        remember(colors) {
            lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                background = colors.background,
                surface = colors.surface,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                onSurfaceVariant = colors.textSecondary,
                outlineVariant = colors.divider,
                error = colors.error,
                surfaceContainerLow = colors.surface,
                surfaceContainerHighest = colors.codeBackground,
            )
        }
    CompositionLocalProvider(
        LocalProbeColors provides colors,
        LocalProbeTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
            content = content,
        )
    }
}
