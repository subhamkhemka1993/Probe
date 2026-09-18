package com.dev.probe.sample

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers

expect fun createSampleHttpClient(): HttpClient

/** Fires a demo request so the Network inspector has something to show. */
suspend fun HttpClient.fireSampleRequest() {
    get("https://httpbin.org/get") {
        headers { append("X-Probe-Sample", "true") }
    }
}
