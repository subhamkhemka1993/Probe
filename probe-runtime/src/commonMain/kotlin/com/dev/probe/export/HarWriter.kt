package com.dev.probe.export

import com.dev.probe.network.model.NetworkCall
import com.dev.probe.network.redactHeaders
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val harJson =
    Json {
        encodeDefaults = true
    }

internal object HarWriter {
    fun write(calls: List<NetworkCall>): String {
        val har = HarLog(log = HarLogBody(entries = calls.map { it.toHarEntry() }))
        return harJson.encodeToString(har)
    }
}

@Serializable
internal data class HarLog(
    val log: HarLogBody,
)

@Serializable
internal data class HarLogBody(
    val version: String = "1.2",
    val creator: HarCreator = HarCreator(),
    val entries: List<HarEntry>,
)

@Serializable
internal data class HarCreator(
    val name: String = "Probe",
    val version: String = "1.0",
)

@Serializable
internal data class HarEntry(
    val startedDateTime: String,
    val time: Long,
    val request: HarRequest,
    val response: HarResponse,
    val cache: HarCache = HarCache(),
    val timings: HarTimings,
)

@Serializable
internal class HarCache

@Serializable
internal data class HarTimings(
    val send: Long = 0,
    val wait: Long,
    val receive: Long = 0,
)

@Serializable
internal data class HarRequest(
    val method: String,
    val url: String,
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarHeader>,
    val queryString: List<HarQueryParam> = emptyList(),
    val postData: HarPostData? = null,
    val headersSize: Int = -1,
    val bodySize: Int = -1,
)

@Serializable
internal data class HarResponse(
    val status: Int,
    val statusText: String = "",
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarHeader>,
    val content: HarContent,
    val redirectURL: String = "",
    val headersSize: Int = -1,
    val bodySize: Int = -1,
)

@Serializable
internal data class HarHeader(
    val name: String,
    val value: String,
)

@Serializable
internal data class HarQueryParam(
    val name: String,
    val value: String,
)

@Serializable
internal data class HarPostData(
    val mimeType: String,
    val text: String,
)

@Serializable
internal data class HarContent(
    val size: Int,
    val mimeType: String = "application/json",
    val text: String? = null,
)

private fun NetworkCall.toHarEntry(): HarEntry {
    val redactedRequestHeaders = redactHeaders(requestHeaders)
    val redactedResponseHeaders = responseHeaders?.let(::redactHeaders) ?: emptyMap()
    val duration = durationMs ?: 0L
    return HarEntry(
        startedDateTime = Instant.fromEpochMilliseconds(timestampMillis).toString(),
        time = duration,
        request =
            HarRequest(
                method = method,
                url = url,
                headers = redactedRequestHeaders.toHarHeaders(),
                queryString = query.toHarQueryParams(),
                postData =
                    requestBody?.takeIf { it.isNotEmpty() }?.let { body ->
                        HarPostData(mimeType = redactedRequestHeaders.contentType(), text = body)
                    },
            ),
        response =
            HarResponse(
                status = responseStatus ?: 0,
                headers = redactedResponseHeaders.toHarHeaders(),
                content = HarContent(size = responseBody?.length ?: 0, text = responseBody),
            ),
        timings = HarTimings(wait = duration),
    )
}

private fun Map<String, String>.toHarHeaders(): List<HarHeader> = map { (name, value) -> HarHeader(name, value) }

private fun Map<String, String>.contentType(): String = entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value ?: "text/plain"

private fun String?.toHarQueryParams(): List<HarQueryParam> =
    this
        ?.split("&")
        ?.filter { it.isNotEmpty() }
        ?.map { param ->
            val separatorIndex = param.indexOf("=")
            if (separatorIndex == -1) {
                HarQueryParam(name = param, value = "")
            } else {
                HarQueryParam(name = param.substring(0, separatorIndex), value = param.substring(separatorIndex + 1))
            }
        }
        ?: emptyList()
