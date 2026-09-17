package com.dev.probe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.medium
import com.dev.probe.theme.semiBold

/** Two-line list label — title + subtitle stacked. */
@Composable
internal fun ProbeListLabel(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = typography.bodyMedium.semiBold(),
            color = colors.textPrimary,
        )
        Text(
            text = subtitle,
            style = typography.labelMedium.medium(),
            color = colors.textSecondary,
        )
    }
}

@ThemePreviews
@Composable
private fun ProbeListLabelPreview() {
    ProbeBackgroundPreviewContainer {
        ProbeListLabel(
            title = "Probe inspector",
            subtitle = "Capture traffic in-app with list and detail views",
        )
    }
}
