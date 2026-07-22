package com.ericdevwang.jsonassetsminify

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeResourcesMinifyPlugin(extension: JsonAssetsMinifyExtension) {
    logger.info("Configuring JSON minification for Compose Multiplatform resources")

    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
    val compose = extensions.getByType(ComposeExtension::class.java)
    val resources = compose.extensions.getByType(ResourcesExtension::class.java)

    val taskProviders = linkedMapOf<String, TaskProvider<ComposeResourcesMinifyTask>>()

    kotlin.sourceSets.configureEach {
        val sourceSetName = name
        val taskName = "minifyComposeResources${sourceSetName.uppercaseFirstChar()}"
        val sourceDirectory = layout.projectDirectory.dir("src/$sourceSetName/composeResources")
        val outputDirectory = layout.buildDirectory.dir(
            "generated/jsonAssetsMinify/composeResources/$sourceSetName/composeResources",
        )

        val task = tasks.register<ComposeResourcesMinifyTask>(taskName) {
            group = "compose resources"
            description = "Minifies JSON files in Compose Resources for $sourceSetName"
            inputDirectory.set(sourceDirectory)
            this.outputDirectory.set(outputDirectory)
            ignoredFiles.set(provider { extension.ignoredFiles })
        }
        taskProviders[sourceSetName] = task

        resources.customDirectory(
            sourceSetName = sourceSetName,
            directoryProvider = task.flatMap { it.outputDirectory },
        )
    }

    afterEvaluate {
        taskProviders.forEach { (sourceSetName, task) ->
            task.configure {
                extension.configureComposeResourcesDirectory(sourceSetName, inputDirectory)
            }
            resources.customDirectory(
                sourceSetName = sourceSetName,
                directoryProvider = task.flatMap { it.outputDirectory },
            )
        }
    }
}
