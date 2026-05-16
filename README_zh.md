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
    id("com.ericdevwang.jsonassetsminify") version "0.2.2"
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
2. 任务在 `merge{Variant}Assets` 完成后执行
3. 从合并的资源目录（`build/intermediates/merged_assets/{variant}`）读取 JSON 文件
4. 使用 kotlinx-serialization 解析并压缩 JSON
5. 在合并目录中原地修改文件
6. 压缩后的文件被打包到最终的 APK/AAR 中

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
- **最低 AGP 版本**: 9.0.0+ (默认支持 SDK Build Tools 36.0.0)
- **最低 Gradle 版本**: 9.1.0+
- **Kotlin 版本**: 2.2.10+ (AGP 9.0 内置)
- **JDK 版本**: 17+ (通过 Gradle Toolchain 自动下载，无需手动安装)
- **Android Studio**: Koala Feature Drop (2025.1.1) 或更高版本
- **最低 targetSdk**: 36 (Android 16) - 符合 Google Play Store 要求

### 为什么选择 AGP 9.0.0 作为最低版本？

- AGP 9.0.0 是最新的主要版本，性能更优，使用新的 DSL 接口
- AGP 9.0.0 内置 Kotlin 支持，简化项目配置
- SDK Build Tools 36.0.0 提供最新的 Android 开发特性

## 许可证

查看 [LICENSE](LICENSE) 文件了解详情。

## 作者

Eric Wang ([@eric-dev-wang](https://github.com/eric-dev-wang))
