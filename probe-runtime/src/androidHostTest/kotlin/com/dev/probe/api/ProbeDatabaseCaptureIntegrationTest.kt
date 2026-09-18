package com.dev.probe.api

import com.dev.probe.network.createTestProbeDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class ProbeDatabaseCaptureIntegrationTest {
    /**
     * Cleans up every name any test in this class registers, unconditionally — unregistering an
     * absent name is a no-op. Runs even if a test fails an assertion partway through, unlike a
     * manual unregister() as the test's last line (which a failed assertion above it would skip,
     * leaking that registration into every later test in this JVM).
     */
    @AfterTest
    fun tearDown() {
        ProbeDatabaseCapture.unregister("AppDatabase")
        ProbeDatabaseCapture.unregister("Db")
        ProbeDatabaseCapture.unregister("Temp")
    }

    @Test
    fun registerAddsAnEntryReadableViaSnapshot() {
        val database = createTestProbeDatabase()
        ProbeDatabaseCapture.register("AppDatabase", database)

        assertTrue(ProbeDatabaseCapture.snapshot()["AppDatabase"] === database)
    }

    @Test
    fun registeringTwiceUnderTheSameNameReplacesThePriorEntry() {
        val first = createTestProbeDatabase()
        val second = createTestProbeDatabase()
        ProbeDatabaseCapture.register("Db", first)
        ProbeDatabaseCapture.register("Db", second)

        assertTrue(ProbeDatabaseCapture.snapshot()["Db"] === second)
    }

    @Test
    fun unregisterRemovesTheEntry() {
        ProbeDatabaseCapture.register("Temp", createTestProbeDatabase())
        ProbeDatabaseCapture.unregister("Temp")

        assertTrue(!ProbeDatabaseCapture.snapshot().containsKey("Temp"))
    }

    @Test
    fun registrationsStateFlowReflectsRegisterAndUnregister() {
        assertTrue(!ProbeDatabaseCapture.registrations.value.containsKey("AppDatabase"))

        val database = createTestProbeDatabase()
        ProbeDatabaseCapture.register("AppDatabase", database)
        assertTrue(ProbeDatabaseCapture.registrations.value["AppDatabase"] === database)

        ProbeDatabaseCapture.unregister("AppDatabase")
        assertTrue(!ProbeDatabaseCapture.registrations.value.containsKey("AppDatabase"))
    }
}
