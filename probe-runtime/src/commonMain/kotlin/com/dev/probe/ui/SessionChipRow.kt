package com.dev.probe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.session.SessionRole
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold

/**
 * Toggle between the active "Current" session and the read-only "Previous" session in the
 * network inspector toolbar. The previous chip is disabled until a previous session exists.
 */
@Composable
internal fun SessionChipRow(
    selectedRole: SessionRole,
    previousAvailable: Boolean,
    onRoleSelected: (SessionRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionChip(
            label = "Current",
            selected = selectedRole == SessionRole.CURRENT,
            enabled = true,
            onClick = { onRoleSelected(SessionRole.CURRENT) },
        )
        SessionChip(
            label = "Previous (read-only)",
            selected = selectedRole == SessionRole.PREVIOUS,
            enabled = previousAvailable,
            onClick = { onRoleSelected(SessionRole.PREVIOUS) },
        )
    }
}

@Composable
private fun SessionChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    val clickableModifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier

    Text(
        text = label,
        style = typography.labelMedium.semiBold(),
        color =
        when {
            !enabled -> colors.textSecondary.copy(alpha = 0.4f)
            selected -> colors.onPrimary
            else -> colors.textSecondary
        },
        modifier =
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.primary else colors.surface)
            .then(clickableModifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@ThemePreviews
@Composable
private fun SessionChipRowCurrentPreview() {
    ProbeBackgroundPreviewContainer {
        SessionChipRow(
            selectedRole = SessionRole.CURRENT,
            previousAvailable = true,
            onRoleSelected = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SessionChipRowPreviousSelectedPreview() {
    ProbeBackgroundPreviewContainer {
        SessionChipRow(
            selectedRole = SessionRole.PREVIOUS,
            previousAvailable = true,
            onRoleSelected = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SessionChipRowNoPreviousPreview() {
    ProbeBackgroundPreviewContainer {
        SessionChipRow(
            selectedRole = SessionRole.CURRENT,
            previousAvailable = false,
            onRoleSelected = {},
        )
    }
}
