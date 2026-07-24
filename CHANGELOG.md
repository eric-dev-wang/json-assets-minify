# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-07-25

### Added
- Added JSON minification for Compose Multiplatform `composeResources`.
- Added dynamic source-set discovery that supports common, platform, intermediate, and custom hierarchy source sets.
- Added coverage for Android, iOS, and JVM/Desktop Compose Multiplatform resource packaging.
- Added support for Compose Multiplatform plugin version 1.11.1.

## [0.3.0] - 2026-07-22

### Changed
- **BREAKING**: Updated the Android build baseline to AGP 9.3.0 and Gradle 9.5.0 for Android API 37 support.
- **BREAKING**: Raised the minimum supported AGP version to 9.1.1 and Gradle version to 9.3.1.
- Upgraded Kotlin to 2.3.20 and kotlinx-serialization to 1.11.0.
- Upgraded Gradle Plugin Publish to 2.1.1 and JUnit Jupiter to 6.1.2.
- Updated the Gradle wrappers and configured the root and sample builds to use an Amazon JDK 21 Gradle daemon.
- Updated the sample application and library modules to compile against Android API 37.
- Added the sample test dependencies required by the existing JUnit 4 and AndroidX instrumentation tests.

### Fixed
- Marked the in-place JSON minification task as not cacheable so it passes Gradle 9.5 plugin validation.
- Updated functional-test project directories for Gradle 9.5 compatibility and expanded the AGP/Gradle compatibility matrix.

## [0.2.2] - 2026-05-16

### Fixed
- Fixed minified JSON files not being included in final APK/AAR by running minification after `merge{Variant}Assets` task instead of before
- Minification now modifies files in-place in the merged assets directory, ensuring minified content is packaged

### Changed
- Removed `addStaticSourceDirectory` approach which caused original files to take priority during merge
- Simplified task implementation by removing incremental build logic and separate output directory

## [0.2.1] - 2026-04-26

### Fixed
- Fixed lint task dependency wiring for AGP 9 / Gradle 9 by matching lint task names case-insensitively (for example `lintAnalyze*` and `lintVital*`), so lint tasks now explicitly depend on `minifyJsonAssets*` and no longer trigger Gradle implicit dependency validation errors.

## [0.2.0] - 2026-01-30

### Changed
- **BREAKING**: Upgraded minimum AGP version from 8.8.2 to 9.0.0
- **BREAKING**: Upgraded minimum Gradle version from 8.10.2 to 9.1.0
- **BREAKING**: Upgraded minimum Kotlin version from 2.0.21 to 2.2.10
- Updated to support AGP 9.0.0 new DSL interfaces (non-parameterized CommonExtension)
- Updated SDK Build Tools to 36.0.0

### Technical Details
- Minimum AGP version: 9.0.0
- Minimum Gradle version: 9.1.0
- Minimum Kotlin version: 2.2.10 (built-in with AGP 9.0)
- Target SDK: 36 (Android 16)
- Removed support for AGP 8.x to align with latest Android development standards

## [0.1.1] - 2026-01-06

### Fixed
- Corrected GitHub repository URL from `ericdevwang` to `eric-dev-wang` in plugin metadata
- Fixed JAR artifact naming from `plugin-0.1.0.jar` to `jsonassetsminify-0.1.1.jar`

### Changed
- Updated version to 0.1.1 for republishing after initial submission rejection

## [0.1.0] - 2026-01-03 [YANKED]

**Note:** This version was rejected during Gradle Plugin Portal submission due to incorrect repository URL.

### Added
- Initial release of JSON Assets Minify Gradle Plugin
- Automatic JSON minification for Android assets during build process
- Support for both Android Application and Library modules
- `disabledBuildTypes` configuration to disable minification for specific build types
- `ignoredFiles` configuration with glob pattern support to exclude files from minification
- Seamless integration with Android build pipeline (runs before asset merging)
- Comprehensive test coverage including unit tests and functional tests
- Multi-version compatibility testing (AGP 8.8.2+ and Gradle 8.10.2+)
- Support for nested directory structures in assets
- Automatic filtering of non-JSON files
- Build statistics reporting (files processed, size saved, compression ratio)

### Technical Details
- Minimum AGP version: 8.8.2
- Minimum Gradle version: 8.10.2
- Minimum Kotlin version: 2.0
- JDK version: 17+ (via Gradle Toolchain)
- Uses kotlinx-serialization for safe JSON parsing and minification

[0.4.0]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.4.0
[0.3.0]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.3.0
[0.2.2]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.2.2
[0.2.1]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.2.1
[0.2.0]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.2.0
[0.1.1]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.1.1
[0.1.0]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.1.0
