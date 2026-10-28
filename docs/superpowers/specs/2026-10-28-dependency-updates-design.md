# Dependency update design

Approved in conversation on 2026-10-28: update stable dependencies in an isolated worktree and open a PR.

## Intent and constraints

Update the repository toolchain and dependencies without changing JSON minification or public DSL behavior. Keep Java bytecode at 17, the Amazon JDK 21 daemon, Android API 37, and plugin release version 0.4.0. Preserve the AGP 9.1.1 / Gradle 9.3.1 consumer compatibility floor if real functional tests pass; report rather than silently raise it if they fail.

## Approaches

- Selected: latest stable Gradle 9.8.2 and AGP 9.4.1, validated by consumer builds. This includes the public Gradle security fixes but exceeds KGP 2.4.21's fully supported Gradle 9.7.0 / AGP 9.3.1 range.
- Alternative: update Gradle and libraries while retaining the AGP 9.3 line, reducing Android migration scope. This does not remove the KGP/Gradle support-range caveat.

## Version decisions

Use embedded/build Kotlin 2.4.10 with Gradle 9.8.2; use Kotlin Multiplatform and Compose Compiler 2.4.21; Compose Multiplatform 1.12.2; kotlinx-serialization 1.12.0; Plugin Publish 2.2.1; JUnit Jupiter 6.1.3. Keep Foojay 1.0.0, JUnit 4.13.2, AndroidX Test core/runner 1.7.0 and ext-junit 1.3.0.

Update both complete Gradle wrappers. Update checkout v6 to v7, setup-java v5 to v6 and upload-artifact v6 to v7; retain gradle/actions v6 and action-gh-release v3. Keep publishing a separate authorized action, outside this PR task.

## Verification

Run existing unit tests, functional tests, Gradle plugin validation, plugin build and publication POM generation. Functional tests must exercise the old minimum, previous baseline and new baseline; inspect APK/AAR/JAR and hierarchical iOS resources. iOS resource generation does not establish native compilation or runtime behavior. Check configuration cache on a representative sample task. Local checks do not establish hosted CI success.

## Implementation discretion

Centralize functional-test version values, retain scoped fixes required by newer APIs, and correct documentation that conflates example SDK values with consumer requirements. Avoid unrelated cleanup.
