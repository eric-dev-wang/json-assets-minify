package com.ericdevwang.jsonassetsminify

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

/**
 * Compose Multiplatform-specific configuration.
 */
open class ComposeResourcesMinifyExtension {
    private val sourceSetDirectoryConfigurators = linkedMapOf<String, (DirectoryProperty) -> Unit>()

    /**
     * Configure the input Compose Resources directory for a Kotlin source set.
     *
     * If omitted, the plugin uses `src/<sourceSetName>/composeResources`.
     */
    fun sourceSet(
        sourceSetName: String,
        directoryProvider: Provider<Directory>,
    ) {
        require(sourceSetName.isNotBlank()) {
            "Compose Resources source set names cannot be blank"
        }
        sourceSetDirectoryConfigurators[sourceSetName] = { property -> property.set(directoryProvider) }
    }

    /**
     * Configure the input directory directly when it is known at configuration time.
     */
    fun sourceSet(
        sourceSetName: String,
        directory: Directory,
    ) {
        require(sourceSetName.isNotBlank()) {
            "Compose Resources source set names cannot be blank"
        }
        sourceSetDirectoryConfigurators[sourceSetName] = { property -> property.set(directory) }
    }

    internal fun configureDirectoryFor(
        sourceSetName: String,
        property: DirectoryProperty,
    ) {
        sourceSetDirectoryConfigurators[sourceSetName]?.invoke(property)
    }
}
