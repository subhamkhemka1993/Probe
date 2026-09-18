package com.dev.probe.sample

import com.dev.probe.api.ProbeHttpCapture
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createSampleHttpClient(): HttpClient = HttpClient(CIO) {
    ProbeHttpCapture.run { installCapture() }
}
