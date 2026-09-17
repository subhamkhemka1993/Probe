package com.dev.probe.devactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.medium
import com.dev.probe.theme.semiBold
import com.dev.probe.ui.primitives.ProbeBottomSheet
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbePrimaryButton
import kotlinx.coroutines.launch

private suspend fun knownPermissionRows(controller: PermissionDevActionsController): List<PermissionRow> =
    KnownPermission.entries.map { known ->
        PermissionRow(id = known.id, label = known.label, state = controller.statusOf(known.id))
    }

private fun PermissionState.displayLabel(): String = when (this) {
    PermissionState.Granted -> "Granted"
    PermissionState.Denied -> "Denied"
    PermissionState.NotDetermined -> "Not requested"
    PermissionState.Unsupported -> "Not declared by host app"
}

@Composable
private fun PermissionState.displayColor(): Color {
    val colors = LocalProbeColors.current
    return when (this) {
        PermissionState.Granted -> colors.success
        PermissionState.Denied -> colors.error
        PermissionState.NotDetermined -> colors.warning
        PermissionState.Unsupported -> colors.textSecondary
    }
}

/** Full sheet: live permission matrix + per-row request action + "Open Settings" footer. */
@Composable
internal fun PermissionDevPanel(
    isVisible: MutableState<Boolean>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberPermissionDevActionsController()
    var rows by remember { mutableStateOf<List<PermissionRow>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isVisible.value, controller) {
        if (isVisible.value) rows = knownPermissionRows(controller)
    }

    ProbeBottomSheet(
        isVisible = isVisible,
        title = "Permissions",
        onDismissRequest = onBack,
        modifier = modifier,
        allowDismiss = true,
        allowBack = true,
        onBackPress = onBack,
        headerIcon = Icons.Filled.Lock,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        footer = {
            ProbePrimaryButton(
                title = "Open Settings",
                onClick = { controller.openAppSettings() },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        PermissionDevPanelContent(
            rows = rows,
            onRequest = { id ->
                scope.launch {
                    controller.request(id)
                    rows = knownPermissionRows(controller)
                }
            },
        )
    }
}

/** Presentational list — split out from [PermissionDevPanel] so it can be previewed without a controller. */
@Composable
internal fun PermissionDevPanelContent(
    rows: List<PermissionRow>,
    onRequest: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEachIndexed { index, row ->
            PermissionDevPanelRow(row = row, onRequest = onRequest)
            if (index < rows.lastIndex) {
                ProbeDivider()
            }
        }
    }
}

@Composable
private fun PermissionDevPanelRow(
    row: PermissionRow,
    onRequest: (String) -> Unit,
) {
    val typography = LocalProbeTypography.current
    val colors = LocalProbeColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = row.label,
                style = typography.bodyMedium.semiBold(),
                color = colors.textPrimary
            )
            Text(
                text = row.state.displayLabel(),
                style = typography.labelMedium.medium(),
                color = row.state.displayColor()
            )
        }
        if (row.state == PermissionState.Denied || row.state == PermissionState.NotDetermined) {
            ProbePrimaryButton(
                title = "Request",
                onClick = { onRequest(row.id) },
                minHeight = 36.dp
            )
        }
    }
}

@ThemePreviews
@Composable
private fun PermissionDevPanelContentPreview() {
    ProbeBackgroundPreviewContainer {
        PermissionDevPanelContent(
            rows = listOf(
                PermissionRow(
                    id = "notifications",
                    label = "Notifications",
                    state = PermissionState.Granted
                ),
                PermissionRow(id = "camera", label = "Camera", state = PermissionState.Denied),
                PermissionRow(
                    id = "location",
                    label = "Location",
                    state = PermissionState.NotDetermined
                ),
            ),
            onRequest = {},
        )
    }
}
