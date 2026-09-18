package com.dev.probe.dbinspector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.RoomDatabase
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

private const val PAGE_SIZE = 50

private sealed interface DbInspectorScreen {
    data object Databases : DbInspectorScreen

    data class Tables(val databaseName: String) : DbInspectorScreen

    data class Rows(val databaseName: String, val table: String) : DbInspectorScreen
}

/**
 * [ProbePlugin] for the Database inspector: browses every [ProbeDatabaseCapture]-registered
 * database's tables and rows generically, via [SqliteRowReader]. Read-only in v1.
 *
 * [registrations] is injected (defaulting to the real [ProbeDatabaseCapture.registrations]) so
 * this plugin can be exercised in tests against a fake registry rather than the global singleton.
 */
internal class DatabaseInspectorPluginUi(
    private val registrations: StateFlow<Map<String, RoomDatabase>> = ProbeDatabaseCapture.registrations,
) : ProbePlugin {
    override val id = "database"
    override val displayName = "Database"
    override val description = "Browse registered Room databases (read-only)"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        var screen by remember { mutableStateOf<DbInspectorScreen>(DbInspectorScreen.Databases) }
        val databases by registrations.collectAsState()

        when (val current = screen) {
            is DbInspectorScreen.Databases ->
                ProbeTextRowList(
                    items = databases.keys.toList(),
                    emptyMessage = "No databases registered.",
                    onItemClick = { name -> screen = DbInspectorScreen.Tables(name) },
                )

            is DbInspectorScreen.Tables -> {
                val database = databases[current.databaseName]
                var tables by remember(database) { mutableStateOf<Result<List<String>>?>(null) }
                LaunchedEffect(database) {
                    tables = database?.let { readCatchingCancellation { SqliteRowReader.listTables(it) } } ?: Result.success(emptyList())
                }
                ProbeTextRowList(
                    items = tables?.getOrNull() ?: emptyList(),
                    emptyMessage = tables.toEmptyMessage(loadingLabel = "Loading…", emptyLabel = "No tables."),
                    onItemClick = { table -> screen = DbInspectorScreen.Rows(current.databaseName, table) },
                )
            }

            is DbInspectorScreen.Rows -> {
                val database = databases[current.databaseName]
                var rows by remember(database, current.table) { mutableStateOf<Result<List<Map<String, String?>>>?>(null) }
                LaunchedEffect(database, current.table) {
                    rows =
                        database?.let {
                            readCatchingCancellation { SqliteRowReader.readRows(it, current.table, limit = PAGE_SIZE, offset = 0) }
                        }
                            ?: Result.success(emptyList())
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
                        items =
                        rows?.getOrNull()?.map { row -> row.entries.joinToString(", ") { entry -> "${entry.key}=${entry.value}" } }
                            ?: emptyList(),
                        emptyMessage = rows.toEmptyMessage(loadingLabel = "Loading…", emptyLabel = "No rows."),
                        onItemClick = {},
                    )
                }
            }
        }
    }
}

/** Renders a failed [Result] as its error message, otherwise the usual loading/empty copy. */
private fun <T> Result<List<T>>?.toEmptyMessage(loadingLabel: String, emptyLabel: String): String = when {
    this == null -> loadingLabel
    isFailure -> "⚠ ${exceptionOrNull()?.message ?: "Failed to read database."}"
    else -> emptyLabel
}

/**
 * Like [runCatching] for a suspend [block], but rethrows [CancellationException] instead of
 * capturing it into a failed [Result] — swallowing it would break structured concurrency by
 * letting this coroutine keep running (and writing to state) past its own cancellation point.
 */
private suspend fun <T> readCatchingCancellation(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
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
