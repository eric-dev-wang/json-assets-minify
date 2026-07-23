plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
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
                implementation(libs.compose.runtime)
                implementation(libs.compose.components.resources)
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
