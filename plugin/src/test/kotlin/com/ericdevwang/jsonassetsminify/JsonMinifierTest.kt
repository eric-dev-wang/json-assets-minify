package com.ericdevwang.jsonassetsminify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonMinifierTest {
    private val minifier = JsonMinifier()

    @Test
    fun `isValidJson with valid  well formatted JSON`() {
        // Pass a standard, pretty-printed JSON string and assert that the function returns true.
        val prettyJson = """
            {
              "name": "John",
              "age": 30,
              "city": "New York"
            }
        """.trimIndent()
        assertTrue(minifier.isValidJson(prettyJson))
    }

    @Test
    fun `isValidJson with valid  minified JSON`() {
        // Pass a JSON string with no extra whitespace and assert that the function returns true.
        val minifiedJson = """{"name":"John","age":30,"city":"New York"}"""
        assertTrue(minifier.isValidJson(minifiedJson))
    }

    @Test
    fun `isValidJson with malformed JSON  missing brace `() {
        // Pass a JSON string with a missing closing brace '}' and assert that the function returns false.
        val malformedJson = """{"name":"John","age":30"""
        assertFalse(minifier.isValidJson(malformedJson))
    }

    @Test
    fun `isValidJson with malformed JSON  extra comma `() {
        // Pass a JSON string with a trailing comma in an object or array and assert it returns false, as isLenient is false.
        val extraCommaJson = """{"name":"John","age":30,}"""
        assertFalse(minifier.isValidJson(extraCommaJson))
    }

    @Test
    fun `isValidJson with an empty string`() {
        // Pass an empty string "" and assert that the function returns false due to the isBlank() check.
        assertFalse(minifier.isValidJson(""))
    }

    @Test
    fun `isValidJson with a blank string  spaces tabs `() {
        // Pass a string containing only whitespace characters (e.g., "  \t\n") and assert it returns false.
        assertFalse(minifier.isValidJson("  \t\n  "))
    }

    @Test
    fun `isValidJson with a non JSON string`() {
        // Pass a simple text string like "hello world" and assert that the function returns false.
        assertFalse(minifier.isValidJson("hello world"))
    }

    @Test
    fun `isValidJson with just a JSON primitive value`() {
        // Pass a string containing only a valid JSON primitive (e.g., "123", "true", "\"string\"") and assert it returns true.
        assertTrue(minifier.isValidJson("123"))
        assertTrue(minifier.isValidJson("true"))
        assertTrue(minifier.isValidJson("\"string\""))
        assertTrue(minifier.isValidJson("null"))
    }

    @Test
    fun `isValidJson with a very large JSON string`() {
        // Test with a very large and deeply nested, but valid, JSON string to check for performance issues or stack overflows, asserting true.
        val largeJson = buildString {
            append("{\"root\":")
            repeat(100) { append("{\"nested\":") }
            append("\"deep\"")
            repeat(100) { append("}") }
            append("}")
        }
        assertTrue(minifier.isValidJson(largeJson))
    }

    @Test
    fun `minify with valid  pretty printed JSON`() {
        // Provide a well-formatted JSON with lots of whitespace and assert the output is a compact, single-line string with identical data.
        val prettyJson = """
            {
              "name": "John",
              "age": 30,
              "active": true
            }
        """.trimIndent()
        val minified = minifier.minify(prettyJson)
        assertEquals("""{"name":"John","age":30,"active":true}""", minified)
    }

    @Test
    fun `minify with already minified JSON`() {
        // Provide an already minified JSON string and assert that the output is identical to the input.
        val minifiedJson = """{"name":"John","age":30}"""
        val result = minifier.minify(minifiedJson)
        assertEquals(minifiedJson, result)
    }

    @Test
    fun `minify with JSON containing various data types`() {
        // Test with a JSON containing strings, numbers, booleans, arrays, nested objects, and nulls to ensure all are preserved correctly.
        val complexJson = """
            {
              "string": "value",
              "number": 42,
              "decimal": 3.14,
              "bool": true,
              "nullValue": null,
              "array": [1, 2, 3],
              "nested": {
                "key": "val"
              }
            }
        """.trimIndent()
        val minified = minifier.minify(complexJson)
        // Verify that minified is valid and contains all data
        assertTrue(minifier.isValidJson(minified))
        assertTrue(minified.contains("\"string\":\"value\""))
        assertTrue(minified.contains("\"number\":42"))
        assertTrue(minified.contains("\"decimal\":3.14"))
        assertTrue(minified.contains("\"bool\":true"))
        assertTrue(minified.contains("\"nullValue\":null"))
        assertTrue(minified.contains("\"array\":[1,2,3]"))
    }

    @Test
    fun `minify with JSON containing special characters`() {
        // Ensure that strings with escaped characters (e.g., \n, \t, \", \\) and Unicode characters are minified correctly without data loss.
        val jsonWithSpecialChars = """
            {
              "newline": "line1\nline2",
              "tab": "col1\tcol2",
              "quote": "He said \"Hello\"",
              "backslash": "C:\\path\\to\\file",
              "unicode": "Hello 世界"
            }
        """.trimIndent()
        val minified = minifier.minify(jsonWithSpecialChars)
        assertTrue(minifier.isValidJson(minified))
        assertTrue(minified.contains("line1\\nline2"))
        assertTrue(minified.contains("He said \\\"Hello\\\""))
    }

    /**
     * TODO: enable it when issue is fixed
     *  https://github.com/Kotlin/kotlinx.serialization/issues/2511
     */
    @Test
    fun `minify with malformed JSON input`() {
        // Provide a malformed JSON string (e.g., with a missing quote) and assert that a JsonMinificationException is thrown due to the initial validation.
        /*val malformedJson = """{"key": value}"""
        assertFailsWith<JsonMinificationException> {
            minifier.minify(malformedJson)
        }*/
    }

    @Test
    fun `minify with an empty JSON object`() {
        // Provide "{  }" as input and assert the output is "{}".
        val emptyObject = "{  }"
        val result = minifier.minify(emptyObject)
        assertEquals("{}", result)
    }

    @Test
    fun `minify with an empty JSON array`() {
        // Provide "[  ]" as input and assert the output is "[]".
        val emptyArray = "[  ]"
        val result = minifier.minify(emptyArray)
        assertEquals("[]", result)
    }

    @Test
    fun `minify with a blank string input`() {
        // Pass a blank string and assert that a JsonMinificationException is thrown because it's not valid JSON.
        assertFailsWith<JsonMinificationException> {
            minifier.minify("   \t\n   ")
        }
    }

    @Test
    fun `minify with an empty string input`() {
        // Pass an empty string and assert that a JsonMinificationException is thrown because it's not valid JSON.
        assertFailsWith<JsonMinificationException> {
            minifier.minify("")
        }
    }

    @Test
    fun `minify with JSON containing unknown keys`() {
        // Since ignoreUnknownKeys is false, ensure that all keys and values from the input are present in the minified output.
        val jsonWithAllKeys = """
            {
              "known": "value",
              "another": 123,
              "deep": {
                "nested": "data"
              }
            }
        """.trimIndent()
        val minified = minifier.minify(jsonWithAllKeys)
        assertTrue(minified.contains("\"known\":\"value\""))
        assertTrue(minified.contains("\"another\":123"))
        assertTrue(minified.contains("\"deep\":{\"nested\":\"data\"}"))
    }

    @Test
    fun `minify preserves spaces within string values`() {
        // Verify that spaces inside JSON string values are preserved during minification
        val json = """
            {
              "message": "This is a sentence with spaces",
              "code": "function test() { return 'hello world'; }",
              "multiline": "Line 1\nLine 2\nLine 3"
            }
        """.trimIndent()
        val minified = minifier.minify(json)
        
        assertTrue(minifier.isValidJson(minified))
        assertTrue(minified.contains("This is a sentence with spaces"))
        assertTrue(minified.contains("function test() { return 'hello world'; }"))
        assertTrue(minified.contains("Line 1\\nLine 2\\nLine 3"))
    }

    @Test
    fun `minify with array-heavy JSON from resource file`() {
        // Test minification of JSON with heavy array usage (nested arrays, arrays in objects, etc.)
        val arrayHeavyJson = readResourceFile("array-heavy.json")
        
        assertTrue(minifier.isValidJson(arrayHeavyJson))
        
        val minified = minifier.minify(arrayHeavyJson)
        
        // Verify minified JSON is valid
        assertTrue(minifier.isValidJson(minified))
        
        // Verify no newlines in minified output
        assertFalse(minified.contains("\n"))
        
        // Verify key data structures are preserved
        assertTrue(minified.contains("\"users\":["))
        assertTrue(minified.contains("\"roles\":["))
        assertTrue(minified.contains("\"permissions\":"))
        
        // Verify minified is smaller than original
        assertTrue(minified.length < arrayHeavyJson.length)
    }

    @Test
    fun `minify with large file from resource`() {
        // Test minification of a large JSON file (~500KB) to ensure performance and correctness
        val largeJson = readResourceFile("large-file.json")
        
        assertTrue(minifier.isValidJson(largeJson))
        
        val minified = minifier.minify(largeJson)
        
        // Verify minified JSON is valid
        assertTrue(minifier.isValidJson(minified))
        
        // Verify no newlines in minified output
        assertFalse(minified.contains("\n"))
        
        // Verify structure is preserved
        assertTrue(minified.contains("\"data\":["))
        
        // Verify significant size reduction
        assertTrue(minified.length < largeJson.length)
        println("Large file minification: ${largeJson.length} -> ${minified.length} bytes (${100 - (minified.length * 100 / largeJson.length)}% reduction)")
    }

    /**
     * Read a file from test resources directory.
     */
    private fun readResourceFile(filename: String): String {
        return this::class.java.classLoader
            .getResourceAsStream(filename)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalArgumentException("Resource file not found: $filename")
    }

}