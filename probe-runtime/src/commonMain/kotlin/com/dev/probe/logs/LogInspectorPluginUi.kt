package com.dev.probe.logs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow
import com.dev.probe.ui.primitives.ProbeSearchField

/**
 * [ProbePlugin] for the Log inspector: renders [ringBuffer]'s live-updating entries, filtered by
 * tag or message text.
 */
internal class LogInspectorPluginUi(private val ringBuffer: LogRingBuffer) : ProbePlugin {
    override val id = "logs"
    override val displayName = "Logs"
    override val description = "Inspect the app's own log stream"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        var query by remember { mutableStateOf("") }
        val entries by ringBuffer.entries.collectAsState()
        val filtered =
            remember(entries, query) {
                if (query.isBlank()) {
                    entries
                } else {
                    entries.filter { it.tag.contains(query, ignoreCase = true) || it.message.contains(query, ignoreCase = true) }
                }
            }

        Column(modifier = Modifier.fillMaxSize()) {
            ProbeSearchField(
                value = query,
                onValueChange = { query = it },
                onDone = {},
                placeholder = "Search tag or message…",
                modifier = Modifier.padding(8.dp),
            )
            ProbeDivider()
            if (filtered.isEmpty()) {
                Text(
                    text = if (entries.isEmpty()) "No log lines captured yet." else "No matches.",
                    style = LocalProbeTypography.current.bodyMedium,
                    color = LocalProbeColors.current.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { entry ->
                        ProbeListRow(
                            onClick = {},
                            leftContent = {
                                Text(
                                    text = entry.toDisplayText(),
                                    style = LocalProbeTypography.current.bodySmall,
                                    color = LocalProbeColors.current.textPrimary,
                                )
                            },
                        )
                        ProbeDivider()
                    }
                }
            }
        }
    }
}

/**
 * A one-line summary, not the full [Throwable.stackTraceToString] — this renders inline inside a
 * `LazyColumn` row, and a deep/long stack trace there would blow up that row's height unbounded
 * in a list meant for a quick scroll, not a full trace viewer.
 */
private fun LogEntry.toDisplayText(): String {
    val base = "[$severity] $tag: $message"
    val throwableSummary = throwable?.let { "${it::class.simpleName}: ${it.message}" }
    return if (throwableSummary == null) base else "$base — $throwableSummary"
}
