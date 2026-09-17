package com.dev.probe.ui.primitives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold

internal data class ContentWeights(val leftWeight: Boolean = true, val rightWeight: Boolean = true)

@Composable
internal fun ProbeListRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leftContent: @Composable RowScope.() -> Unit = {},
    rightContent: @Composable RowScope.() -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    contentWeights: ContentWeights = ContentWeights(leftWeight = true, rightWeight = false),
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(innerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
            Modifier.then(
                if (contentWeights.leftWeight) {
                    Modifier.weight(1f)
                } else {
                    Modifier
                },
            ),
            content = leftContent,
        )
        Row(
            modifier =
            Modifier.then(
                if (contentWeights.rightWeight) {
                    Modifier.weight(1f)
                } else {
                    Modifier
                },
            ),
            horizontalArrangement = Arrangement.End,
            content = rightContent,
        )
    }
}

@ThemePreviews
@Composable
private fun ProbeListRowClickablePreview() {
    ProbeBackgroundPreviewContainer {
        ProbeListRow(
            onClick = {},
            leftContent = {
                Text(
                    text = "Network",
                    style = LocalProbeTypography.current.bodyMedium.semiBold(),
                    color = LocalProbeColors.current.textPrimary,
                )
            },
            rightContent = {
                Text(
                    text = "›",
                    style = LocalProbeTypography.current.bodyMedium,
                    color = LocalProbeColors.current.textSecondary,
                )
            },
        )
    }
}
