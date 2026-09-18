package com.dev.probe.dbinspector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow

private const val PAGE_SIZE = 50

private sealed interface DbInspectorScreen {
    data object Databases : DbInspectorScreen

    data class Tables(val databaseName: String) : DbInspectorScreen

    data class Rows(val databaseName: String, val table: String) : DbInspectorScreen
}

/**
 * [ProbePlugin] for the Database inspector: browses every [ProbeDatabaseCapture]-registered
 * database's tables and rows generically, via [SqliteRowReader]. Read-only in v1.
 */
internal class DatabaseInspectorPluginUi : ProbePlugin {
    override val id = "database"
    override val displayName = "Database"
    override val description = "Browse registered Room databases (read-only)"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        var screen by remember { mutableStateOf<DbInspectorScreen>(DbInspectorScreen.Databases) }
        val databases = remember { ProbeDatabaseCapture.snapshot() }

        when (val current = screen) {
            is DbInspectorScreen.Databases ->
                ProbeTextRowList(
                    items = databases.keys.toList(),
                    emptyMessage = "No databases registered.",
                    onItemClick = { name -> screen = DbInspectorScreen.Tables(name) },
                )

            is DbInspectorScreen.Tables -> {
                var tables by remember(current.databaseName) { mutableStateOf<List<String>?>(null) }
                val database = databases[current.databaseName]
                LaunchedEffect(current.databaseName) {
                    tables = database?.let { SqliteRowReader.listTables(it) } ?: emptyList()
                }
                ProbeTextRowList(
                    items = tables ?: emptyList(),
                    emptyMessage = if (tables == null) "Loading…" else "No tables.",
                    onItemClick = { table -> screen = DbInspectorScreen.Rows(current.databaseName, table) },
                )
            }

            is DbInspectorScreen.Rows -> {
                var rows by remember(current.databaseName, current.table) { mutableStateOf<List<Map<String, String?>>?>(null) }
                val database = databases[current.databaseName]
                LaunchedEffect(current.databaseName, current.table) {
                    rows = database?.let { SqliteRowReader.readRows(it, current.table, limit = PAGE_SIZE, offset = 0) } ?: emptyList()
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = current.table,
                        style = LocalProbeTypography.current.bodyMedium,
                        color = LocalProbeColors.current.textPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                    ProbeDivider()
                    ProbeTextRowList(
                        items = rows?.map { it.entries.joinToString(", ") { entry -> "${entry.key}=${entry.value}" } } ?: emptyList(),
                        emptyMessage = if (rows == null) "Loading…" else "No rows.",
                        onItemClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun ProbeTextRowList(items: List<String>, emptyMessage: String, onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Text(
                text = emptyMessage,
                style = LocalProbeTypography.current.bodyMedium,
                color = LocalProbeColors.current.textSecondary,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { item ->
                    ProbeListRow(
                        onClick = { onItemClick(item) },
                        leftContent = {
                            Text(
                                text = item,
                                style = LocalProbeTypography.current.bodyMedium,
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
