package com.dev.probe.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal object BodyPrettyPrinter {
    private const val MAX_PRETTY_CHARS = 32_000

    private val jsonFormatter = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun format(body: String, contentType: String? = null): String {
        if (body.isBlank()) return "(empty)"
        val trimmed = body.trim()
        if (trimmed.length > MAX_PRETTY_CHARS) return trimmed
        return when {
            isJson(contentType, trimmed) -> formatJson(trimmed)
            isXml(contentType, trimmed) -> prettyXml(trimmed)
            else -> trimmed
        }
    }

    private fun isJson(contentType: String?, body: String): Boolean =
        contentType?.contains("json", ignoreCase = true) == true ||
            body.startsWith("{") || body.startsWith("[")

    private fun isXml(contentType: String?, body: String): Boolean =
        contentType?.contains("xml", ignoreCase = true) == true ||
            body.startsWith("<")

    private fun formatJson(raw: String): String =
        try {
            val element = Json.parseToJsonElement(raw)
            jsonFormatter.encodeToString(JsonElement.serializer(), element)
        } catch (_: Exception) {
            raw
        }

    private fun prettyXml(raw: String): String =
        try {
            val lines = raw.trim()
                .replace(">\\s+<".toRegex(), "><")
                .replace("><", ">\n<")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (lines.isEmpty()) return raw

            val sb = StringBuilder()
            var depth = 0
            for (line in lines) {
                if (line.startsWith("</")) {
                    depth = (depth - 1).coerceAtLeast(0)
                }
                sb.append("  ".repeat(depth))
                sb.appendLine(line)
                if (
                    line.startsWith("<") &&
                    !line.startsWith("</") &&
                    !line.startsWith("<?") &&
                    !line.endsWith("/>")
                ) {
                    depth++
                }
            }
            sb.toString().trimEnd()
        } catch (_: Exception) {
            raw
        }
}
