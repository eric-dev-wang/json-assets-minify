# JSON Assets Minify

[中文文档](README_zh.md)

A Gradle plugin that automatically minifies JSON resource files in Android projects by removing unnecessary whitespace and line breaks to reduce APK size.

## Features

- � ***Automatic Minification** - Automatically minifies all JSON files in the `assets` directory during build
- 🎯 **Build Type Filtering** - Configure to enable minification only for specific build types (e.g., release)
- � ***File Ignore Patterns** - Flexibly ignore specific files or directories using glob patterns
- 🔧 **Seamless Integration** - Automatically integrates into the Android build process without manual task configuration
- 📦 **Dual Module Support** - Supports both Android Application and Library modules
- 🌍 **Compose Multiplatform Resources** - Supports JSON minification for every discovered Kotlin source set, including `commonMain`, intermediate source sets, Android, iOS, and JVM/Desktop resources
- ✅ **Safe and Reliable** - Uses kotlinx-serialization to ensure JSON structure integrity

## Quick Start

### 1. Add Plugin

Configure the plugin repository in your project's root `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Apply the plugin in your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("com.ericdevwang.jsonassetsminify") version "0.4.0"
}
```

### 2. Configure Plugin (Optional)

```kotlin
jsonAssetsMinify {
    // Disable JSON minification for specific build types
    disabledBuildTypes("debug")
    
    // Ignore specific files or directories
    ignoredFiles(
        "test.json",           // Ignore specific file
        "config/*.json",       // Ignore all JSON files in config directory
        "**/debug/**"          // Ignore all debug directories
    )
}
```

### Compose Multiplatform Resources

When the module applies both the Kotlin Multiplatform and Compose Multiplatform plugins, JSON files in each discovered source set's `composeResources` directory are processed automatically. The plugin does not assume platform source-set names, so custom hierarchies and intermediate source sets are supported.

The default input directory for a source set is:

```text
src/<sourceSetName>/composeResources
```

To use a different input directory for a source set, configure it explicitly:

```kotlin
jsonAssetsMinify {
    composeResources.sourceSet(
        "commonMain",
        layout.projectDirectory.dir("shared-resources"),
    )
}
```

The provider form is also available for lazily calculated directories:

```kotlin
jsonAssetsMinify {
    composeResources.sourceSet(
        "desktopCommonMain",
        provider { layout.projectDirectory.dir("desktop-resources") },
    )
}
```

Only JSON files under `composeResources/files` are minified. Non-JSON resources, ignored files, and invalid JSON are copied unchanged. Generated output is registered as Compose Resources' source-set directory, so the minified content is consumed by Android, iOS, and JVM/Desktop resource packaging.

If the project uses Compose Multiplatform's `compose.resources.customDirectory`, use the `jsonAssetsMinify.composeResources.sourceSet(...)` configuration for that source set instead. The plugin must own the generated output directory in order to guarantee that downstream resource tasks consume the minified files.

### 3. Build Project

```bash
./gradlew assembleRelease
```

The plugin will automatically minify JSON files during the build process.

## Configuration Options

### disabledBuildTypes

Disable JSON minification for specific build types.

```kotlin
jsonAssetsMinify {
    disabledBuildTypes("debug", "staging")
}
```

**Use Case:** Keep readable JSON format in development environment, minify only in production.

### ignoredFiles

Ignore specific files using glob patterns.

Patterns are matched against each feature's own relative path: Android patterns
are relative to the module's `assets` directory, while Compose Multiplatform
patterns are relative to the source set's `composeResources` directory.

```kotlin
jsonAssetsMinify {
    ignoredFiles(
        "test.json",              // Exact match
        "*.test.json",            // Wildcard match
        "**/test/**",             // Directory match
        "config/local.json"       // Path match
    )
}
```

**Glob Pattern Explanation:**
- `*` - Matches any characters within a single directory level
- `**` - Matches any characters across multiple directory levels
- `?` - Matches a single character

## How It Works

1. Plugin registers `minifyJsonAssets{Variant}` task in the Android build process
2. Task executes after `merge{Variant}Assets` completes
3. Reads JSON files from the merged assets directory (`build/intermediates/merged_assets/{variant}`)
4. Parses and minifies JSON using kotlinx-serialization
5. Modifies files in-place in the merged directory
6. Minified files are packaged into the final APK/AAR

## Example

### Minification Effect

**Before:**
```json
{
  "name": "example",
  "version": "1.0.0",
  "config": {
    "enabled": true,
    "timeout": 3000
  }
}
```

**After:**
```json
{"name":"example","version":"1.0.0","config":{"enabled":true,"timeout":3000}}
```

### Typical Configuration

```kotlin
android {
    // ... Android configuration
}

jsonAssetsMinify {
    // Only minify in release builds
    disabledBuildTypes("debug")
    
    // Ignore test and config files
    ignoredFiles(
        "**/*test*.json",
        "config/local.json"
    )
}
```

## Compatibility Requirements

### Consumer compatibility

- **Minimum supported AGP / Gradle pair**: AGP 9.1.1 / Gradle 9.3.1, retained as a functional-test regression target.
- **JDK**: 17+.
- **Compose Multiplatform Resources**: source-set discovery is dynamic; no platform source-set names are required. Android libraries use `com.android.kotlin.multiplatform.library`.
- Android API 37 is used by this repository's samples and compatibility tests; it is not a minimum `targetSdk` imposed by the plugin.

### Repository build and validation baseline

| Component | Version |
| --- | --- |
| Gradle | 9.8.2 |
| AGP | 9.4.1 |
| Build Kotlin / serialization compiler plugin | 2.4.10 (aligned with Gradle's embedded Kotlin) |
| Kotlin Multiplatform / Compose Compiler | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| kotlinx-serialization | 1.12.0 |
| Gradle Plugin Publish | 2.2.1 |
| JUnit Jupiter | 6.1.3 |

Java compilation targets JDK 17. The Gradle daemon is pinned to Amazon JDK 21 via `updateDaemonJvm`. Android samples use `compileSdk = 37` and the app uses `targetSdk = 37`. Use an Android Studio version supporting AGP 9.4, such as Quail 4 (2026.1.4) or newer, when opening the samples.

Gradle 9.8.2 includes the public security fixes introduced in 9.8.1. KGP 2.4.21's [fully supported range](https://kotlinlang.org/docs/gradle-configure-project.html) currently ends at Gradle 9.7.0 and AGP 9.3.1. The newer repository baseline is therefore covered by functional consumer-build tests rather than claimed to be within that range. The previous AGP 9.3.0 / Gradle 9.5.0 pair is also retained as a regression target. Older Gradle pairs are compatibility targets, not recommended build-tool versions.

The functional tests inspect Android APK/AAR files, the Desktop JAR, and generated iOS hierarchical resources. Resource generation does not establish iOS native compilation or runtime compatibility.

## License

See [LICENSE](LICENSE) file for details.

## Author

Eric Wang ([@eric-dev-wang](https://github.com/eric-dev-wang))
