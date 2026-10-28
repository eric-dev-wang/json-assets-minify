# Dependency Updates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade stable dependencies, verify old/new consumer builds, and open a PR from an isolated worktree.

**Architecture:** Keep the existing Kotlin DSL Gradle plugin and Android/Compose resource integrations. Build Kotlin follows Gradle's embedded compiler; KMP samples and Compose Compiler use their own aligned version.

**Tech Stack:** Gradle, Kotlin, AGP, Compose Multiplatform, kotlinx-serialization, JUnit, GitHub Actions.

**Spec:** docs/superpowers/specs/2026-10-28-dependency-updates-design.md

## Global Constraints

- Gradle 9.8.2, AGP 9.4.1; build Kotlin 2.4.10; KMP/Compose Compiler 2.4.21.
- Compose 1.12.2; serialization 1.12.0; Plugin Publish 2.2.1; JUnit Jupiter 6.1.3.
- Java bytecode 17; Amazon JDK 21 daemon; Android API 37; plugin version 0.4.0.
- Preserve AGP 9.1.1 / Gradle 9.3.1 compatibility if functional tests establish it.
- No JSON algorithm or public DSL behavior changes; no release publication or merge.

## Review Focus

- New plugin bytecode and Kotlin dependencies must load in older Gradle consumer builds.
- Ignored, invalid and non-JSON resources must remain unchanged.
- Default and intermediate source-set hierarchies must package minified resources.
- Android build types and library flavors must retain existing behavior.
- CI must preserve version/POM validation and avoid triggering a release publication.

### Task 1: Toolchain and library updates

**Files:** gradle/libs.versions.toml; root and samples Gradle wrappers; plugin/build.gradle.kts (deprecated task-registration API).

- [x] Run the existing `:plugin:test` baseline and inspect results.
- [x] Update the catalog to the exact versions above and regenerate both complete wrappers, including distribution SHA-256 verification.
- [x] Run `:plugin:test :plugin:validatePlugins` and inspect compiler/plugin compatibility diagnostics.

### Task 2: Consumer verification

**Files:** plugin/src/functionalTest/kotlin/com/ericdevwang/jsonassetsminify/JsonAssetsMinifyPluginFunctionalTest.kt; samples/kmp/build.gradle.kts (deprecated source-set delegate APIs).

- [x] Centralize the current Gradle version; retain minimum and previous-baseline combinations and add AGP 9.4.1 / Gradle 9.8.2.
- [x] Use the new baseline for content checks covering APK/AAR/JAR/iOS resources and both source-set hierarchies.
- [x] Run `:plugin:check :plugin:assemble :plugin:generatePomFileForJsonassetsminifyPublication`; inspect XML test reports and POM dependencies.
- [x] Run a sample Compose resource task twice with configuration cache and inspect reuse.

### Task 3: CI, documentation and PR

**Files:** .github/workflows/ci.yml; .github/workflows/publish.yml; README.md; README_zh.md; CHANGELOG.md.

- [x] Update Actions major versions and document actual build versions separately from compatibility floors and sample SDK settings.
- [x] Inspect the complete diff and run `git diff --check`.
- [ ] Commit, obtain an independent review, fix material findings with appropriate verification, then push the feature branch and create/attach the PR.
- [ ] Report local validation and hosted CI status separately.
