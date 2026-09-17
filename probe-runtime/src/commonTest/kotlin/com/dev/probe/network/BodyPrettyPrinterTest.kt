package com.dev.probe.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodyPrettyPrinterTest {

    @Test
    fun prettyPrintJson_indents() {
        val input = """{"a":1,"b":[2,3]}"""
        val out = BodyPrettyPrinter.format(input, contentType = "application/json")
        assertTrue(out.contains("\n"))
        assertTrue(out.contains("  "))
    }

    @Test
    fun prettyPrintXml_indents() {
        val input = "<root><item>v</item></root>"
        val out = BodyPrettyPrinter.format(input, contentType = "application/xml")
        assertTrue(out.contains("\n"))
    }

    @Test
    fun plainText_unchanged() {
        val input = "hello world"
        assertEquals(input, BodyPrettyPrinter.format(input, contentType = "text/plain"))
    }
}
