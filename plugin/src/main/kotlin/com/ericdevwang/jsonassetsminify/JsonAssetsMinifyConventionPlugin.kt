package com.ericdevwang.jsonassetsminify

import org.gradle.api.Plugin
import org.gradle.api.Project

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
            val extension = extensions.create("jsonAssetsMinify", JsonAssetsMinifyExtension::class.java)

            pluginManager.withPlugin("com.android.application") {
                configureAndroidJsonMinifyPlugin(extension)
            }
            pluginManager.withPlugin("com.android.library") {
                configureAndroidJsonMinifyPlugin(extension)
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                pluginManager.withPlugin("org.jetbrains.compose") {
                    configureComposeResourcesMinifyPlugin(extension)
                }
            }

            afterEvaluate {
                extension.validate()
                extension.logConfiguration(logger)
            }
        }
    }
}
