# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.1.1]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.1.1
[0.1.0]: https://github.com/eric-dev-wang/json-assets-minify/releases/tag/v0.1.0
