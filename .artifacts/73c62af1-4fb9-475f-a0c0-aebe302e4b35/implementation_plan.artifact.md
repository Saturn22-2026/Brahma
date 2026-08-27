# Fix Gradle Sync Error: "Cannot add extension with name 'kotlin'"

The project is experiencing a Gradle sync failure in the `:wallet-android:app` module. The error `Cannot add extension with name 'kotlin', as there is an extension already registered with that name` indicates a conflict between Kotlin plugins, likely due to mixing `kotlin-android` and `kotlin-multiplatform` in the same project hierarchy or a double-application of the Kotlin plugin.

## Proposed Changes

### `:wallet-android:app`

I will convert the `:wallet-android:app` module to use the `kotlin("multiplatform")` plugin with an `androidTarget()`. This ensures consistency with the `:wallet-android:shared` and `:wallet-android:composeApp` modules which already use the multiplatform plugin. This is a recommended approach in Kotlin Multiplatform projects to avoid extension name conflicts between `kotlin-android` and `kotlin-multiplatform`.

#### [MODIFY] [build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/build.gradle.kts)
- Replace `id("org.jetbrains.kotlin.android")` with `kotlin("multiplatform")`.
- Add a `kotlin { androidTarget() }` block to configure the Android target.
- Move `kotlinOptions` configuration into the `kotlin.androidTarget` block for consistency, although keeping it in `android` block is also possible, the `kotlin` DSL is preferred in KMP projects.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify the error is resolved.
- Run `:wallet-android:app:assembleDebug` to ensure the project still builds correctly.

### Manual Verification
- Verify that the IDE recognizes Kotlin code in the `app` module.
