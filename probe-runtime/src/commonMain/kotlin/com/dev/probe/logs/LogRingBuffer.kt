package com.dev.probe.logs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class LogEntry(
    val severity: String,
    val tag: String,
    val message: String,
    val throwable: Throwable?,
    val timestampMillis: Long,
    val id: Long = 0L,
)

/**
 * Fixed-size, oldest-evicted-first buffer of [LogEntry]. Backed by [MutableStateFlow.update],
 * which is already atomic (a CAS loop) — no additional locking needed, and safe to call from
 * whatever thread a host's logging library calls [com.dev.probe.api.ProbeLogSink.write] from.
 *
 * [add] assigns each stored entry a monotonically increasing [LogEntry.id], derived from the
 * current list on every (possibly retried) update rather than a separately-mutated counter, so
 * it stays consistent under the CAS retry loop. This gives the UI a stable per-entry identity to
 * key a `LazyColumn` on, since list indices shift on every eviction.
 */
internal class LogRingBuffer(private val capacity: Int) {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(entry: LogEntry) {
        _entries.update { current ->
            val nextId = (current.lastOrNull()?.id ?: 0L) + 1
            val appended = current + entry.copy(id = nextId)
            if (appended.size > capacity) appended.takeLast(capacity) else appended
        }
    }
}
