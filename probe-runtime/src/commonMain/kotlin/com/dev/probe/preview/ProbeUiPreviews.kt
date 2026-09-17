package com.dev.probe.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.ProbeTheme

@Preview(name = "Light", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
annotation class ThemePreviews

@Composable
internal fun ProbeFullScreenPreview(content: @Composable () -> Unit) {
    ProbeTheme {
        val colors = LocalProbeColors.current
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
internal fun ProbeBackgroundPreviewContainer(
    modifier: Modifier = Modifier,
    innerPaddingValues: PaddingValues = PaddingValues(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    ProbeTheme {
        val colors = LocalProbeColors.current
        Surface(color = colors.background, modifier = modifier) {
            Column(
                modifier = Modifier.padding(innerPaddingValues),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}

@Composable
internal fun ProbeSheetPreviewContainer(content: @Composable () -> Unit) {
    ProbeTheme {
        content()
    }
}
