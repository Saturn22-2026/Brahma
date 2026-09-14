# Fix Kotlin Extension Conflict (AGP 9.3.2 Built-in Kotlin)

The project is using Android Gradle Plugin (AGP) version 9.3.2, which introduces built-in Kotlin support. The error `Cannot add extension with name 'kotlin'` occurs because both AGP and the `org.jetbrains.kotlin.android` plugin attempt to register the `kotlin` extension.

## Proposed Changes

### Build Configuration

#### [MODIFY] [root build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/build.gradle.kts)
- Remove `org.jetbrains.kotlin.android` from the root plugins block.

#### [MODIFY] [app build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.
- Migrate `android.kotlinOptions` to the new `kotlin.compilerOptions` DSL (or remove if redundant with `compileOptions`).

#### [MODIFY] [shared build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/shared/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.

#### [MODIFY] [composeApp build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/composeApp/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.

## Verification Plan

### Automated Tests
- Run Gradle Sync to verify the error is resolved.
- Run `./gradlew :app:assembleDebug` to ensure compilation still works with built-in Kotlin.
