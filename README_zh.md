# JSON Assets Minify

[English Documentation](README.md)

一个用于自动压缩 Android 项目中 JSON 资源文件的 Gradle 插件，通过移除不必要的空格和换行来减小 APK 体积。

## 功能特性

- 🚀 **自动压缩** - 在构建过程中自动压缩 `assets` 目录下的所有 JSON 文件
- 🎯 **构建类型过滤** - 可配置仅在特定构建类型（如 release）中启用压缩
- 📁 **文件忽略模式** - 支持 glob 模式灵活忽略特定文件或目录
- 🔧 **无缝集成** - 自动集成到 Android 构建流程，无需手动配置任务依赖
- 📦 **双模块支持** - 同时支持 Android Application 和 Library 模块
- 🌍 **Compose Multiplatform 资源** - 支持发现并处理所有 Kotlin source set，包括 `commonMain`、自定义 intermediate source set、Android、iOS 和 JVM/Desktop 资源
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
    id("com.ericdevwang.jsonassetsminify") version "0.3.0"
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

### Compose Multiplatform 资源

当模块同时应用 Kotlin Multiplatform 和 Compose Multiplatform 插件时，插件会动态发现每个 Kotlin source set，并处理其 `composeResources` 目录中的 JSON 文件。不依赖固定的平台 source set 名称，因此可以支持自定义 hierarchy 和 intermediate source set。

source set 的默认输入目录为：

```text
src/<sourceSetName>/composeResources
```

如果某个 source set 使用其他目录，可以显式配置：

```kotlin
jsonAssetsMinify {
    composeResources.sourceSet(
        "commonMain",
        layout.projectDirectory.dir("shared-resources"),
    )
}
```

也支持延迟计算目录的 Provider：

```kotlin
jsonAssetsMinify {
    composeResources.sourceSet(
        "desktopCommonMain",
        provider { layout.projectDirectory.dir("desktop-resources") },
    )
}
```

只有 `composeResources/files` 下的 JSON 文件会被压缩。非 JSON 资源、被忽略的文件和非法 JSON 都会原样复制。生成目录会注册为对应 source set 的 Compose Resources 目录，因此 Android、iOS 和 JVM/Desktop 的后续资源任务都会消费压缩后的内容。

如果项目使用 Compose Multiplatform 的 `compose.resources.customDirectory`，请改用 `jsonAssetsMinify.composeResources.sourceSet(...)` 配置该 source set。插件必须接管生成目录，才能保证下游资源任务使用压缩后的文件。

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
- **最低 AGP 版本**: 9.1.1+（支持 Android API 37）
- **最低 Gradle 版本**: 9.3.1+
- **Kotlin 版本**: 2.3.20（构建逻辑和 serialization 编译插件）
- **kotlinx-serialization**: 1.11.0
- **Gradle Plugin Publish**: 2.1.1
- **JUnit Jupiter**: 6.1.2
- **JDK 版本**: 17+；Gradle daemon 通过 `updateDaemonJvm` 固定使用 Amazon JDK 21
- **Android Studio**: Panda 3（2025.3.3 Patch 1）或更高版本
- **最低 targetSdk**: 37（Android 17）
- **Compose Multiplatform 资源**：已使用 Compose Multiplatform 插件 1.11.1 验证
- **Kotlin Multiplatform**：动态发现 source set，不要求固定的平台 source set 名称

### 为什么选择 AGP 9.3.0 作为项目基线？

- AGP 9.3.0 支持 Android API 37，并以 Gradle 9.5.0 作为匹配版本
- AGP 9.0.0 及更早版本不支持 Android API 37
- AGP 9.x 内置 Kotlin 支持，简化项目配置
- SDK Build Tools 36.0.0 提供最新的 Android 开发特性

## 许可证

查看 [LICENSE](LICENSE) 文件了解详情。

## 作者

Eric Wang ([@eric-dev-wang](https://github.com/eric-dev-wang))
