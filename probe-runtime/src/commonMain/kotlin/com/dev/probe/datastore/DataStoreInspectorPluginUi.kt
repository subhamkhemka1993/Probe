package com.dev.probe.datastore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.api.ProbeDataStoreCapture
import com.dev.probe.api.RegisteredStore
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * [ProbePlugin] for the DataStore inspector: renders one live-updating row per resource
 * registered via [ProbeDataStoreCapture.register], each formatted through
 * [DataStoreSnapshotFormatter] using that registration's own redactor.
 *
 * [registrations] is injected (defaulting to the real [ProbeDataStoreCapture.registrations]) so
 * this plugin can be exercised in tests against a fake registry rather than the global singleton.
 */
internal class DataStoreInspectorPluginUi(
    private val registrations: StateFlow<Map<String, RegisteredStore>> = ProbeDataStoreCapture.registrations,
) : ProbePlugin {
    override val id = "datastore"
    override val displayName = "DataStore"
    override val description = "Inspect registered DataStore/preference snapshots"

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        val rowsFlow =
            remember {
                registrations.flatMapLatest { registered ->
                    val entries = registered.entries.toList()
                    if (entries.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(
                            entries.map { (name, store) ->
                                store.flow
                                    .map { value -> DataStoreSnapshotFormatter.format(name, value, store.redactor) }
                                    .catch { e -> emit(DataStoreRow(name, "⚠ ${e.message ?: "failed to read"}")) }
                            },
                        ) { rows -> rows.toList() }
                    }
                }
            }
        val rows by rowsFlow.collectAsState(initial = emptyList())

        Column(modifier = Modifier.fillMaxSize()) {
            if (rows.isEmpty()) {
                Text(
                    text = "No DataStore snapshots registered.",
                    style = LocalProbeTypography.current.bodyMedium,
                    color = LocalProbeColors.current.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.name }) { row ->
                        ProbeListRow(
                            onClick = {},
                            leftContent = {
                                Text(
                                    text = row.name,
                                    style = LocalProbeTypography.current.bodyMedium,
                                    color = LocalProbeColors.current.textPrimary,
                                )
                            },
                            rightContent = {
                                Text(
                                    text = row.value,
                                    style = LocalProbeTypography.current.bodySmall,
                                    color = LocalProbeColors.current.textSecondary,
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
