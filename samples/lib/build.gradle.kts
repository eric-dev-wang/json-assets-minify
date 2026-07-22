plugins {
    alias(libs.plugins.android.library)
    id("com.ericdevwang.jsonassetsminify")
}

android {
    namespace = "com.ericdevwang.jsonassetsminify.sample.app.lib"
    compileSdk = 37

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // add flavors
    flavorDimensions("type")
    productFlavors {
        create("free") {
            dimension = "type"
        }
        create("paid") {
            dimension = "type"
        }
    }
}

// Configure JSON minification
jsonAssetsMinify {
    disabledBuildTypes("debug")
    ignoredFiles("**/level1.json")
}
