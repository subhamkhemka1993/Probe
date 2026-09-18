package com.dev.probe.sample

import com.dev.probe.api.ProbeHttpCapture
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createSampleHttpClient(): HttpClient = HttpClient(Darwin) {
    ProbeHttpCapture.run { installCapture() }
}
