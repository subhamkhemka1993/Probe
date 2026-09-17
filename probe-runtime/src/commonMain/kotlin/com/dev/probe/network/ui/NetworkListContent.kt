@file:OptIn(ExperimentalTime::class)

package com.dev.probe.network.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.probe.DebugToolbarIconSize
import com.dev.probe.export.ExportFormat
import com.dev.probe.export.SessionExporter
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.platform.shareFile
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ProbeFullScreenPreview
import com.dev.probe.preview.ProbePreviewData
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.medium
import com.dev.probe.theme.semiBold
import com.dev.probe.ui.ProbeListLabel
import com.dev.probe.ui.primitives.ProbeBottomSheet
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeIconButton
import com.dev.probe.ui.primitives.ProbeSearchField
import com.dev.probe.ui.probeClickable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Request list for the network debug inspector: card-style rows inspired by KtorMonitor /
 * Chucker with status stripe, method badge, path, host, relative time, and duration.
 */
@Composable
internal fun NetworkListContent(
    calls: List<NetworkCall>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCallClick: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    val exportSheetVisible = remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProbeIconButton(
                onClick = onClose,
                icon = Icons.Filled.Close,
                tint = colors.textPrimary,
                size = DebugToolbarIconSize,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Network",
                    style = typography.titleMedium.semiBold(),
                    color = colors.textPrimary,
                )
                Text(
                    text = when (calls.size) {
                        0 -> "No requests"
                        1 -> "1 request"
                        else -> "${calls.size} requests"
                    },
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
            ProbeIconButton(
                onClick = { exportSheetVisible.value = true },
                icon = Icons.Filled.Share,
                enabled = calls.isNotEmpty(),
                tint = colors.textPrimary,
                size = DebugToolbarIconSize,
                contentDescription = "Export session",
            )
            ProbeIconButton(
                onClick = onClear,
                icon = Icons.Filled.Delete,
                enabled = calls.isNotEmpty(),
                tint = colors.textPrimary,
                size = DebugToolbarIconSize,
            )
        }
        ProbeDivider()

        ProbeSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            onDone = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            placeholder = "Search URL, path, status…",
        )

        if (calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NetworkEmptyState(
                    message = if (searchQuery.isBlank()) {
                        "No requests captured yet"
                    } else {
                        "No matching requests"
                    },
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(calls, key = { it.id }) { call ->
                NetworkCallRow(call = call, onClick = { onCallClick(call) })
            }
        }
    }

    ExportSessionSheet(isVisible = exportSheetVisible, calls = calls)
}

/**
 * Bottom sheet offering the session as JSON, HAR, or a cURL bundle via [shareFile]. Exports
 * exactly the observed [calls] list, which the repository already caps to
 * `ProbeCaptureLimits.maxEntries` at the query level, so no separate size guard is needed here.
 */
@Composable
private fun ExportSessionSheet(
    isVisible: MutableState<Boolean>,
    calls: List<NetworkCall>,
    modifier: Modifier = Modifier,
) {
    if (!isVisible.value) return

    ProbeBottomSheet(
        isVisible = isVisible,
        title = "Export session",
        onDismissRequest = {},
        modifier = modifier,
        headerIcon = Icons.Filled.Share,
    ) {
        ExportFormat.entries.forEachIndexed { index, format ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .probeClickable(
                        onClick = {
                            shareFile(
                                fileName = format.exportFileName(),
                                content = SessionExporter.export(calls, format),
                                mimeType = format.exportMimeType(),
                            )
                            isVisible.value = false
                        },
                    )
                    .padding(vertical = 4.dp),
            ) {
                ProbeListLabel(title = format.exportLabel(), subtitle = format.exportSubtitle())
            }
            if (index < ExportFormat.entries.lastIndex) {
                ProbeDivider()
            }
        }
    }
}

private fun ExportFormat.exportLabel(): String = when (this) {
    ExportFormat.JSON -> "JSON"
    ExportFormat.HAR -> "HAR"
    ExportFormat.CURL_BUNDLE -> "cURL bundle"
}

private fun ExportFormat.exportSubtitle(): String = when (this) {
    ExportFormat.JSON -> "Structured request/response list"
    ExportFormat.HAR -> "Import into Chrome DevTools or Charles"
    ExportFormat.CURL_BUNDLE -> "Reproducible curl commands, one per call"
}

private fun ExportFormat.exportMimeType(): String = when (this) {
    ExportFormat.JSON -> "application/json"
    ExportFormat.HAR -> "application/json"
    ExportFormat.CURL_BUNDLE -> "text/plain"
}

private fun ExportFormat.exportFileExtension(): String = when (this) {
    ExportFormat.JSON -> "json"
    ExportFormat.HAR -> "har"
    ExportFormat.CURL_BUNDLE -> "txt"
}

private fun ExportFormat.exportFileName(): String =
    "probe-session-${Clock.System.now().toEpochMilliseconds()}.${exportFileExtension()}"

@ThemePreviews
@Composable
private fun NetworkListContentPreview() {
    ProbeFullScreenPreview {
        NetworkListContent(
            calls = ProbePreviewData.networkCalls,
            searchQuery = "",
            onSearchQueryChange = {},
            onCallClick = {},
            onClear = {},
            onClose = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkListContentEmptyPreview() {
    ProbeFullScreenPreview {
        NetworkListContent(
            calls = emptyList(),
            searchQuery = "",
            onSearchQueryChange = {},
            onCallClick = {},
            onClear = {},
            onClose = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkListContentFilteredPreview() {
    ProbeFullScreenPreview {
        NetworkListContent(
            calls = listOf(ProbePreviewData.getHomeLayout),
            searchQuery = "home-screen",
            onSearchQueryChange = {},
            onCallClick = {},
            onClear = {},
            onClose = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun NetworkCallRow(
    call: NetworkCall,
    onClick: () -> Unit,
) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    val status = call.statusPresentation()

    NetworkCardContainer(
        tone = status.tone,
        modifier = Modifier.probeClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NetworkMethodBadge(method = call.method)
            Text(
                modifier = Modifier.weight(1f),
                text = call.path,
                style = typography.bodyMedium.medium(),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            NetworkStatusBadge(presentation = status)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = call.host,
                style = typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatRelativeTimestamp(call.timestampMillis),
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = call.durationLabel(),
                    style = typography.labelMedium.semiBold(),
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun NetworkCallRowPreview() {
    ProbeBackgroundPreviewContainer(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NetworkCallRow(call = ProbePreviewData.getHomeLayout, onClick = {})
        NetworkCallRow(call = ProbePreviewData.postLogin, onClick = {})
        NetworkCallRow(call = ProbePreviewData.pendingCall, onClick = {})
        NetworkCallRow(call = ProbePreviewData.getNotFound, onClick = {})
        NetworkCallRow(call = ProbePreviewData.failedCall, onClick = {})
    }
}
