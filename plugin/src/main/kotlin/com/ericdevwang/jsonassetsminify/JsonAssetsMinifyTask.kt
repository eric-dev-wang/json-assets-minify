package com.ericdevwang.jsonassetsminify

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

/**
 * Gradle task that minifies JSON files in the assets directories.
 *
 * This task processes JSON files recursively from one or more specified asset source directories,
 * removing unnecessary whitespace while preserving JSON structure and validity.
 * All source directories are merged into a single output directory.
 *
 * The task supports incremental builds - only changed JSON files will be reprocessed.
 * Files can be ignored based on configuration in JsonMinifyExtension.
 */
abstract class JsonAssetsMinifyTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assetsRoots: ListProperty<Directory>

    /**
     * Multiple asset source directories (from variant sourceSets configuration).
     * These are the actual directories configured in android.sourceSets.assets.srcDirs.
     */
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assetsSources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val minifiedAssetsDirectory: DirectoryProperty

    @get:Nested
    abstract val extension: Property<JsonAssetsMinifyExtension>

    private val jsonMinifier = JsonMinifier()

    init {
        description =
            "Minifies JSON files from assets directory and outputs them to a separate directory"
    }

    @TaskAction
    fun minifyJsonFiles(inputChanges: InputChanges) {
        val sourceAssetsRoot = assetsRoots.get()
        val sourceAssetsFiles = assetsSources.files.filter { it.exists() }.toList()
        val outputAssetsDir = minifiedAssetsDirectory.get().asFile

        // Log process start with detailed information
        logger.lifecycle("=== JSON Assets Minification Started ===")
        logger.lifecycle("Output assets directory: ${outputAssetsDir.absolutePath}")
        logger.lifecycle("Build type: ${if (inputChanges.isIncremental) "Incremental" else "Full rebuild"}")
        logger.lifecycle("Source asset directories (${sourceAssetsFiles.size}):")
        sourceAssetsFiles.forEach { file ->
            logger.info("  - ${file.absolutePath}")
        }

        // Ensure output directory exists and is clean for full rebuilds
        if (!inputChanges.isIncremental) {
            if (outputAssetsDir.exists()) {
                outputAssetsDir.deleteRecursively()
            }
        }
        outputAssetsDir.mkdirs()

        // Validate source assets directories exist
        if (sourceAssetsFiles.isEmpty()) {
            logger.lifecycle("No source assets directories found")
            logger.lifecycle("Skipping JSON minification - no assets to process")
            return
        }

        // Handle incremental vs full build
        val filesToProcess =
            if (inputChanges.isIncremental) {
                logger.lifecycle("Scanning for changed JSON files...")
                getChangedJsonFiles(inputChanges, sourceAssetsRoot, assetsSources)
            } else {
                logger.lifecycle("Scanning for all JSON files in assets directories...")
                sourceAssetsFiles
            }.distinct()

        // Add file ignore functionality
        val filteredFiles =
            filesToProcess.filter { file ->
                val rootDir = findRootDir(sourceAssetsRoot, file)
                val shouldIgnore = extension.get().shouldIgnoreFile(file, rootDir.asFile)
                if (shouldIgnore) {
                    logger.info("Ignoring file: ${file.path} (matches ignore pattern)")
                }
                !shouldIgnore
            }

        if (filteredFiles.isEmpty()) {
            logger.lifecycle("No JSON files found to process")
            return
        }

        logger.lifecycle("Found ${filteredFiles.size} JSON file(s) to process:")
        filteredFiles.forEach { file ->
            val rootDir = findRootDir(sourceAssetsRoot, file)
            logger.info("  - ${file.relativeTo(rootDir.asFile).path} (${file.length()} bytes)")
        }

        // Process JSON files
        filteredFiles.forEach { file ->
            val sourceDir = findRootDir(sourceAssetsRoot, file).asFile
            val displayPath = file.relativeTo(sourceDir).path
            logger.info("Processing file: $displayPath")

            val result = processJsonFile(file, sourceDir, outputAssetsDir)

            val sizeSaved = result.originalSize - result.minifiedSize
            val compressionRatio =
                if (result.originalSize > 0) {
                    ((sizeSaved.toDouble() / result.originalSize) * 100).toInt()
                } else {
                    0
                }

            logger.info("✓ Successfully processed: $displayPath")
            logger.info("  Original size: ${result.originalSize} bytes")
            logger.info("  Minified size: ${result.minifiedSize} bytes")
            logger.info("  Size saved: $sizeSaved bytes ($compressionRatio% reduction)")
        }
    }

    private fun findRootDir(
        sourceAssetsRoot: List<Directory>,
        assetFile: File,
    ): Directory {
        for (root in sourceAssetsRoot) {
            if (assetFile.absolutePath.startsWith(root.asFile.absolutePath)) {
                return root
            }
        }
        throw IllegalStateException("Could not find root directory for file: ${assetFile.absolutePath}")
    }

    private fun getChangedJsonFiles(
        inputChanges: InputChanges,
        sourceAssetsRoot: List<Directory>,
        sourceAssetsFiles: FileCollection,
    ): List<File> {
        val changedFiles = mutableListOf<File>()

        try {
            // Get all changed files from the assetsSources input property
            inputChanges.getFileChanges(sourceAssetsFiles).forEach { change ->
                val file = change.file
                val rootDir = findRootDir(sourceAssetsRoot, file).asFile

                when (change.changeType) {
                    ChangeType.ADDED,
                    ChangeType.MODIFIED,
                    -> {
                        // Check if file should be ignored
                        if (!extension.get().shouldIgnoreFile(file, rootDir)) {
                            changedFiles.add(file)
                            logger.debug("Changed JSON file detected: ${file.path}")
                        }
                    }

                    ChangeType.REMOVED -> {
                        logger.debug("JSON file removed: ${file.path}")
                        // Remove corresponding output file (regardless of ignore patterns)
                        val displayPath = file.relativeTo(rootDir).path
                        val outputFile = File(minifiedAssetsDirectory.get().asFile, displayPath)
                        if (!outputFile.exists() || outputFile.delete()) {
                            logger.debug("Removed corresponding output file: $displayPath")
                        } else {
                            throw IllegalStateException("Failed to delete output file: $displayPath")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error getting changed files for incremental build: ${e.message}")
        }

        return changedFiles
    }

    private fun processJsonFile(
        sourceFile: File,
        sourceAssetsDir: File,
        outputAssetsDir: File,
    ): JsonProcessingResult {
        val relativePath = sourceFile.relativeTo(sourceAssetsDir)
        val outputFile = File(outputAssetsDir, relativePath.path)

        return try {
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()

            // Read original content
            val originalContent =
                try {
                    sourceFile.readText()
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to read source file content: ${e.message}")
                }

            val originalSize = originalContent.length.toLong()

            // Check if file is empty
            if (originalContent.isBlank()) {
                throw IllegalStateException("Source file is empty or contains only whitespace")
            }

            // Minify the JSON content
            val minifiedContent =
                try {
                    jsonMinifier.minify(originalContent)
                } catch (e: JsonMinificationException) {
                    throw IllegalStateException("JSON minification failed: ${e.message}")
                }

            val minifiedSize = minifiedContent.length.toLong()

            // Write minified content to output file
            try {
                outputFile.writeText(minifiedContent)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to write minified content to output file: ${e.message}")
            }

            JsonProcessingResult(
                file = sourceFile,
                originalSize = originalSize,
                minifiedSize = minifiedSize,
            )
        } catch (e: Exception) {
            throw IllegalStateException("Unexpected error processing file: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

/**
 * Represents the result of processing a single JSON file.
 */
private data class JsonProcessingResult(
    val file: File,
    val originalSize: Long,
    val minifiedSize: Long,
)
