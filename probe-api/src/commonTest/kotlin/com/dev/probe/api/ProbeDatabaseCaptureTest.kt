package com.dev.probe.api

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [ProbeDatabaseCapture.register] takes a real [androidx.room.RoomDatabase], and this module has
 * deliberately avoided hand-rolling a fake one for `commonTest`/`iosTest` — `RoomDatabase`'s
 * actual implementation varies per Kotlin/Native target, and its abstract surface beyond
 * `createInvalidationTracker()` isn't stable enough across targets to fake safely here (see the
 * runtime-state-capture design spec, §14). `register`/`unregister`/replace-on-same-name are
 * covered against a real database instance in `:probe-runtime`'s
 * `ProbeDatabaseCaptureIntegrationTest` (Robolectric, JVM only) instead — this class only covers
 * what's testable without a `RoomDatabase` instance.
 */
class ProbeDatabaseCaptureTest {
    @Test
    fun noRegistrationsByDefault() {
        assertTrue(ProbeDatabaseCapture.snapshot().isEmpty())
        assertTrue(ProbeDatabaseCapture.registrations.value.isEmpty())
    }
}
