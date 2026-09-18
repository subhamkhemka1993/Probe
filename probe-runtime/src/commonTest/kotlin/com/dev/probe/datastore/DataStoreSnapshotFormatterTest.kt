package com.dev.probe.datastore

import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSnapshotFormatterTest {
    @Test
    fun defaultRedactorFallsBackToToString() {
        val row = DataStoreSnapshotFormatter.format("Counter", 42, redactor = { it.toString() })

        assertEquals("Counter", row.name)
        assertEquals("42", row.value)
    }

    @Test
    fun customRedactorOverridesDefaultRendering() {
        val row = DataStoreSnapshotFormatter.format("Secret", "super-secret-token", redactor = { "***redacted***" })

        assertEquals("***redacted***", row.value)
    }

    @Test
    fun nullValueIsFormattedByTheRedactor() {
        val row = DataStoreSnapshotFormatter.format("Empty", null, redactor = { value -> value?.toString() ?: "∅" })

        assertEquals("∅", row.value)
    }
}
