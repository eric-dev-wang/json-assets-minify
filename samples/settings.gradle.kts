pluginManagement {
    val enabledCompositionBuild = true

    if (enabledCompositionBuild) {
        includeBuild("..")
    }

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.ericdevwang.jsonassetsminify") {
                // It will be replaced by a local module using `includeBuild` above,
                // thus we just put a generic version (+) here.
                useModule("com.ericdevwang:jsonassetsminify:+")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "json-assets-minify-sample"
include(":app", ":lib")
