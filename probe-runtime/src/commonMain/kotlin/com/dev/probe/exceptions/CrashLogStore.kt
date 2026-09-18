package com.dev.probe.exceptions

import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.session.DebugSessionManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

/**
 * Append-only, synchronous, file-backed store for [CrashEntry]. Deliberately not Room-backed:
 * [append] runs on the crashing thread, immediately before the automatic uncaught-exception
 * handler chains onward and the process likely dies — an async `scope.launch` Room insert has no
 * guarantee of completing in that window. A flat local file write does.
 *
 * Each line is `id|base64(sessionId)|timestampMillis|base64(threadName)|isFatal|base64(exceptionClassName)|base64(message)|base64(stackTrace)`.
 * Every free-text field is base64-encoded *independently* (not concatenated behind one shared
 * `\n`-delimited payload) — a multi-line message would otherwise misalign against a `\n`-limited
 * split and corrupt the adjacent stack-trace field. A valid base64 string never contains `|`, so a
 * plain `split("|")` on the whole line is always safe.
 */
@OptIn(ExperimentalEncodingApi::class, InternalCoroutinesApi::class)
internal class CrashLogStore(platform: ProbePlatformContext, private val capacity: Int) {
    private val fileSystem = FileSystem.SYSTEM
    private val path = crashLogFilePath(platform).toPath()

    /**
     * Guards every mutating operation below. [ProbeCrashCapture.reportCaught] is documented safe
     * to call from any thread, and the fatal path runs on whichever thread just crashed — so two
     * callers can genuinely race into [append]/[pruneToSessions]/[clear] at once. A `StateFlow`'s
     * own CAS-based `update {}` only keeps the in-memory list internally consistent; it does
     * nothing for the two threads' interleaved, non-atomic file writes underneath, and here the
     * assigned id and the appended-vs-trimmed decision both also need to see a consistent
     * snapshot. Same non-suspending, callable-from-any-thread lock [com.dev.probe.api.ProbeRuntime]
     * already uses for its own teardown.
     */
    private val lock = SynchronizedObject()
    private val _entries = MutableStateFlow(readFromDisk())
    val entries: StateFlow<List<CrashEntry>> = _entries.asStateFlow()

    /**
     * Counts on-disk lines beyond [capacity] that a plain append has left stale since the last
     * full rewrite. [append] only pays for a full rewrite once every [capacity] excess appends
     * (batched) instead of on every single one past capacity — otherwise a chatty
     * [ProbeCrashCapture.reportCaught] caller would pay an O(capacity) file rewrite on every call
     * forever once past the cap. This bounds the file to at most ~2×[capacity] lines between
     * rewrites rather than [capacity] exactly, which is the tradeoff for not rewriting every call.
     */
    private var staleExcessLines = 0

    /**
     * Synchronous by design — safe to call from any thread, including a crashing one. Below
     * [capacity] this is always a plain append. Once at capacity, appends are still cheap (plain
     * append) most of the time; only every [capacity]-th excess append pays for a full rewrite
     * that trims the file back down to exactly [capacity] lines — see [staleExcessLines].
     */
    fun append(entry: CrashEntry) = synchronized(lock) {
        val assignedId = (_entries.value.lastOrNull()?.id ?: 0L) + 1
        val stamped = entry.copy(id = assignedId)
        val appended = _entries.value + stamped
        val trimmed = if (appended.size > capacity) appended.takeLast(capacity) else appended
        runCatching {
            path.parent?.let { fileSystem.createDirectories(it) }
            when {
                trimmed.size == appended.size -> {
                    fileSystem.appendingSink(path).buffer().use { it.writeUtf8(stamped.toLine() + "\n") }
                }
                staleExcessLines >= capacity -> {
                    writeAll(trimmed)
                    staleExcessLines = 0
                }
                else -> {
                    fileSystem.appendingSink(path).buffer().use { it.writeUtf8(stamped.toLine() + "\n") }
                    staleExcessLines++
                }
            }
        }
        _entries.value = trimmed
    }

    /**
     * Re-tags any entry still stamped with [DebugSessionManager.PENDING_SESSION_ID] to
     * [realSessionId], the just-bootstrapped session. Without this, a crash captured in the tiny
     * window before the first `ensureInitialSession()` resolves would stay tagged "pending"
     * forever and [com.dev.probe.exceptions.ExceptionInspectorPluginUi] would keep showing it
     * under whatever session is CURRENT at *view* time — including many app restarts later, long
     * after it stopped being current. Retagging ties it to the one real session it actually
     * belongs to, so it ages out of "current" into "previous" and out of the two-session
     * retention window exactly like every other entry.
     */
    fun retagPending(realSessionId: String) = synchronized(lock) {
        val current = _entries.value
        val retagged = current.map {
            if (it.sessionId == DebugSessionManager.PENDING_SESSION_ID) it.copy(sessionId = realSessionId) else it
        }
        if (retagged != current) {
            runCatching { writeAll(retagged) }
            staleExcessLines = 0
            _entries.value = retagged
        }
    }

    fun pruneToSessions(keepSessionIds: Set<String>) = synchronized(lock) {
        val kept = _entries.value.filter { it.sessionId in keepSessionIds }
        runCatching { writeAll(kept) }
        staleExcessLines = 0
        _entries.value = kept
    }

    fun clear() = synchronized(lock) {
        _entries.value = emptyList()
        staleExcessLines = 0
        runCatching { fileSystem.delete(path, mustExist = false) }
    }

    private fun writeAll(entries: List<CrashEntry>) {
        val text = entries.joinToString("\n") { it.toLine() } + if (entries.isEmpty()) "" else "\n"
        fileSystem.write(path) { writeUtf8(text) }
    }

    private fun readFromDisk(): List<CrashEntry> = runCatching {
        if (!fileSystem.exists(path)) return emptyList()
        fileSystem.read(path) { readUtf8() }
            .split("\n")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toEntryOrNull() }
            .takeLast(capacity)
    }.getOrDefault(emptyList())

    private fun String.b64Encode(): String = Base64.encode(encodeToByteArray())

    private fun String.b64Decode(): String = Base64.decode(this).decodeToString()

    private fun CrashEntry.toLine(): String = listOf(
        id.toString(),
        sessionId.b64Encode(),
        timestampMillis.toString(),
        threadName.b64Encode(),
        isFatal.toString(),
        exceptionClassName.b64Encode(),
        message.orEmpty().b64Encode(),
        stackTrace.b64Encode(),
    ).joinToString("|")

    private fun String.toEntryOrNull(): CrashEntry? = runCatching {
        val parts = split("|")
        require(parts.size == 8) { "expected 8 fields, got ${parts.size}" }
        CrashEntry(
            id = parts[0].toLong(),
            sessionId = parts[1].b64Decode(),
            timestampMillis = parts[2].toLong(),
            threadName = parts[3].b64Decode(),
            isFatal = parts[4].toBoolean(),
            exceptionClassName = parts[5].b64Decode(),
            message = parts[6].b64Decode().ifEmpty { null },
            stackTrace = parts[7].b64Decode(),
        )
    }.getOrNull()
}
