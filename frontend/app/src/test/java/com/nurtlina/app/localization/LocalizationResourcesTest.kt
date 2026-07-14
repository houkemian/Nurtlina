package com.nurtlina.app.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourcesTest {

    @Test
    fun `simplified Chinese covers every translatable default string`() {
        val defaults = readStrings("values")
        val chinese = readStrings("values-zh-rCN")
        val missing = defaults
            .filterValues { it.translatable }
            .keys - chinese.keys

        assertTrue("Missing Simplified Chinese strings: ${missing.sorted()}", missing.isEmpty())
    }

    @Test
    fun `simplified Chinese preserves format arguments`() {
        val defaults = readStrings("values")
        val chinese = readStrings("values-zh-rCN")
        val mismatches = defaults.keys.intersect(chinese.keys).filter { key ->
            formatArguments(defaults.getValue(key).value) !=
                formatArguments(chinese.getValue(key).value)
        }

        assertEquals("Format argument mismatches: $mismatches", emptyList<String>(), mismatches)
    }

    private fun readStrings(valuesDirectory: String): Map<String, StringEntry> {
        val file = File("src/main/res/$valuesDirectory/strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                put(
                    element.getAttribute("name"),
                    StringEntry(
                        value = element.textContent,
                        translatable = element.getAttribute("translatable") != "false",
                    ),
                )
            }
        }
    }

    private fun formatArguments(value: String): List<String> =
        FORMAT_ARGUMENT.findAll(value).map { it.value }.sorted().toList()

    private data class StringEntry(
        val value: String,
        val translatable: Boolean,
    )

    private companion object {
        val FORMAT_ARGUMENT = Regex("%(?:\\d+\\$)?(?:\\.\\d+)?[a-zA-Z]")
    }
}
