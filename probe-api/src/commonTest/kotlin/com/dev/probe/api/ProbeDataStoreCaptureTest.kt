package com.dev.probe.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf

class ProbeDataStoreCaptureTest {
    @Test
    fun noRegistrationsByDefault() {
        assertTrue(ProbeDataStoreCapture.snapshot().isEmpty())
    }

    @Test
    fun registerAddsAnEntryReadableViaSnapshot() {
        ProbeDataStoreCapture.register("UserPreference", flowOf("value"))

        assertTrue(ProbeDataStoreCapture.snapshot().containsKey("UserPreference"))

        ProbeDataStoreCapture.unregister("UserPreference")
    }

    @Test
    fun registeringTwiceUnderTheSameNameReplacesThePriorEntry() {
        ProbeDataStoreCapture.register("Store", flowOf("first"))
        ProbeDataStoreCapture.register("Store", flowOf("second"), redactor = { "custom" })

        val redactor = ProbeDataStoreCapture.snapshot().getValue("Store").redactor

        assertEquals("custom", redactor(null))

        ProbeDataStoreCapture.unregister("Store")
    }

    @Test
    fun unregisterRemovesTheEntry() {
        ProbeDataStoreCapture.register("Temp", flowOf(Unit))
        ProbeDataStoreCapture.unregister("Temp")

        assertTrue(!ProbeDataStoreCapture.snapshot().containsKey("Temp"))
    }

    @Test
    fun defaultRedactorIsPlainToString() {
        ProbeDataStoreCapture.register("Plain", flowOf("hello"))

        val redactor = ProbeDataStoreCapture.snapshot().getValue("Plain").redactor
        assertEquals("hello", redactor("hello"))
        assertEquals("null", redactor(null))

        ProbeDataStoreCapture.unregister("Plain")
    }
}
