plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.ericdevwang.jsonassetsminify")
}

kotlin {
    android {
        namespace = "com.ericdevwang.jsonassetsminify.sample.kmp"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
                implementation("org.jetbrains.compose.components:components-resources:1.11.1")
            }
        }

        // Deliberately use an intermediate source set to verify that the plugin
        // does not rely on a fixed list of platform source-set names.
        val desktopCommonMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(desktopCommonMain)
        }
    }
}

jsonAssetsMinify {
    ignoredFiles("files/ignored.json")
}
