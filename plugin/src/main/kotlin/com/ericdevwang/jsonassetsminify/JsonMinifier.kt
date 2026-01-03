package com.ericdevwang.jsonassetsminify

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.annotations.VisibleForTesting

/**
 * Core component responsible for JSON compression logic.
 */
internal class JsonMinifier {
    private val json = Json {
        // Configure for compact output (no pretty printing)
        prettyPrint = false
        // Allow parsing of slightly malformed JSON where possible
        isLenient = false
        // Don't ignore unknown keys to preserve all data
        ignoreUnknownKeys = false
    }

    /**
     * Minifies JSON content by removing unnecessary whitespace while preserving structure and data.
     *
     * @param jsonContent The original JSON content as a string
     * @return The minified JSON content, or the original content if minification fails
     * @throws JsonMinificationException if the JSON is malformed and cannot be processed
     */
    fun minify(jsonContent: String): String {
        // Validate input JSON before processing
        if (!isValidJson(jsonContent)) {
            throw JsonMinificationException("Input JSON is malformed and cannot be parsed.")
        }

        return try {
            val jsonElement = json.parseToJsonElement(jsonContent)
            val minified = json.encodeToString(JsonElement.serializer(), jsonElement)

            // Validate the minified output is still valid JSON
            if (!isValidJson(minified)) {
                throw JsonMinificationException("Minified output is not valid JSON. This indicates an internal error.")
            }

            minified
        } catch (e: SerializationException) {
            throw JsonMinificationException(
                "Failed to minify JSON due to serialization error: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw JsonMinificationException(
                "Unexpected error during JSON minification: ${e.javaClass.simpleName} - ${e.message}",
                e,
            )
        }
    }

    /**
     * Validates that the provided content is valid JSON.
     *
     * @param content The JSON content to validate
     * @return true if the content is valid JSON, false otherwise
     */
    @VisibleForTesting
    fun isValidJson(content: String): Boolean {
        if (content.isBlank()) return false

        return try {
            json.parseToJsonElement(content)
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Exception thrown when JSON minification fails.
 *
 * @param message The error message describing what went wrong
 * @param cause The underlying cause of the error, if any
 */
internal class JsonMinificationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
