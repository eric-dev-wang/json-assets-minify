# JSON Assets Minify

[English Documentation](README.md)

一个用于自动压缩 Android 项目中 JSON 资源文件的 Gradle 插件，通过移除不必要的空格和换行来减小 APK 体积。

## 功能特性

- 🚀 **自动压缩** - 在构建过程中自动压缩 `assets` 目录下的所有 JSON 文件
- 🎯 **构建类型过滤** - 可配置仅在特定构建类型（如 release）中启用压缩
- 📁 **文件忽略模式** - 支持 glob 模式灵活忽略特定文件或目录
- 🔧 **无缝集成** - 自动集成到 Android 构建流程，无需手动配置任务依赖
- 📦 **双模块支持** - 同时支持 Android Application 和 Library 模块
- ✅ **安全可靠** - 使用 kotlinx-serialization 确保 JSON 结构完整性

## 快速开始

### 1. 添加插件

在项目根目录的 `settings.gradle.kts` 中配置插件仓库：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

在模块的 `build.gradle.kts` 中应用插件：

```kotlin
plugins {
    id("com.android.application")
    id("com.ericdevwang.jsonassetsminify") version "0.1.0"
}
```

### 2. 配置插件（可选）

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

### 3. 构建项目

```bash
./gradlew assembleRelease
```

插件会自动在构建过程中压缩 JSON 文件。

## 配置选项

### disabledBuildTypes

禁用特定构建类型的 JSON 压缩。

```kotlin
jsonAssetsMinify {
    disabledBuildTypes("debug", "staging")
}
```

**使用场景：** 在开发环境保留可读的 JSON 格式，仅在生产环境压缩。

### ignoredFiles

使用 glob 模式忽略特定文件。

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

**Glob 模式说明：**
- `*` - 匹配单个目录层级内的任意字符
- `**` - 匹配跨多个目录层级的任意字符
- `?` - 匹配单个字符

## 工作原理

1. 插件在 Android 构建流程中注册 `minifyJsonAssets{Variant}` 任务
2. 任务在 `merge{Variant}Assets` 之前执行
3. 扫描 `assets` 目录下的所有 `.json` 文件
4. 使用 kotlinx-serialization 解析并压缩 JSON
5. 将压缩后的文件输出到 `build/intermediates/minified_assets/{variant}`
6. 压缩后的文件自动替换原始文件参与后续构建

## 示例

### 压缩效果

**压缩前：**
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

**压缩后：**
```json
{"name":"example","version":"1.0.0","config":{"enabled":true,"timeout":3000}}
```

### 典型配置

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

## 兼容性要求

### 插件兼容性
- **最低 AGP 版本**: 8.8.2+ (默认支持 SDK Build Tools 35.0.0)
- **最高 AGP 版本**: 8.13+ (已测试)
- **最低 Gradle 版本**: 8.10.2+ (推荐 9.2.1+)
- **最低 Kotlin 版本**: 2.0+
- **JDK 版本**: 17+ (通过 Gradle Toolchain 自动下载，无需手动安装)
- **Android Studio**: Ladybug Feature Drop (2024.2.2) 或更高版本
- **最低 targetSdk**: 35 (Android 15) - 符合 Google Play Store 要求

### 为什么选择 AGP 8.8.2 作为最低版本？

根据 [Google Play Store 政策](https://support.google.com/googleplay/android-developer/answer/11926878)：
- **2025年8月31日起**，所有新应用和更新必须 targetSdk >= 35 (Android 15)
- AGP 8.6.0 是支持 API 35 的最低版本，但 SDK Build Tools 默认仍为 34.0.0
- **AGP 8.8.0** 是第一个默认使用 SDK Build Tools 35.0.0 的版本
- 选择 AGP 8.8.2 确保开箱即用支持 API 35，无需额外配置

## 许可证

查看 [LICENSE](LICENSE) 文件了解详情。

## 作者

Eric Wang ([@eric-dev-wang](https://github.com/eric-dev-wang))
