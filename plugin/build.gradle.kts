plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
    alias(libs.plugins.gradle.plugin.publish)
}

group = "com.ericdevwang"
version = "0.2.0"

base {
    archivesName.set("jsonassetsminify")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter.params)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "testImplementation"(libs.android.gradlePlugin)
    "testRuntimeOnly"(libs.android.gradlePlugin)
}

gradlePlugin {
    website = "https://github.com/eric-dev-wang/json-assets-minify"
    vcsUrl = "https://github.com/eric-dev-wang/json-assets-minify"
    
    plugins {
        create("jsonassetsminify") {
            id = "com.ericdevwang.jsonassetsminify"
            implementationClass = "com.ericdevwang.jsonassetsminify.JsonAssetsMinifyConventionPlugin"
            displayName = "JSON Assets Minify"
            description = "Automatically minifies JSON files in Android assets to reduce APK size"
            tags = listOf("android", "json", "minify", "assets", "optimization", "apk-size")
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet =
    sourceSets.create("functionalTest") {
    }

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("jsonassetsminify") {
            from(components["java"])
            groupId = "com.ericdevwang"
            artifactId = "jsonassetsminify"
            version = "0.2.0"
        }
    }
}
