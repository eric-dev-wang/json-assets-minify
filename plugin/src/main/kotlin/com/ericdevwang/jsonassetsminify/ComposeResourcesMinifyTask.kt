package com.ericdevwang.jsonassetsminify

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

/**
 * Copies one Compose Resources source-set directory and minifies JSON files
 * under its `files` directory into a generated directory.
 */
@CacheableTask
abstract class ComposeResourcesMinifyTask : DefaultTask() {
    @get:Internal
    abstract val inputDirectory: DirectoryProperty

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputFiles: FileTree
        get() = inputDirectory.asFileTree

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val ignoredFiles: ListProperty<String>

    private val jsonMinifier = JsonMinifier()

    @TaskAction
    fun minify() {
        val input = inputDirectory.get().asFile
        val output = outputDirectory.get().asFile

        output.deleteRecursively()
        if (!input.isDirectory) {
            logger.info("No Compose Resources directory for ${inputDirectory.get().asFile}; skipping")
            return
        }

        var processedCount = 0
        var skippedCount = 0
        var totalSaved = 0L

        input.walkTopDown()
            .filter(File::isFile)
            .forEach { sourceFile ->
                val relativePath = sourceFile.relativeTo(input).path.replace(File.separatorChar, '/')
                val destination = output.resolve(relativePath)
                destination.parentFile.mkdirs()

                if (shouldIgnore(relativePath)) {
                    sourceFile.copyTo(destination, overwrite = true)
                    skippedCount++
                } else if (relativePath.startsWith("files/") &&
                    sourceFile.extension.equals("json", ignoreCase = true)
                ) {
                    val content = sourceFile.readText(StandardCharsets.UTF_8)
                    val minified = try {
                        jsonMinifier.minify(content)
                    } catch (error: JsonMinificationException) {
                        logger.warn("Skipping invalid Compose Resource JSON $relativePath: ${error.message}")
                        sourceFile.copyTo(destination, overwrite = true)
                        skippedCount++
                        return@forEach
                    }
                    destination.writeText(minified, StandardCharsets.UTF_8)
                    processedCount++
                    totalSaved += (content.toByteArray(StandardCharsets.UTF_8).size -
                        minified.toByteArray(StandardCharsets.UTF_8).size).coerceAtLeast(0)
                } else {
                    sourceFile.copyTo(destination, overwrite = true)
                }
            }

        logger.lifecycle(
            "Compose Resources JSON minification for ${inputDirectory.get().asFile.name}: " +
                "processed=$processedCount, skipped=$skippedCount, saved=$totalSaved bytes",
        )
    }

    private fun shouldIgnore(relativePath: String): Boolean =
        ignoredFiles.get().any { pattern ->
            try {
                val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
                matcher.matches(FileSystems.getDefault().getPath(relativePath))
            } catch (_: Exception) {
                relativePath == pattern
            }
        }
}
