package com.dev.probe.export

import com.dev.probe.network.model.NetworkCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionExporterTest {
    @Test
    fun jsonExportContainsRedactedFieldsForEachCall() {
        val calls =
            listOf(
                sampleCall(id = "1", authorization = "Bearer secret-1"),
                sampleCall(id = "2", authorization = "Bearer secret-2"),
            )

        val json = SessionExporter.export(calls, ExportFormat.JSON)

        assertTrue(json.contains("\"id\":\"1\""))
        assertTrue(json.contains("\"id\":\"2\""))
        assertTrue(json.contains("\"method\":\"POST\""))
        assertFalse(json.contains("secret-1"))
        assertFalse(json.contains("secret-2"))
        assertTrue(json.contains("\"***\""))
    }

    @Test
    fun jsonExportOfEmptyListProducesEmptyArray() {
        val json = SessionExporter.export(emptyList(), ExportFormat.JSON)

        assertEquals("[]", json)
    }

    @Test
    fun harExportHasVersionAndEntriesWithRequestResponseAndTime() {
        val calls = listOf(sampleCall(id = "1", authorization = "Bearer secret-1"))

        val har = SessionExporter.export(calls, ExportFormat.HAR)

        assertTrue(har.contains("\"version\":\"1.2\""))
        assertTrue(har.contains("\"entries\""))
        assertTrue(har.contains("\"request\""))
        assertTrue(har.contains("\"response\""))
        assertTrue(har.contains("\"time\":"))
        assertTrue(har.contains("\"status\":200"))
        assertFalse(har.contains("secret-1"))
    }

    @Test
    fun harExportIncludesQueryStringAndPostData() {
        val calls =
            listOf(
                sampleCall(
                    id = "1",
                    url = "https://api.zebpay.com/v1/foo?a=1&b=2",
                    query = "a=1&b=2",
                ),
            )

        val har = SessionExporter.export(calls, ExportFormat.HAR)

        assertTrue(har.contains("\"queryString\""))
        assertTrue(har.contains("\"name\":\"a\""))
        assertTrue(har.contains("\"value\":\"1\""))
        assertTrue(har.contains("\"postData\""))
    }

    @Test
    fun curlBundleJoinsPerCallCurlCommandsWithBlankLineSeparator() {
        // Headers are already redacted upstream at capture time (NetworkDebugPlugin),
        // mirroring the real data SessionExporter receives from the repository.
        val calls =
            listOf(
                sampleCall(id = "1", authorization = "***"),
                sampleCall(id = "2", authorization = "***"),
            )

        val bundle = SessionExporter.export(calls, ExportFormat.CURL_BUNDLE)

        val commands = bundle.split("\n\n")
        assertEquals(2, commands.size)
        commands.forEach { command -> assertTrue(command.startsWith("curl -X POST")) }
        assertFalse(bundle.contains("Bearer"))
    }

    private fun sampleCall(
        id: String,
        url: String = "https://api.zebpay.com/v1/foo",
        query: String? = null,
        authorization: String = "Bearer secret",
    ) = NetworkCall(
        id = id,
        timestampMillis = 1_700_000_000_000L,
        method = "POST",
        url = url,
        host = "api.zebpay.com",
        path = "/v1/foo",
        query = query,
        requestHeaders =
            mapOf(
                "Authorization" to authorization,
                "Content-Type" to "application/json",
            ),
        requestBody = """{"email":"user@example.com"}""",
        responseStatus = 200,
        responseHeaders = mapOf("Set-Cookie" to "session=abc123"),
        responseBody = """{"ok":true}""",
        durationMs = 42L,
        error = null,
        isComplete = true,
    )
}
