package com.dev.probe.exceptions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UncaughtExceptionHookTest {
    @Test
    fun install_capturesException_thenChainsToPreviousHandler() {
        val previousCalls = mutableListOf<Throwable>()
        val previousHandler = Thread.UncaughtExceptionHandler { _, e -> previousCalls.add(e) }
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)

        val captured = mutableListOf<Pair<Throwable, String>>()
        val uninstall = installUncaughtExceptionHook { throwable, threadName -> captured.add(throwable to threadName) }

        try {
            val error = RuntimeException("boom")
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), error)

            assertEquals(1, captured.size)
            assertEquals(error, captured.first().first)
            assertTrue(previousCalls.contains(error)) // must not swallow the chain
        } finally {
            uninstall()
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }

    @Test
    fun install_whenOnUncaughtThrows_stillChainsToPreviousHandler() {
        val previousCalls = mutableListOf<Throwable>()
        val previousHandler = Thread.UncaughtExceptionHandler { _, e -> previousCalls.add(e) }
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)

        val uninstall = installUncaughtExceptionHook { _, _ -> throw IllegalStateException("capture bug") }

        try {
            val error = RuntimeException("boom")
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), error)

            assertTrue(previousCalls.contains(error)) // a bug in capture must never break the real chain
        } finally {
            uninstall()
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }
}
