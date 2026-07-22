package com.ericdevwang.jsonassetsminify

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task that minifies JSON files in the merged assets directory in-place.
 *
 * This task runs AFTER merge{Variant}Assets and modifies JSON files directly
 * in the merged output directory, ensuring minified content ends up in the final APK/AAR.
 */
@DisableCachingByDefault(because = "The task modifies merged assets in place and must always run when requested.")
abstract class JsonAssetsMinifyTask : DefaultTask() {
    /**
     * The merged assets directory (output of merge{Variant}Assets).
     * Marked as @Internal because we modify files in-place — Gradle's
     * UP-TO-DATE checking is handled by [outputs.upToDateWhen].
     */
    @get:Internal
    abstract val mergedAssetsDirectory: DirectoryProperty

    @get:Nested
    abstract val extension: Property<JsonAssetsMinifyExtension>

    private val jsonMinifier = JsonMinifier()

    init {
        description = "Minifies JSON files in the merged assets directory"
        // Always run when triggered — the task modifies merged output in-place
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun minifyJsonFiles() {
        val assetsDir = mergedAssetsDirectory.get().asFile

        logger.lifecycle("=== JSON Assets Minification Started ===")
        logger.lifecycle("Merged assets directory: ${assetsDir.absolutePath}")

        if (!assetsDir.exists() || !assetsDir.isDirectory) {
            logger.lifecycle("Merged assets directory does not exist or is not a directory. Skipping.")
            return
        }

        // Find all JSON files in the merged directory
        val jsonFiles = assetsDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            .toList()

        if (jsonFiles.isEmpty()) {
            logger.lifecycle("No JSON files found in merged assets directory.")
            return
        }

        logger.lifecycle("Found ${jsonFiles.size} JSON file(s) to evaluate.")

        var processedCount = 0
        var skippedCount = 0
        var totalSaved = 0L

        jsonFiles.forEach { file ->
            val relativePath = file.relativeTo(assetsDir).path

            // Check ignore patterns
            if (extension.get().shouldIgnoreFile(file, assetsDir)) {
                logger.info("Ignoring file: $relativePath (matches ignore pattern)")
                skippedCount++
                return@forEach
            }

            try {
                val originalContent = file.readText()
                if (originalContent.isBlank()) {
                    logger.info("Skipping empty file: $relativePath")
                    skippedCount++
                    return@forEach
                }

                val originalSize = originalContent.length.toLong()
                val minifiedContent = jsonMinifier.minify(originalContent)
                val minifiedSize = minifiedContent.length.toLong()

                // Only write if content actually changed
                if (minifiedSize < originalSize) {
                    file.writeText(minifiedContent)
                    val saved = originalSize - minifiedSize
                    totalSaved += saved
                    val ratio = ((saved.toDouble() / originalSize) * 100).toInt()
                    logger.info("✓ Minified: $relativePath ($originalSize → $minifiedSize bytes, -$ratio%)")
                } else {
                    logger.info("✓ Already compact: $relativePath ($originalSize bytes)")
                }
                processedCount++
            } catch (e: JsonMinificationException) {
                logger.warn("⚠ Failed to minify $relativePath: ${e.message}")
                skippedCount++
            } catch (e: Exception) {
                logger.warn("⚠ Unexpected error processing $relativePath: ${e.message}")
                skippedCount++
            }
        }

        logger.lifecycle("=== JSON Assets Minification Complete ===")
        logger.lifecycle("Processed: $processedCount, Skipped: $skippedCount, Total saved: $totalSaved bytes")
    }
}
