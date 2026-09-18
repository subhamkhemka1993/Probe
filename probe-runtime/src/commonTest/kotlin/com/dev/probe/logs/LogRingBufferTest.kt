package com.dev.probe.logs

import kotlin.test.Test
import kotlin.test.assertEquals

private fun entry(message: String) = LogEntry(
    severity = "INFO",
    tag = "tag",
    message = message,
    throwable = null,
    timestampMillis = 0L,
)

class LogRingBufferTest {
    @Test
    fun startsEmpty() {
        val buffer = LogRingBuffer(capacity = 3)

        assertEquals(emptyList(), buffer.entries.value)
    }

    @Test
    fun addAppendsInOrder() {
        val buffer = LogRingBuffer(capacity = 3)

        buffer.add(entry("a"))
        buffer.add(entry("b"))

        assertEquals(listOf("a", "b"), buffer.entries.value.map { it.message })
    }

    @Test
    fun exceedingCapacityEvictsTheOldestEntryFirst() {
        val buffer = LogRingBuffer(capacity = 2)

        buffer.add(entry("a"))
        buffer.add(entry("b"))
        buffer.add(entry("c"))

        assertEquals(listOf("b", "c"), buffer.entries.value.map { it.message })
    }
}
