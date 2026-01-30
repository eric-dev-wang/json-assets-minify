package com.ericdevwang.jsonassetsminify

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Convention plugin for JSON Assets Minification.
 *
 * This plugin applies to Android application modules and registers tasks to minify
 * JSON files from the assets directory for configured build variants.
 * The plugin supports configuration through JsonMinifyExtension for build type
 * filtering and file ignore patterns.
 */
class JsonAssetsMinifyConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply to Android application modules
            pluginManager.withPlugin("com.android.application") {
                configureJsonMinifyPlugin()
            }
            // Apply to Android library modules
            pluginManager.withPlugin("com.android.library") {
                configureJsonMinifyPlugin()
            }
        }
    }

    private fun Project.configureJsonMinifyPlugin() {
        logger.info("Configuring JSON Assets Minify plugin for project: $name")

        // Create extension for configuration
        val extension = extensions.create("jsonAssetsMinify", JsonAssetsMinifyExtension::class.java)

        // Configure tasks for all variants with build type filtering
        val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            val buildType = variant.buildType ?: return@onVariants

            if (!extension.isEnabledForBuildType(buildType)) {
                logger.info("Skipping JSON minification for build type '$buildType' (disabled in configuration)")
                return@onVariants
            }

            val assets = variant.sources.assets ?: return@onVariants
            assets.all

            val taskName = "minifyJsonAssets${variant.name.uppercaseFirstChar()}"

            val outputAssetsDir =
                layout.buildDirectory.dir("intermediates/minified_assets/${variant.name}")

            val minifyTask: TaskProvider<JsonAssetsMinifyTask> =
                tasks.register<JsonAssetsMinifyTask>(taskName) {
                    group = "android"
                    description =
                        "Minifies JSON files from assets directory for ${variant.name} build"

                    assetsRoots.set(
                        assets.all.map {
                            it
                                .flatten()
                                .filterNot { dir -> dir == outputAssetsDir.get() }
                        },
                    )
                    // Set asset sources from variant configuration
                    assetsSources.from(
                        assets.all.map { orderedDirectories ->
                            orderedDirectories
                                .flatten()
                                .filterNot { dir -> dir == outputAssetsDir.get() }
                                .map { dir ->
                                    dir.asFileTree.matching {
                                        include("**/*.json")
                                    }
                                }
                        },
                    )

                    minifiedAssetsDirectory.set(outputAssetsDir)

                    // Pass extension and buildType to task
                    this.extension.set(extension)

                    logger.info("JSON minify task registered for ${variant.name}:")
                    logger.info("  Output: ${outputAssetsDir.get().asFile.absolutePath}")
                }

            // Configure task dependencies and Android integration
            configureTaskDependencies(minifyTask, variant.name.uppercaseFirstChar())
            configureAndroidAssetsReplacement(variant, outputAssetsDir)

            logger.info("JSON minify task '$taskName' registered for build type '$buildType'")
        }

        // Log configuration after evaluation
        afterEvaluate {
            extension.validate()
            extension.logConfiguration(logger)
            logger.info("JSON minification configuration applied")
        }

        logger.lifecycle("JSON Assets Minify plugin applied successfully to project '$name'")
    }

    private fun Project.configureTaskDependencies(
        minifyTask: TaskProvider<JsonAssetsMinifyTask>,
        variantName: String,
    ) {
        // Configure task dependencies to run after Android assets are generated for this variant
        minifyTask.dependsOn("generate${variantName}Assets")
        logger.debug("Configured minify task dependency: ${minifyTask.name} depends on generate${variantName}Assets")
        // Configure task dependencies to run before Android assets are merged for this variant
        tasks.matching { task ->
            task.name == "merge${variantName}Assets"
        }.configureEach {
            dependsOn(minifyTask)
            logger.debug("Configured asset task dependency: $name depends on ${minifyTask.name}")
        }

        // Configure lint tasks to depend on minification (required by Gradle 9.1+)
        tasks.matching { task ->
            task.name.contains(variantName) &&
                    (task.name.contains("generateLint") ||
                            task.name.contains("generateLintModel") ||
                            task.name.contains("lintVital") ||
                            task.name.contains("Lint"))
        }.configureEach {
            dependsOn(minifyTask)
        }

        logger.debug("Configured dependencies for $variantName to run before asset merging")
    }

    private fun Project.configureAndroidAssetsReplacement(
        variant: Variant,
        outputAssetsDir: Provider<Directory>,
    ) {
        try {
            // Add the minified assets directory as an additional assets source set
            // Use relative path from project root
            val relativePath = outputAssetsDir.get()
                .asFile
                .relativeTo(project.projectDir)
                .path

            logger.lifecycle("Adding minified assets directory to ${variant.name}: $relativePath")
            variant.sources
                .assets
                ?.addStaticSourceDirectory(relativePath)

            logger.info("Added minified assets directory to ${variant.name} source set: $relativePath")
        } catch (e: Exception) {
            logger.warn("Could not configure Android assets replacement for ${variant.name}: ${e.message}")
        }
    }
}
