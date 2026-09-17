package com.dev.probe.ui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import com.dev.probe.network.ui.NetworkStatusTone
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography

@Composable
internal fun NetworkStatusTone.toBadgeColor(): Color {
    val colors = LocalProbeColors.current
    return when (this) {
        NetworkStatusTone.Success -> colors.success
        NetworkStatusTone.Warning -> colors.warning
        NetworkStatusTone.Error -> colors.error
        NetworkStatusTone.Pending -> colors.textSecondary
        NetworkStatusTone.Neutral -> colors.textSecondary
    }
}

@Composable
internal fun ProbeStatusBadge(
    text: String,
    tone: NetworkStatusTone,
    modifier: Modifier = Modifier,
) {
    val typography = LocalProbeTypography.current
    Text(
        text = text,
        style = typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(tone.toBadgeColor())
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@ThemePreviews
@Composable
private fun ProbeStatusBadgePreview() {
    ProbeBackgroundPreviewContainer {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProbeStatusBadge(text = "200", tone = NetworkStatusTone.Success)
            ProbeStatusBadge(text = "404", tone = NetworkStatusTone.Warning)
            ProbeStatusBadge(text = "500", tone = NetworkStatusTone.Error)
            ProbeStatusBadge(text = "…", tone = NetworkStatusTone.Pending)
        }
    }
}
