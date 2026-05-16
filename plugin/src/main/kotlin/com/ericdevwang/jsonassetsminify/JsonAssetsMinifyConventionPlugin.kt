package com.ericdevwang.jsonassetsminify

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Convention plugin for JSON Assets Minification.
 *
 * Registers tasks to minify JSON files in the merged assets directory
 * for configured build variants. The minification runs AFTER mergeAssets
 * and modifies files in-place, ensuring the final APK/AAR contains
 * minified JSON.
 */
class JsonAssetsMinifyConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                configureJsonMinifyPlugin()
            }
            pluginManager.withPlugin("com.android.library") {
                configureJsonMinifyPlugin()
            }
        }
    }

    private fun Project.configureJsonMinifyPlugin() {
        logger.info("Configuring JSON Assets Minify plugin for project: $name")

        val extension = extensions.create("jsonAssetsMinify", JsonAssetsMinifyExtension::class.java)

        val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            val buildType = variant.buildType ?: return@onVariants

            if (!extension.isEnabledForBuildType(buildType)) {
                logger.info("Skipping JSON minification for build type '$buildType' (disabled)")
                return@onVariants
            }

            val variantName = variant.name.uppercaseFirstChar()
            val taskName = "minifyJsonAssets$variantName"

            val minifyTask: TaskProvider<JsonAssetsMinifyTask> =
                tasks.register<JsonAssetsMinifyTask>(taskName) {
                    group = "android"
                    description =
                        "Minifies JSON files in merged assets for ${variant.name} build"
                    this.extension.set(extension)
                }

            configureTaskDependencies(minifyTask, variantName, taskName)

            logger.lifecycle("JSON Assets Minify plugin applied for variant '${variant.name}'")
        }

        afterEvaluate {
            extension.validate()
            extension.logConfiguration(logger)
        }
    }

    /**
     * Wire task dependencies:
     * merge{Variant}Assets → minifyJsonAssets{Variant} → downstream tasks
     */
    private fun Project.configureTaskDependencies(
        minifyTask: TaskProvider<JsonAssetsMinifyTask>,
        variantName: String,
        taskName: String,
    ) {
        val mergeTaskName = "merge${variantName}Assets"

        // minify depends on merge and reads its output directory
        minifyTask.configure {
            dependsOn(mergeTaskName)
        }

        // Wire the merged assets directory from the merge task output
        afterEvaluate {
            val mergeTask = tasks.findByName(mergeTaskName)
            if (mergeTask != null) {
                val mergedOutputDir = mergeTask.outputs.files.singleFile
                minifyTask.configure {
                    mergedAssetsDirectory.set(mergedOutputDir)
                }
                logger.info("Wired minify task '$taskName' to merged assets: ${mergedOutputDir.absolutePath}")
            } else {
                logger.warn("Merge task '$mergeTaskName' not found. JSON minification may not work.")
            }
        }

        // Downstream tasks that consume merged assets must run after minification.
        // Use tasks.matching (lazy) so it works even if tasks are registered later.
        tasks.matching { task ->
            task.name != taskName &&
                task.name.contains(variantName) &&
                task.name.let { name ->
                    name.startsWith("compress") ||
                        name.startsWith("package") ||
                        name.startsWith("bundle")
                }
        }.configureEach {
            dependsOn(minifyTask)
        }

        // Lint tasks
        tasks.matching { task ->
            val name = task.name
            val nameLower = name.lowercase()
            name.contains(variantName) &&
                name != taskName &&
                (nameLower.startsWith("lint") ||
                    (nameLower.startsWith("generate") && nameLower.contains("lint")))
        }.configureEach {
            dependsOn(minifyTask)
        }
    }
}
