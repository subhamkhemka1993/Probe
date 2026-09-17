@file:OptIn(ExperimentalMaterial3Api::class)

package com.dev.probe.ui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeSheetPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold

@Composable
internal fun ProbeBottomSheet(
    isVisible: MutableState<Boolean>,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    allowDismiss: Boolean = true,
    allowBack: Boolean = false,
    onBackPress: (() -> Unit)? = null,
    headerIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!isVisible.value) return

    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { sheetValue ->
                if (!allowDismiss) sheetValue != SheetValue.Hidden else true
            },
        )

    ModalBottomSheet(
        onDismissRequest = {
            isVisible.value = false
            onDismissRequest()
        },
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.background,
        dragHandle = null,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (allowBack && onBackPress != null) {
                    ProbeIconButton(
                        onClick = onBackPress,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = colors.textPrimary,
                        contentDescription = "Back",
                    )
                }
                if (headerIcon != null) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .padding(8.dp),
                    )
                }
                Text(
                    text = title,
                    style = typography.titleMedium.semiBold(),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            ProbeDivider()

            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                content = content,
            )

            if (footer != null) {
                ProbeDivider()
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = footer,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ProbeBottomSheetPreview() {
    val visible = remember { mutableStateOf(true) }
    ProbeSheetPreviewContainer {
        ProbeBottomSheet(
            isVisible = visible,
            title = "Network Output",
            onDismissRequest = {},
            allowBack = true,
            onBackPress = {},
            headerIcon = Icons.Filled.SwapHoriz,
            footer = {
                ProbePrimaryButton(
                    title = "Open Inspector",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            ProbeRadioButton(
                onChecked = {},
                checked = true,
                content = {
                    Text(
                        text = "In-app inspector",
                        style = LocalProbeTypography.current.bodyMedium.semiBold(),
                        color = LocalProbeColors.current.textPrimary,
                    )
                },
            )
        }
    }
}
