package com.dev.probe.api

import platform.Foundation.NSThread

actual fun currentThreadName(): String = NSThread.currentThread.name ?: "main"
