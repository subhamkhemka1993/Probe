package com.dev.probe.ui.primitives

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography

@Composable
internal fun ProbeDivider(modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = colors.divider,
    )
}

@ThemePreviews
@Composable
private fun ProbeDividerPreview() {
    ProbeBackgroundPreviewContainer {
        val typography = LocalProbeTypography.current
        val colors = LocalProbeColors.current
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = "Above", style = typography.bodyMedium, color = colors.textPrimary)
            ProbeDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "Below", style = typography.bodyMedium, color = colors.textPrimary)
        }
    }
}
