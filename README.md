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
    id("com.ericdevwang.jsonassetsminify") version "0.1.0"
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
2. Task executes before `merge{Variant}Assets`
3. Scans all `.json` files in the `assets` directory
4. Parses and minifies JSON using kotlinx-serialization
5. Outputs minified files to `build/intermediates/minified_assets/{variant}`
6. Minified files automatically replace original files in subsequent build steps

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
- **Minimum AGP Version**: 8.8.2+ (default support for SDK Build Tools 35.0.0)
- **Maximum AGP Version**: 8.13+ (tested)
- **Minimum Gradle Version**: 8.10.2+ (recommended 9.2.1+)
- **Minimum Kotlin Version**: 2.0+
- **JDK Version**: 17+ (automatically downloaded via Gradle Toolchain, no manual installation required)
- **Android Studio**: Ladybug Feature Drop (2024.2.2) or higher
- **Minimum targetSdk**: 35 (Android 15) - complies with Google Play Store requirements

### Why Choose AGP 8.8.2 as Minimum Version?

According to [Google Play Store Policy](https://support.google.com/googleplay/android-developer/answer/11926878):
- **Starting August 31, 2025**, all new apps and updates must have targetSdk >= 35 (Android 15)
- AGP 8.6.0 is the minimum version supporting API 35, but SDK Build Tools defaults to 34.0.0
- **AGP 8.8.0** is the first version to default to SDK Build Tools 35.0.0
- Choosing AGP 8.8.2 ensures out-of-the-box support for API 35 without additional configuration

## License

See [LICENSE](LICENSE) file for details.

## Author

Eric Wang ([@ericdevwang](https://github.com/ericdevwang))
