package com.dev.probe.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ProbePreviewData
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.medium
import com.dev.probe.theme.semiBold
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeIconButton
import com.dev.probe.ui.primitives.ProbeStatusBadge
import com.dev.probe.ui.primitives.toBadgeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HeaderKeyWidth = 120.dp
private val CardShape = RoundedCornerShape(12.dp)
private val SmallShape = RoundedCornerShape(8.dp)
private val ExtraSmallShape = RoundedCornerShape(4.dp)

@Composable
internal fun NetworkStatusTone.toAccentColor(): Color = toBadgeColor()

@Composable
private fun networkMethodColor(method: String): Color {
    val colors = LocalProbeColors.current
    return when (method.uppercase()) {
        "GET" -> colors.methodGet
        "POST" -> colors.methodPost
        "PUT", "PATCH" -> colors.methodPut
        "DELETE" -> colors.methodDelete
        else -> colors.textSecondary
    }
}

@Composable
internal fun NetworkMethodBadge(method: String, modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    Box(
        modifier =
        modifier
            .clip(ExtraSmallShape)
            .background(colors.codeBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = method.uppercase(),
            style = typography.labelMedium.semiBold(),
            color = networkMethodColor(method),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkMethodBadgePreview() {
    ProbeBackgroundPreviewContainer(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NetworkMethodBadge("GET")
            NetworkMethodBadge("POST")
            NetworkMethodBadge("DELETE")
        }
    }
}

@Composable
internal fun NetworkStatusBadge(presentation: NetworkStatusPresentation, modifier: Modifier = Modifier) {
    ProbeStatusBadge(
        text = presentation.label,
        tone = presentation.tone,
        modifier = modifier,
    )
}

@ThemePreviews
@Composable
private fun NetworkStatusBadgePreview() {
    ProbeBackgroundPreviewContainer(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NetworkStatusBadge(ProbePreviewData.getHomeLayout.statusPresentation())
        NetworkStatusBadge(ProbePreviewData.getNotFound.statusPresentation())
        NetworkStatusBadge(ProbePreviewData.getServerError.statusPresentation())
        NetworkStatusBadge(ProbePreviewData.pendingCall.statusPresentation())
    }
}

@Composable
internal fun NetworkStatusStripe(tone: NetworkStatusTone, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(tone.toAccentColor()),
    )
}

@Composable
internal fun NetworkHeadersTable(headers: Map<String, String>, modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current

    if (headers.isEmpty()) {
        NetworkEmptyState(message = "No headers")
        return
    }

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(SmallShape)
            .background(colors.surface),
    ) {
        headers.entries
            .sortedBy { it.key.lowercase() }
            .forEachIndexed { index, (key, value) ->
                if (index > 0) {
                    ProbeDivider()
                }
                SelectionContainer {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (index % 2 == 0) colors.surface else colors.background,
                            ).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            modifier = Modifier.width(HeaderKeyWidth),
                            text = key,
                            style = typography.labelMedium.semiBold(),
                            color = colors.textSecondary,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = value,
                            style = typography.labelMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
    }
}

@ThemePreviews
@Composable
private fun NetworkHeadersTablePreview() {
    ProbeBackgroundPreviewContainer {
        NetworkHeadersTable(headers = ProbePreviewData.sampleHeaders)
    }
}

@ThemePreviews
@Composable
private fun NetworkHeadersTableEmptyPreview() {
    ProbeBackgroundPreviewContainer {
        NetworkHeadersTable(headers = emptyMap())
    }
}

@Composable
internal fun NetworkBodyBlock(body: String?, modifier: Modifier = Modifier, contentType: String? = null, onCopy: (() -> Unit)? = null) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    var formattedBody by remember(body, contentType) {
        mutableStateOf(placeholderFormattedBody(body))
    }
    LaunchedEffect(body, contentType) {
        formattedBody =
            withContext(Dispatchers.Default) {
                NetworkBodyFormatter.prepare(body, contentType)
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (onCopy != null) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                ProbeIconButton(
                    onClick = onCopy,
                    icon = Icons.Filled.ContentCopy,
                    tint = colors.textPrimary,
                )
            }
        }

        SelectionContainer {
            Text(
                modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .clip(SmallShape)
                    .background(colors.codeBackground)
                    .padding(12.dp),
                text = formattedBody.displayText,
                style = typography.code,
                color = colors.textPrimary,
            )
        }
    }
}

private fun placeholderFormattedBody(body: String?): FormattedBody = if (body.isNullOrBlank()) {
    NetworkBodyFormatter.empty()
} else {
    FormattedBody(displayText = "…", fullText = body.trim())
}

@ThemePreviews
@Composable
private fun NetworkBodyBlockPreview() {
    ProbeBackgroundPreviewContainer(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        NetworkBodyBlock(
            body = ProbePreviewData.minifiedJsonBody,
            contentType = "application/json",
            onCopy = {},
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkBodyBlockRequestPreview() {
    ProbeBackgroundPreviewContainer(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        NetworkBodyBlock(
            body = ProbePreviewData.postLogin.requestBody,
            contentType = "application/json",
            onCopy = {},
        )
    }
}

@Composable
internal fun NetworkOverviewRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            modifier = Modifier.width(88.dp),
            text = label,
            style = typography.bodySmall.medium(),
            color = colors.textSecondary,
        )
        Box(modifier = Modifier.weight(1f)) {
            SelectionContainer {
                Text(
                    text = value,
                    style = typography.bodySmall,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun NetworkOverviewRowPreview() {
    ProbeBackgroundPreviewContainer {
        NetworkOverviewRow(
            label = "URL",
            value = ProbePreviewData.getHomeLayout.url,
        )
        NetworkOverviewRow(label = "Duration", value = "193ms")
    }
}

@Composable
internal fun NetworkEmptyState(message: String, modifier: Modifier = Modifier) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@ThemePreviews
@Composable
private fun NetworkEmptyStatePreview() {
    ProbeBackgroundPreviewContainer {
        NetworkEmptyState(message = "No requests captured yet")
    }
}

@Composable
internal fun NetworkCardContainer(tone: NetworkStatusTone, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalProbeColors.current
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CardShape)
            .background(colors.surface),
    ) {
        NetworkStatusStripe(tone = tone)
        Column(
            modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = { content() },
        )
    }
}
