package com.dev.probe.browser

import com.dev.probe.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal actual object BrowserAssetLoader {
    actual fun readText(relativePath: String): String = runBlocking { Res.readBytes(relativePath).decodeToString() }
}
