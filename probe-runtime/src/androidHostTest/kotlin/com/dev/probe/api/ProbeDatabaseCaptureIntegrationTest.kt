package com.dev.probe.api

import com.dev.probe.network.createTestProbeDatabase
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class ProbeDatabaseCaptureIntegrationTest {
    @Test
    fun registerAddsAnEntryReadableViaSnapshot() {
        val database = createTestProbeDatabase()
        ProbeDatabaseCapture.register("AppDatabase", database)

        assertTrue(ProbeDatabaseCapture.snapshot()["AppDatabase"] === database)

        ProbeDatabaseCapture.unregister("AppDatabase")
    }

    @Test
    fun registeringTwiceUnderTheSameNameReplacesThePriorEntry() {
        val first = createTestProbeDatabase()
        val second = createTestProbeDatabase()
        ProbeDatabaseCapture.register("Db", first)
        ProbeDatabaseCapture.register("Db", second)

        assertTrue(ProbeDatabaseCapture.snapshot()["Db"] === second)

        ProbeDatabaseCapture.unregister("Db")
    }

    @Test
    fun unregisterRemovesTheEntry() {
        ProbeDatabaseCapture.register("Temp", createTestProbeDatabase())
        ProbeDatabaseCapture.unregister("Temp")

        assertTrue(!ProbeDatabaseCapture.snapshot().containsKey("Temp"))
    }
}
