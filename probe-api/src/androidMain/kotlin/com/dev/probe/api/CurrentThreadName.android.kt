package com.dev.probe.api

actual fun currentThreadName(): String = Thread.currentThread().name
