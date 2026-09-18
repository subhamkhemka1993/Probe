package com.dev.probe.exceptions

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.session.DebugSessionManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLogStoreTest {
    private val platform by lazy {
        ProbePlatformContext(ApplicationProvider.getApplicationContext<Application>())
    }

    private fun newEntry(sessionId: String = "session-1", id: Long = 0L) = CrashEntry(
        id = id,
        sessionId = sessionId,
        timestampMillis = 1_000L,
        threadName = "main",
        isFatal = true,
        exceptionClassName = "java.lang.RuntimeException",
        message = "boom",
        stackTrace = "java.lang.RuntimeException: boom\n\tat Foo.bar(Foo.kt:1)",
    )

    @Test
    fun append_thenReadBackFromFreshStore_returnsTheEntry() {
        CrashLogStore(platform, capacity = 50).append(newEntry())

        // A fresh instance simulates the next process reading what the crashing process wrote.
        val reloaded = CrashLogStore(platform, capacity = 50)
        val entries = reloaded.entries.value

        assertEquals(1, entries.size)
        assertEquals("boom", entries.first().message)
        assertEquals("java.lang.RuntimeException", entries.first().exceptionClassName)
    }

    @Test
    fun pruneToSessions_removesEntriesForDroppedSessions() {
        val store = CrashLogStore(platform, capacity = 50)
        store.append(newEntry(sessionId = "session-old"))
        store.append(newEntry(sessionId = "session-new"))

        store.pruneToSessions(keepSessionIds = setOf("session-new"))

        assertTrue(store.entries.value.all { it.sessionId == "session-new" })
        assertEquals(1, store.entries.value.size)
    }

    @Test
    fun clear_removesEverything() {
        val store = CrashLogStore(platform, capacity = 50)
        store.append(newEntry())
        store.clear()

        assertEquals(emptyList(), store.entries.value)
    }

    @Test
    fun append_withMultilineMessage_doesNotCorruptStackTrace() {
        val entry = newEntry().copy(
            message = "Validation failed:\nfield 'x' is required\nfield 'y' is required",
            stackTrace = "java.lang.RuntimeException: boom\n\tat Foo.bar(Foo.kt:1)\n\tat Foo.baz(Foo.kt:2)",
        )
        CrashLogStore(platform, capacity = 50).append(entry)

        val reloaded = CrashLogStore(platform, capacity = 50).entries.value.first()

        assertEquals(entry.message, reloaded.message)
        assertEquals(entry.stackTrace, reloaded.stackTrace)
    }

    @Test
    fun append_pastCapacity_trimsOnDiskFileToo() {
        // Regression: append() must not let the on-disk file grow past `capacity` just because
        // the in-memory view is capped — otherwise a long session with many reportCaught() calls
        // grows the file without bound. Reloading with a *larger* capacity than the writer used
        // proves what actually landed on disk, since a same-or-smaller-capacity reload would trim
        // on read regardless of whether the writer itself ever trimmed the file.
        val store = CrashLogStore(platform, capacity = 2)
        repeat(5) { store.append(newEntry(id = it.toLong())) }

        val reloaded = CrashLogStore(platform, capacity = 100)

        assertEquals(2, reloaded.entries.value.size)
    }

    /**
     * `append()` amortizes the full-file rewrite over `capacity` excess appends instead of doing
     * one on every single call past capacity (a real cost otherwise, since a chatty
     * `reportCaught()` caller would pay O(capacity) I/O on every call forever). This proves the
     * amortization still bounds the file — it just allows up to ~2x capacity between rewrites
     * instead of trimming immediately every time. The repeat count here is `capacity` initial
     * fills + `capacity` excess appends: one short of the `2*capacity+1` total that triggers the
     * batched rewrite (see [append_pastCapacity_amortizedRewriteEventuallyTrimsExactly]), so this
     * is deliberately mid-batch.
     */
    @Test
    fun append_pastCapacity_boundsDiskToAtMostTwiceCapacityMidBatch() {
        val capacity = 3
        val store = CrashLogStore(platform, capacity = capacity)
        repeat(2 * capacity) { store.append(newEntry(id = it.toLong())) }

        val reloaded = CrashLogStore(platform, capacity = 1_000)
        assertTrue(reloaded.entries.value.size > capacity, "sanity: this point is mid-batch, disk hasn't been trimmed yet")
        assertTrue(reloaded.entries.value.size <= 2 * capacity, "disk must never exceed ~2x capacity even mid-batch")
    }

    /**
     * The repeat count here is `capacity` initial fills + `(capacity + 1)` excess appends: the
     * `staleExcessLines` check runs *before* incrementing, using the pre-increment count, so it
     * takes `capacity + 1` excess appends (not `capacity`) to see `staleExcessLines >= capacity`
     * and trigger the rewrite.
     */
    @Test
    fun append_pastCapacity_amortizedRewriteEventuallyTrimsExactly() {
        val capacity = 3
        val store = CrashLogStore(platform, capacity = capacity)
        repeat(2 * capacity + 1) { store.append(newEntry(id = it.toLong())) }

        val reloaded = CrashLogStore(platform, capacity = 1_000)
        assertEquals(capacity, reloaded.entries.value.size)
    }

    @Test
    fun retagPending_rewritesPendingEntriesToRealSessionId() {
        // Regression: without retagging, a crash captured before the first ensureInitialSession()
        // resolves stays tagged "pending" forever and ExceptionInspectorPluginUi would keep
        // showing it under whatever session is current at *view* time, including many app
        // restarts later. Retagging ties it to the one real session it actually happened in.
        val store = CrashLogStore(platform, capacity = 50)
        store.append(newEntry(sessionId = DebugSessionManager.PENDING_SESSION_ID))
        store.append(newEntry(sessionId = "session-real"))

        store.retagPending("session-1")

        assertEquals(setOf("session-1", "session-real"), store.entries.value.map { it.sessionId }.toSet())
        val reloaded = CrashLogStore(platform, capacity = 50)
        assertEquals(setOf("session-1", "session-real"), reloaded.entries.value.map { it.sessionId }.toSet())
    }

    @Test
    fun retagPending_withNoPendingEntries_isNoOp() {
        val store = CrashLogStore(platform, capacity = 50)
        store.append(newEntry(sessionId = "session-real"))

        store.retagPending("session-1")

        assertEquals(listOf("session-real"), store.entries.value.map { it.sessionId })
    }

    @Test
    fun append_fromConcurrentThreads_dropsNoEntriesAndAssignsUniqueIds() {
        // Regression: reportCaught() is documented callable from any thread, and the fatal path
        // runs on the crashing thread itself, so append() must be safe against two real threads
        // calling it at once — not just "atomic StateFlow.update", since the assigned id, the
        // appended-vs-trimmed decision, and the file write all need one consistent view.
        val store = CrashLogStore(platform, capacity = 1_000)
        val threadCount = 8
        val perThread = 25
        val pool = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)

        repeat(threadCount) { threadIndex ->
            pool.submit {
                ready.countDown()
                start.await()
                repeat(perThread) { store.append(newEntry(sessionId = "t$threadIndex")) }
            }
        }
        ready.await()
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))

        val entries = store.entries.value
        assertEquals(threadCount * perThread, entries.size)
        assertEquals(entries.size, entries.map { it.id }.toSet().size, "no two entries share an id")

        val reloaded = CrashLogStore(platform, capacity = 1_000)
        assertEquals(entries.size, reloaded.entries.value.size, "every entry reached disk")
    }
}
