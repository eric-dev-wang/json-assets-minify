package com.ericdevwang.jsonassetsminify

import org.gradle.api.logging.Logger
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

/**
 * Extension for configuring JSON Assets Minify plugin behavior.
 */
open class JsonAssetsMinifyExtension {
    private val _disabledBuildTypes = mutableListOf<String>()
    private val _ignoredFiles = mutableListOf<String>()

    /**
     * Configuration for Compose Multiplatform resource source sets.
     */
    @get:Internal
    val composeResources = ComposeResourcesMinifyExtension()

    /**
     * Get the list of disabled build types (for testing).
     */
    @get:Input
    val disabledBuildTypes: List<String>
        get() = _disabledBuildTypes.toList()

    /**
     * Get the list of ignored file patterns (for testing).
     */
    @get:Input
    val ignoredFiles: List<String>
        get() = _ignoredFiles.toList()

    /**
     * Configure build types that should be disabled from processing.
     */
    fun disabledBuildTypes(vararg buildTypes: String) {
        _disabledBuildTypes.addAll(buildTypes)
    }

    /**
     * Configure file patterns that should be ignored during processing.
     *
     * Supports glob patterns:
     * - asterisk matches any characters within a single directory level
     * - double asterisk matches any characters across multiple directory levels
     *
     * Examples:
     * - "config.json" - ignores specific file
     * - "test_asterisk.json" - ignores files starting with "test_"
     * - "double_asterisk/test/double_asterisk" - ignores all files in any "test" directory
     * - "double_asterisk/asterisk_test.json" - ignores files ending with "_test.json" anywhere
     */
    fun ignoredFiles(vararg patterns: String) {
        _ignoredFiles.addAll(patterns)
    }

    /**
     * Check if a build type should be processed for JSON minification.
     */
    fun isEnabledForBuildType(buildType: String): Boolean = buildType !in _disabledBuildTypes

    /**
     * Check if a file should be ignored during JSON minification.
     */
    fun shouldIgnoreFile(
        file: File,
        assetsDir: File,
    ): Boolean {
        if (_ignoredFiles.isEmpty()) return false

        // Get relative path from assets directory
        val relativePath =
            try {
                file.relativeTo(assetsDir).path.replace('\\', '/')
            } catch (e: IllegalArgumentException) {
                // File is not under assets directory, use absolute path
                file.absolutePath.replace('\\', '/')
            }

        return shouldIgnorePath(relativePath)
    }

    /**
     * Check a normalized path against the configured ignore patterns.
     */
    internal fun shouldIgnorePath(relativePath: String): Boolean =
        _ignoredFiles.any { pattern -> matchesGlobPattern(relativePath, pattern) }

    /**
     * Apply a custom Compose Resources input directory to a source-set task, if configured.
     */
    internal fun configureComposeResourcesDirectory(
        sourceSetName: String,
        property: DirectoryProperty,
    ) {
        composeResources.configureDirectoryFor(sourceSetName, property)
    }

    /**
     * Validate the extension configuration.
     */
    fun validate() {
        _disabledBuildTypes.forEach { buildType ->
            if (buildType.isBlank()) {
                throw IllegalArgumentException("Build type names cannot be empty or blank")
            }
        }

        _ignoredFiles.forEach { pattern ->
            if (pattern.isBlank()) {
                throw IllegalArgumentException("File ignore patterns cannot be empty or blank")
            }
        }
    }

    /**
     * Log the current configuration to the provided logger.
     */
    fun logConfiguration(logger: Logger) {
        // Log build type configuration
        if (_disabledBuildTypes.isEmpty()) {
            logger.info("JSON minification enabled for all build types")
        } else {
            logger.info("JSON minification configuration:")
            logger.info("  Disabled build types: ${_disabledBuildTypes.joinToString(", ")}")
            logger.info("  All other build types will be processed")
        }

        // Log file ignore configuration
        if (_ignoredFiles.isEmpty()) {
            logger.info("JSON minification will process all JSON files")
        } else {
            logger.info("JSON minification file ignore patterns:")
            _ignoredFiles.forEach { pattern ->
                logger.info("  - $pattern")
            }
        }
    }
}
