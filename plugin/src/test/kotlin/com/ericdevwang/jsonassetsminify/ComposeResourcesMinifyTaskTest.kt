package com.ericdevwang.jsonassetsminify

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeResourcesMinifyTaskTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `minifies JSON under files and preserves other resources`() {
        val inputDirectory = temporaryDirectory.resolve("src/commonMain/composeResources")
        val outputDirectory = temporaryDirectory.resolve("build/generated/composeResources")
        Files.createDirectories(inputDirectory.resolve("files/nested"))
        Files.writeString(
            inputDirectory.resolve("files/config.json"),
            "{\n  \"enabled\": true,\n  \"items\": [1, 2, 3]\n}\n",
        )
        Files.writeString(inputDirectory.resolve("files/nested/readme.txt"), "keep me\n")

        createTask(inputDirectory, outputDirectory).minify()

        assertEquals(
            "{\"enabled\":true,\"items\":[1,2,3]}",
            Files.readString(outputDirectory.resolve("files/config.json")),
        )
        assertEquals("keep me\n", Files.readString(outputDirectory.resolve("files/nested/readme.txt")))
    }

    @Test
    fun `copies invalid and ignored JSON without changing it`() {
        val inputDirectory = temporaryDirectory.resolve("src/commonMain/composeResources")
        val outputDirectory = temporaryDirectory.resolve("build/generated/composeResources")
        Files.createDirectories(inputDirectory.resolve("files"))
        Files.writeString(inputDirectory.resolve("files/invalid.json"), "{ invalid }\n")
        Files.writeString(inputDirectory.resolve("files/debug.json"), "{\n  \"debug\": true\n}\n")

        val task = createTask(inputDirectory, outputDirectory)
        task.ignoredFiles.set(listOf("files/debug.json"))
        task.minify()

        assertEquals("{ invalid }\n", Files.readString(outputDirectory.resolve("files/invalid.json")))
        assertEquals(
            "{\n  \"debug\": true\n}\n",
            Files.readString(outputDirectory.resolve("files/debug.json")),
        )
        assertTrue(Files.exists(outputDirectory.resolve("files/invalid.json")))
    }

    private fun createTask(inputDirectory: Path, outputDirectory: Path): ComposeResourcesMinifyTask {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        return project.tasks.register("minifyComposeResources", ComposeResourcesMinifyTask::class.java).get().also {
            it.inputDirectory.set(inputDirectory.toFile())
            it.outputDirectory.set(outputDirectory.toFile())
            it.ignoredFiles.set(emptyList())
        }
    }
}
