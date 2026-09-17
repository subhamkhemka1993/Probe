package com.dev.probe.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dev.probe.DebugToolbarIconSize
import com.dev.probe.network.buildCurl
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.platform.shareText
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.preview.ProbeFullScreenPreview
import com.dev.probe.preview.ProbePreviewData
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeIconButton
import com.dev.probe.ui.primitives.ProbeTabBar
import com.dev.probe.ui.primitives.ProbeTabBarStyle
import com.dev.probe.ui.primitives.rememberProbeTabState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DetailTab { Overview, Request, Response }

private enum class PayloadTab { Headers, Body }

private val CardShape = RoundedCornerShape(12.dp)

/**
 * Full detail view for a single captured [NetworkCall] with KtorMonitor-style tabbed
 * navigation: Overview | Request | Response, and Headers | Body sub-tabs per payload.
 */
@Composable
internal fun NetworkDetailContent(
    call: NetworkCall,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val curlCommand = remember(call.id) { buildCurl(call) }
    val status = call.statusPresentation()
    val detailTabState = rememberProbeTabState(initialSelectedIndex = 0, style = ProbeTabBarStyle.Tertiary)
    val selectedDetailTab = DetailTab.entries.getOrElse(detailTabState.selectedIndex.intValue) {
        DetailTab.Overview
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProbeIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                tint = colors.textPrimary,
                size = DebugToolbarIconSize,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.path,
                    style = typography.titleMedium.semiBold(),
                    color = colors.textPrimary,
                )
                Text(
                    text = call.host,
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
            NetworkStatusBadge(
                presentation = status,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        ProbeDivider()

        ProbeTabBar(
            tabItems = DetailTab.entries.map { it.name },
            tabState = detailTabState,
            modifier = Modifier.fillMaxWidth(),
            onTabSelected = { detailTabState.select(it) },
        )

        when (selectedDetailTab) {
            DetailTab.Overview -> OverviewTab(
                modifier = Modifier.weight(1f),
                call = call,
                status = status,
                onCopyCurl = { clipboard.setText(AnnotatedString(curlCommand)) },
                onShareCurl = { shareText(curlCommand) },
            )

            DetailTab.Request -> PayloadTabContent(
                modifier = Modifier.weight(1f),
                headers = call.requestHeaders,
                body = call.requestBody,
                contentType = call.requestHeaders.contentType(),
                onCopyBody = { fullText ->
                    scope.launch {
                        val formatted = withContext(Dispatchers.Default) {
                            NetworkBodyFormatter.formatForCopy(
                                fullText,
                                call.requestHeaders.contentType(),
                            )
                        }
                        clipboard.setText(AnnotatedString(formatted))
                    }
                },
            )

            DetailTab.Response -> PayloadTabContent(
                modifier = Modifier.weight(1f),
                headers = call.responseHeaders.orEmpty(),
                body = call.responseBody ?: call.error,
                contentType = call.responseHeaders?.contentType(),
                onCopyBody = { fullText ->
                    scope.launch {
                        val formatted = withContext(Dispatchers.Default) {
                            NetworkBodyFormatter.formatForCopy(
                                fullText,
                                call.responseHeaders?.contentType(),
                            )
                        }
                        clipboard.setText(AnnotatedString(formatted))
                    }
                },
            )
        }
    }
}

@Composable
private fun NetworkDetailPreview(call: NetworkCall) {
    ProbeFullScreenPreview {
        NetworkDetailContent(
            call = call,
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkDetailContentPreview() {
    NetworkDetailPreview(call = ProbePreviewData.getHomeLayout)
}

@ThemePreviews
@Composable
private fun NetworkDetailContentPostPreview() {
    NetworkDetailPreview(call = ProbePreviewData.postLogin)
}

@ThemePreviews
@Composable
private fun NetworkDetailContentErrorPreview() {
    NetworkDetailPreview(call = ProbePreviewData.failedCall)
}

@Composable
private fun OverviewTab(
    call: NetworkCall,
    status: NetworkStatusPresentation,
    onCopyCurl: () -> Unit,
    onShareCurl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalProbeColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NetworkMethodBadge(method = call.method)
                NetworkStatusBadge(presentation = status)
            }
            NetworkOverviewRow(label = "Status", value = call.overviewStatusLabel())
            NetworkOverviewRow(label = "Duration", value = call.durationLabel())
            NetworkOverviewRow(label = "Response size", value = call.responseSizeLabel())
            NetworkOverviewRow(
                label = "Time",
                value = formatNetworkTimestamp(call.timestampMillis),
            )
            NetworkOverviewRow(label = "URL", value = call.url)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCopyCurl) {
                Text("Copy cURL", color = colors.primary)
            }
            TextButton(onClick = onShareCurl) {
                Text("Share", color = colors.primary)
            }
        }
    }
}

private fun Map<String, String>.contentType(): String? =
    entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value

@Composable
private fun PayloadTabContent(
    headers: Map<String, String>,
    body: String?,
    contentType: String?,
    onCopyBody: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalProbeColors.current
    val subTabState = rememberProbeTabState(initialSelectedIndex = 0, style = ProbeTabBarStyle.Secondary)
    val selectedPayloadTab = PayloadTab.entries.getOrElse(subTabState.selectedIndex.intValue) {
        PayloadTab.Headers
    }
    var bodyTabOpened by remember { mutableStateOf(false) }
    if (selectedPayloadTab == PayloadTab.Body) {
        bodyTabOpened = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ProbeTabBar(
            tabItems = PayloadTab.entries.map { it.name },
            tabState = subTabState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onTabSelected = { subTabState.select(it) },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (bodyTabOpened) {
                NetworkBodyBlock(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f),
                    body = body,
                    contentType = contentType,
                    onCopy = { onCopyBody(body.orEmpty().trim()) },
                )
            }

            if (selectedPayloadTab == PayloadTab.Headers) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .verticalScroll(rememberScrollState())
                        .background(colors.background),
                ) {
                    NetworkHeadersTable(headers = headers)
                }
            }
        }
    }
}
