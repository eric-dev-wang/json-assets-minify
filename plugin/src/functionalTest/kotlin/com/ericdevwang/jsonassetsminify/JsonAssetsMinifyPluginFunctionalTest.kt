package com.ericdevwang.jsonassetsminify

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.stream.Stream
import java.util.zip.ZipFile
import kotlin.test.Test

/**
 * Functional tests for the JSON Assets Minify plugin.
 * Tests compatibility across multiple AGP and Gradle versions.
 */
class JsonAssetsMinifyPluginFunctionalTest {

    companion object {
        private const val testProjectPath = "../samples"

        /**
         * Test matrix for AGP and Gradle version combinations.
         * Format: AGP version, Gradle version, compileSdk
         *
         * Android 37 compatibility matrix: the minimum AGP/Gradle pair and the current project baseline.
         */
        @JvmStatic
        fun versionMatrix(): Stream<Arguments> = Stream.of(
            // AGP 9.1.1 + Gradle 9.3.1 - Minimum supported pair for Android API 37.
            Arguments.of("9.1.1", "9.3.1", 37),
            // AGP 9.3.0 + Gradle 9.5.0 - Current project baseline for Android API 37.
            Arguments.of("9.3.0", "9.5.0", 37)
        )

        @BeforeAll
        @JvmStatic
        fun publishPluginToMavenLocal() {
            println("Publishing plugin to MavenLocal...")
            "./gradlew :plugin:publishToMavenLocal --stacktrace".runCommand(File("../"))
            println("Plugin published to MavenLocal.")
        }
    }

    @ParameterizedTest(name = "AGP {0} with Gradle {1}")
    @MethodSource("versionMatrix")
    fun `plugin works with different AGP and Gradle versions`(
        agpVersion: String,
        gradleVersion: String,
        compileSdk: Int
    ) {
        // Prepare test project
        val testName = "agp-${agpVersion.replace(".", "-")}-gradle-${gradleVersion.replace(".", "-")}"
        val targetProjectDir = prepareTestProject(testName, agpVersion, compileSdk)

        println("Testing AGP $agpVersion with Gradle $gradleVersion...")

        // Run the build with the plugin applied (use release to test minification)
        val result = try {
            GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .forwardOutput()
                .withArguments("clean", "assembleRelease", "--stacktrace")
                .withProjectDir(targetProjectDir)
                .withDebug(false)
                .build()
        } catch (e: Exception) {
            println("Build failed with error: ${e.message}")
            println("Check build output above for details")
            throw e
        }
        
        // Verify the build was successful
        assert(result.output.contains("BUILD SUCCESSFUL")) {
            "Build should complete successfully for AGP $agpVersion with Gradle $gradleVersion"
        }
        
        println("✓ AGP $agpVersion with Gradle $gradleVersion test passed")
    }

    @Test
    fun `plugin minifies JSON files correctly`() {
        // Prepare test project
        val targetProjectDir = prepareTestProject("minify-validation", null, null)

        println("\n=== Testing JSON Minification Functionality ===\n")

        // Build all variants
        println("Building all variants...")
        GradleRunner.create()
            .withGradleVersion("9.5.0")
            .forwardOutput()
            .withArguments(
                "clean",
                "app:assembleDebug",
                "app:assembleRelease",
                "lib:assembleFreeDebug",
                "lib:assemblePaidDebug",
                "lib:assembleFreeRelease",
                "lib:assemblePaidRelease",
                "--stacktrace"
            )
            .withProjectDir(targetProjectDir)
            .build()

        println("\n=== Verifying App Module ===\n")
        
        // App module: All build types should minify (no disabledBuildTypes)
        verifyAppModule(targetProjectDir, "debug")
        verifyAppModule(targetProjectDir, "release")

        println("\n=== Verifying Lib Module ===\n")
        
        // Lib module: Debug disabled, Release enabled
        verifyLibModule(targetProjectDir, "freeDebug", shouldMinify = false)
        verifyLibModule(targetProjectDir, "paidDebug", shouldMinify = false)
        verifyLibModule(targetProjectDir, "freeRelease", shouldMinify = true)
        verifyLibModule(targetProjectDir, "paidRelease", shouldMinify = true)

        println("\n=== Verifying Flavor Consistency ===\n")

        // Verify that different flavors produce identical minification results
        verifyFlavorConsistency(targetProjectDir)

        println("\n✓ All minification tests passed!")
    }

    @Test
    fun `plugin minifies Compose Multiplatform resources for custom Android iOS and Desktop hierarchy`() {
        val targetProjectDir = prepareTestProject("compose-multiplatform-validation", null, null)

        buildComposeMultiplatformSample(targetProjectDir)
        verifyComposeMultiplatformOutputs(targetProjectDir)
    }

    @Test
    fun `plugin minifies Compose Multiplatform resources with default hierarchy`() {
        val targetProjectDir = prepareTestProject(
            "compose-multiplatform-default-hierarchy-validation",
            null,
            null,
        )
        configureDefaultHierarchySample(targetProjectDir)

        buildComposeMultiplatformSample(targetProjectDir)
        verifyComposeMultiplatformOutputs(targetProjectDir)
    }

    private fun buildComposeMultiplatformSample(projectDir: File) {
        GradleRunner.create()
            .withGradleVersion("9.5.0")
            .forwardOutput()
            .withArguments(
                "clean",
                ":kmp:bundleAndroidMainAar",
                ":kmp:iosSimulatorArm64CopyHierarchicalMultiplatformResources",
                ":kmp:desktopJar",
                "--stacktrace",
            )
            .withProjectDir(projectDir)
            .build()
    }

    private fun verifyComposeMultiplatformOutputs(targetProjectDir: File) {

        val aarFile = targetProjectDir.resolve("kmp/build/outputs/aar/kmp.aar")
        assert(aarFile.exists()) { "KMP Android AAR should exist: ${aarFile.absolutePath}" }
        ZipFile(aarFile).use { zip ->
            assertZipSuffixIsMinified(zip, "/files/common.json", "commonMain JSON should be minified in the AAR")
            assertZipSuffixIsNotMinified(
                zip,
                "/files/ignored.json",
                "ignored Compose Resource JSON should remain formatted in the AAR",
            )
            assertZipSuffixExists(zip, "/files/readme.txt", "non-JSON Compose Resource should be packaged in the AAR")
        }

        val desktopJar = targetProjectDir.resolve("kmp/build/libs/kmp-desktop.jar")
        assert(desktopJar.exists()) { "KMP Desktop JAR should exist: ${desktopJar.absolutePath}" }
        JarFile(desktopJar).use { jar ->
            assertZipSuffixIsMinified(jar, "/files/common.json", "commonMain JSON should be minified in the Desktop JAR")
            assertZipSuffixIsMinified(
                jar,
                "/files/desktop.json",
                "desktopCommonMain JSON should be minified in the Desktop JAR",
            )
        }

        val iosResource = targetProjectDir.resolve(
            "kmp/build/kotlin-multiplatform-resources/assemble-hierarchically/iosSimulatorArm64",
        ).walkTopDown().firstOrNull { it.isFile && it.path.endsWith("/files/common.json") }
        assert(iosResource != null) { "iOS Simulator resources should contain common.json" }
        assert(!iosResource!!.readText().contains("\n")) {
            "commonMain JSON should be minified in iOS Simulator resources"
        }
    }

    private fun configureDefaultHierarchySample(projectDir: File) {
        projectDir.resolve("gradle.properties").apply {
            writeText(readText().replace(
                "kotlin.mpp.applyDefaultHierarchyTemplate=false",
                "# Default Kotlin hierarchy template enabled for this test",
            ))
        }

        projectDir.resolve("kmp/build.gradle.kts").apply {
            val content = readText()
            val customHierarchyBlock = Regex(
                "(?s)\\s*// BEGIN_CUSTOM_HIERARCHY.*?// END_CUSTOM_HIERARCHY\\s*",
            )
            check(customHierarchyBlock.containsMatchIn(content)) {
                "The KMP sample custom hierarchy block should be present"
            }
            writeText(customHierarchyBlock.replace(content, "\n"))
        }

        val customResources = projectDir.resolve("kmp/src/desktopCommonMain/composeResources")
        val defaultResources = projectDir.resolve("kmp/src/desktopMain/composeResources")
        check(customResources.isDirectory) {
            "The KMP sample should provide custom-hierarchy Desktop resources"
        }
        customResources.copyRecursively(defaultResources, overwrite = true)
        customResources.deleteRecursively()
    }

    private fun verifyAppModule(projectDir: File, buildType: String) {
        println("Verifying app module - $buildType variant...")
        
        // 1. Verify APK exists (try both signed and unsigned names)
        val apkFile = projectDir.resolve("app/build/outputs/apk/$buildType/app-$buildType.apk")
            .takeIf { it.exists() }
            ?: projectDir.resolve("app/build/outputs/apk/$buildType/app-$buildType-unsigned.apk")
        
        assert(apkFile.exists()) {
            "APK should exist for $buildType: ${apkFile.absolutePath}"
        }
        
        // 2. Extract and verify files from APK
        ZipFile(apkFile).use { zip ->
            // Should be minified
            assertFileIsMinified(zip, "assets/basic_formatted.json", 
                "basic_formatted.json should be minified in app $buildType")
            assertFileIsMinified(zip, "assets/nested/deep/level2.json",
                "nested/deep/level2.json should be minified in app $buildType")
            
            // Should NOT be minified (ignoredFiles)
            assertFileIsNotMinified(zip, "assets/should_be_ignored.json",
                "should_be_ignored.json should NOT be minified (ignoredFiles) in app $buildType")
            assertFileIsNotMinified(zip, "assets/nested/level1.json",
                "nested/level1.json should NOT be minified (ignoredFiles) in app $buildType")
            
            // Non-JSON files should exist unchanged
            val txtEntry = zip.getEntry("assets/non_json_file.txt")
            assert(txtEntry != null) {
                "non_json_file.txt should exist in APK"
            }
        }
        
        println("  ✓ App $buildType variant verified")
    }

    private fun verifyLibModule(projectDir: File, variant: String, shouldMinify: Boolean) {
        println("Verifying lib module - $variant variant (shouldMinify=$shouldMinify)...")
        
        // 1. Verify AAR exists (convert camelCase variant to kebab-case filename)
        val aarFileName = variantToAarFileName(variant)
        val aarFile = projectDir.resolve("lib/build/outputs/aar/lib-$aarFileName.aar")
        assert(aarFile.exists()) {
            "AAR should exist for $variant: ${aarFile.absolutePath}"
        }
        
        // 2. Extract and verify files from AAR
        ZipFile(aarFile).use { zip ->
            if (shouldMinify) {
                // Release: Should be minified (except ignored files)
                assertFileIsMinified(zip, "assets/basic_formatted.json",
                    "basic_formatted.json should be minified in lib $variant")
                assertFileIsMinified(zip, "assets/nested/deep/level2.json",
                    "nested/deep/level2.json should be minified in lib $variant")
                
                // Should NOT be minified (ignoredFiles)
                assertFileIsNotMinified(zip, "assets/nested/level1.json",
                    "nested/level1.json should NOT be minified (ignoredFiles) in lib $variant")
            } else {
                // Debug: Nothing should be minified (disabledBuildTypes)
                assertFileIsNotMinified(zip, "assets/basic_formatted.json",
                    "basic_formatted.json should NOT be minified (disabledBuildTypes) in lib $variant")
                assertFileIsNotMinified(zip, "assets/nested/deep/level2.json",
                    "nested/deep/level2.json should NOT be minified (disabledBuildTypes) in lib $variant")
                assertFileIsNotMinified(zip, "assets/nested/level1.json",
                    "nested/level1.json should NOT be minified in lib $variant")
            }
            
            // Non-JSON files should exist unchanged
            val txtEntry = zip.getEntry("assets/non_json_file.txt")
            assert(txtEntry != null) {
                "non_json_file.txt should exist in AAR"
            }
        }
        
        println("  ✓ Lib $variant variant verified")
    }

    private fun verifyFlavorConsistency(projectDir: File) {
        println("Verifying that freeRelease and paidRelease produce identical minification...")
        
        val freeReleaseAar = projectDir.resolve("lib/build/outputs/aar/lib-free-release.aar")
        val paidReleaseAar = projectDir.resolve("lib/build/outputs/aar/lib-paid-release.aar")
        
        val filesToCompare = listOf(
            "assets/basic_formatted.json",
            "assets/nested/deep/level2.json"
        )
        
        filesToCompare.forEach { assetPath ->
            val freeContent = extractFileFromZip(freeReleaseAar, assetPath)
            val paidContent = extractFileFromZip(paidReleaseAar, assetPath)
            
            assert(freeContent == paidContent) {
                "$assetPath should be identical in freeRelease and paidRelease"
            }
            
            println("  ✓ $assetPath is identical across flavors")
        }
    }

    private fun assertFileIsMinified(zip: ZipFile, entryPath: String, message: String) {
        val content = extractFileFromZip(zip, entryPath)
        assert(!content.contains("\n")) {
            "$message (file should not contain newlines)"
        }
    }

    private fun assertFileIsNotMinified(zip: ZipFile, entryPath: String, message: String) {
        val content = extractFileFromZip(zip, entryPath)
        assert(content.contains("\n")) {
            "$message (file should contain newlines and formatting)"
        }
    }

    private fun assertZipSuffixIsMinified(zip: ZipFile, suffix: String, message: String) {
        val content = extractFileBySuffix(zip, suffix, message)
        assert(!content.contains("\n")) {
            "$message (file should not contain newlines)"
        }
    }

    private fun assertZipSuffixIsNotMinified(zip: ZipFile, suffix: String, message: String) {
        val content = extractFileBySuffix(zip, suffix, message)
        assert(content.contains("\n")) {
            "$message (file should contain newlines and formatting)"
        }
    }

    private fun assertZipSuffixExists(zip: ZipFile, suffix: String, message: String) {
        zip.entries().asSequence().firstOrNull { it.name.endsWith(suffix) }
            ?: error("$message: no ZIP entry ends with $suffix")
    }

    private fun extractFileBySuffix(zip: ZipFile, suffix: String, message: String): String {
        val entry = zip.entries().asSequence().firstOrNull { it.name.endsWith(suffix) }
            ?: error("$message: no ZIP entry ends with $suffix")
        return zip.getInputStream(entry).bufferedReader().use { it.readText() }
    }

    private fun extractFileFromZip(zipFile: File, entryPath: String): String {
        return ZipFile(zipFile).use { zip ->
            extractFileFromZip(zip, entryPath)
        }
    }

    private fun extractFileFromZip(zip: ZipFile, entryPath: String): String {
        val entry = zip.getEntry(entryPath)
        assert(entry != null) {
            "Entry $entryPath should exist in ${zip.name}"
        }
        return zip.getInputStream(entry).bufferedReader().use { it.readText() }
    }

    private fun prepareTestProject(testName: String, agpVersion: String?, compileSdk: Int?): File {
        val targetProjectDir = File("./build/functional-test/$testName").absoluteFile.apply {
            deleteRecursively()
            mkdirs()
        }

        // Copy samples project to build directory
        val testProjectDir = File(testProjectPath)
        testProjectDir.copyRecursively(targetProjectDir) { file, exception ->
            // Skip .gradle and build directories to avoid copying build artifacts
            if (file.path.contains("/.gradle/") || file.path.contains("/build/")) {
                OnErrorAction.SKIP
            } else {
                throw exception
            }
        }
        
        // Copy and optionally modify libs.versions.toml
        targetProjectDir.resolve("gradle/libs.versions.toml").apply {
            parentFile.mkdirs()
            val mainVersionsFile = File("../gradle/libs.versions.toml")
            val content = mainVersionsFile.readText()
            val updatedContent = if (agpVersion != null) {
                content.replace(
                    Regex("""agp = "[^"]+""""),
                    """agp = "$agpVersion""""
                )
            } else {
                content
            }
            writeText(updatedContent)
        }
        
        // Update settings.gradle.kts to disable composition build
        targetProjectDir.resolve("settings.gradle.kts").apply {
            val content = readText()
            val updatedContent = removeVersionCatalogsBlock(content)
                .replace(
                    "val enabledCompositionBuild = true",
                    "val enabledCompositionBuild = false"
                )
            writeText(updatedContent)
        }

        // Update compileSdk if specified
        if (compileSdk != null) {
            targetProjectDir.resolve("app/build.gradle.kts").apply {
                val content = readText()
                val updatedContent = content
                    .replace(Regex("""compileSdk = \d+"""), "compileSdk = $compileSdk")
                    .replace(Regex("""targetSdk = \d+"""), "targetSdk = $compileSdk")
                writeText(updatedContent)
            }

            targetProjectDir.resolve("lib/build.gradle.kts").apply {
                val content = readText()
                val updatedContent = content
                    .replace(Regex("""compileSdk = \d+"""), "compileSdk = $compileSdk")
                writeText(updatedContent)
            }
        }

        return targetProjectDir
    }

    private fun removeVersionCatalogsBlock(content: String): String {
        val versionCatalogsStart = content.indexOf("versionCatalogs {")
        if (versionCatalogsStart < 0) return content
        
        var braceCount = 0
        var index = versionCatalogsStart + "versionCatalogs {".length
        while (index < content.length) {
            when (content[index]) {
                '{' -> braceCount++
                '}' -> {
                    if (braceCount == 0) {
                        index++
                        break
                    }
                    braceCount--
                }
            }
            index++
        }
        
        return content.take(versionCatalogsStart) + content.substring(index)
    }

    /**
     * Convert camelCase variant name to kebab-case AAR filename.
     * Example: freeDebug -> free-debug, paidRelease -> paid-release
     */
    private fun variantToAarFileName(variant: String): String {
        return variant.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()
    }
}

private fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(15, TimeUnit.MINUTES)
}
