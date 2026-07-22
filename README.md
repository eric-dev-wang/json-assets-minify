# JSON Assets Minify

[中文文档](README_zh.md)

A Gradle plugin that automatically minifies JSON resource files in Android projects by removing unnecessary whitespace and line breaks to reduce APK size.

## Features

- � ***Automatic Minification** - Automatically minifies all JSON files in the `assets` directory during build
- 🎯 **Build Type Filtering** - Configure to enable minification only for specific build types (e.g., release)
- � ***File Ignore Patterns** - Flexibly ignore specific files or directories using glob patterns
- 🔧 **Seamless Integration** - Automatically integrates into the Android build process without manual task configuration
- 📦 **Dual Module Support** - Supports both Android Application and Library modules
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
    id("com.ericdevwang.jsonassetsminify") version "0.2.2"
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

### Plugin Compatibility
- **Minimum AGP Version**: 9.1.1+ (supports Android API 37)
- **Minimum Gradle Version**: 9.3.1+
- **Kotlin Version**: 2.3.20 (build logic and serialization compiler plugin)
- **kotlinx-serialization**: 1.11.0
- **Gradle Plugin Publish**: 2.1.1
- **JUnit Jupiter**: 6.0.2
- **JDK Version**: 17+; the Gradle daemon is pinned to Amazon JDK 21 via `updateDaemonJvm`
- **Android Studio**: Panda 3 (2025.3.3 Patch 1) or higher
- **Minimum targetSdk**: 37 (Android 17)

### Why Choose AGP 9.3.0 as the Project Baseline?

According to [Google Play Store Policy](https://support.google.com/googleplay/android-developer/answer/11926878):
- **Starting August 31, 2025**, all new apps and updates must have targetSdk >= 35 (Android 15)
- **Starting in 2026**, all new apps and updates should target the latest API level
- AGP 9.3.0 supports Android API 37 and uses Gradle 9.5.0 as its matching Gradle version
- AGP 9.0.0 and earlier do not support Android API 37
- AGP 9.x includes built-in Kotlin support, simplifying project configuration
- SDK Build Tools 36.0.0 provides the latest Android development features

## License

See [LICENSE](LICENSE) file for details.

## Author

Eric Wang ([@eric-dev-wang](https://github.com/eric-dev-wang))
