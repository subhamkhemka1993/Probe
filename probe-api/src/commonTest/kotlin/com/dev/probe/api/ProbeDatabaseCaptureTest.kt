package com.dev.probe.api

import kotlin.test.Test
import kotlin.test.assertTrue

class ProbeDatabaseCaptureTest {
    @Test
    fun noRegistrationsByDefault() {
        assertTrue(ProbeDatabaseCapture.snapshot().isEmpty())
    }
}
