plugins {
    alias(libs.plugins.android.application)
    id("com.ericdevwang.jsonassetsminify")
}

android {
    namespace = "com.ericdevwang.jsonassetsminify.sample.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ericwang.jsonassetsminify"
        minSdk = 33
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "1.0"
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
}

dependencies {
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

// Configure JSON minification
jsonAssetsMinify {
    ignoredFiles("should_be_ignored.json", "**/level1.json")
}
