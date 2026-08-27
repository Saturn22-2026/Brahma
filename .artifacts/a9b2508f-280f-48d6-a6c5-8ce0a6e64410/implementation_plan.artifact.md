# Migrate to Built-in Kotlin for AGP 9.0+

The project is currently failing to sync because it uses the `org.jetbrains.kotlin.android` plugin which is no longer required and causes conflicts with Android Gradle Plugin 9.0's built-in Kotlin support.

## User Review Required

> [!IMPORTANT]
> This migration will enable built-in Kotlin support in AGP. If your project relies on specific behaviors of the legacy `kotlin-android` plugin that are not yet covered by built-in Kotlin, you might encounter other build issues. However, this is the recommended path for AGP 9.0+.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android") version "2.2.10" apply false`.

#### [MODIFY] [wallet-android/app/build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")` from the `plugins` block if present.
- Ensure only `id("com.android.application")` is used for Android application modules, as it now provides Kotlin support.

#### [MODIFY] [gradle.properties](file:///C:/GitHub/Bitcoin-Quantum/gradle.properties)
- Remove `android.builtInKotlin=false` and `android.newDsl=false` to enable the new built-in Kotlin and DSL features.

#### [MODIFY] [wallet-android/gradle.properties](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/gradle.properties)
- Remove `android.builtInKotlin=false` and `android.newDsl=false`.

## Verification Plan

### Manual Verification
1. Run Gradle Sync in Android Studio.
2. Verify that the `IllegalStateException` is resolved.
3. Build the `:app` module to ensure Kotlin compilation still works.
