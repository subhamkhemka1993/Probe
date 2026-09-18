package com.dev.probe.api

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf

class ProbeDataStoreCaptureTest {
    /**
     * Cleans up every name any test in this class registers, unconditionally — unregistering an
     * absent name is a no-op. Runs even if a test fails an assertion partway through, unlike a
     * manual unregister() as the test's last line (which a failed assertion above it would skip,
     * leaking that registration into every later test in this JVM).
     */
    @AfterTest
    fun tearDown() {
        ProbeDataStoreCapture.unregister("UserPreference")
        ProbeDataStoreCapture.unregister("Store")
        ProbeDataStoreCapture.unregister("Temp")
        ProbeDataStoreCapture.unregister("Plain")
    }

    @Test
    fun noRegistrationsByDefault() {
        assertTrue(ProbeDataStoreCapture.snapshot().isEmpty())
    }

    @Test
    fun registerAddsAnEntryReadableViaSnapshot() {
        ProbeDataStoreCapture.register("UserPreference", flowOf("value"))

        assertTrue(ProbeDataStoreCapture.snapshot().containsKey("UserPreference"))
    }

    @Test
    fun registeringTwiceUnderTheSameNameReplacesThePriorEntry() {
        ProbeDataStoreCapture.register("Store", flowOf("first"))
        ProbeDataStoreCapture.register("Store", flowOf("second"), redactor = { "custom" })

        val redactor = ProbeDataStoreCapture.snapshot().getValue("Store").redactor

        assertEquals("custom", redactor(null))
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
    }

    @Test
    fun registrationsStateFlowReflectsRegisterAndUnregister() {
        assertTrue(ProbeDataStoreCapture.registrations.value.isEmpty())

        ProbeDataStoreCapture.register("UserPreference", flowOf("value"))
        assertTrue(ProbeDataStoreCapture.registrations.value.containsKey("UserPreference"))

        ProbeDataStoreCapture.unregister("UserPreference")
        assertTrue(!ProbeDataStoreCapture.registrations.value.containsKey("UserPreference"))
    }
}
