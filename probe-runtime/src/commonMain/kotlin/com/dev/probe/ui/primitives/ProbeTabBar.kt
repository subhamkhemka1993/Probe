package com.dev.probe.ui.primitives

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold

internal enum class ProbeTabBarStyle {
    Secondary,
    Tertiary,
}

@Stable
internal class ProbeTabState(val selectedIndex: MutableIntState, val style: ProbeTabBarStyle) {
    fun isSelected(index: Int): Boolean = selectedIndex.intValue == index

    fun select(index: Int) {
        selectedIndex.intValue = index
    }
}

@Composable
internal fun rememberProbeTabState(initialSelectedIndex: Int = 0, style: ProbeTabBarStyle = ProbeTabBarStyle.Tertiary): ProbeTabState =
    remember {
        ProbeTabState(
            selectedIndex = mutableIntStateOf(initialSelectedIndex),
            style = style,
        )
    }

@Composable
internal fun ProbeTabBar(tabItems: List<String>, tabState: ProbeTabState, modifier: Modifier = Modifier, onTabSelected: (Int) -> Unit) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current

    Box(modifier = modifier.fillMaxWidth()) {
        if (tabState.style == ProbeTabBarStyle.Tertiary) {
            HorizontalDivider(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = colors.divider,
            )
        }

        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                    when (tabState.style) {
                        ProbeTabBarStyle.Tertiary -> 16.dp
                        ProbeTabBarStyle.Secondary -> 8.dp
                    },
                ),
            horizontalArrangement =
            when (tabState.style) {
                ProbeTabBarStyle.Tertiary -> Arrangement.spacedBy(16.dp)
                ProbeTabBarStyle.Secondary -> Arrangement.spacedBy(8.dp)
            },
        ) {
            tabItems.forEachIndexed { index, label ->
                val selected = tabState.isSelected(index)
                val tabModifier =
                    when (tabState.style) {
                        ProbeTabBarStyle.Tertiary ->
                            Modifier
                                .clickable {
                                    tabState.select(index)
                                    onTabSelected(index)
                                }

                        ProbeTabBarStyle.Secondary ->
                            Modifier
                                .clickable {
                                    tabState.select(index)
                                    onTabSelected(index)
                                }.background(
                                    color = if (selected) colors.surface else colors.background,
                                    shape = RoundedCornerShape(8.dp),
                                ).padding(horizontal = 8.dp, vertical = 4.dp)
                    }

                Column(
                    modifier = tabModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = label,
                        style = typography.labelLarge.semiBold(),
                        color = if (selected) colors.primary else colors.textSecondary,
                    )

                    AnimatedVisibility(
                        tabState.style == ProbeTabBarStyle.Tertiary && selected,
                        modifier = Modifier.padding(top = 2.dp),
                        enter = scaleIn(tween(500)),
                        exit = scaleOut(tween(500)),
                    ) {
                        Spacer(
                            modifier =
                            Modifier
                                .size(20.dp, 2.dp)
                                .background(colors.primary),
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ProbeTabBarTertiaryPreview() {
    ProbeBackgroundPreviewContainer {
        val tabState =
            rememberProbeTabState(initialSelectedIndex = 0, style = ProbeTabBarStyle.Tertiary)
        ProbeTabBar(
            tabItems = listOf("Overview", "Request", "Response"),
            tabState = tabState,
            onTabSelected = {},
        )
    }
}

@ThemePreviews
@Composable
private fun ProbeTabBarSecondaryPreview() {
    ProbeBackgroundPreviewContainer {
        val tabState =
            rememberProbeTabState(initialSelectedIndex = 1, style = ProbeTabBarStyle.Secondary)
        ProbeTabBar(
            tabItems = listOf("All", "Errors", "Pending"),
            tabState = tabState,
            onTabSelected = {},
        )
    }
}
