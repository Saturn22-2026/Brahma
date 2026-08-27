# Implementation Plan: Deploy Android Wallet to Device

This plan outlines the steps to build and deploy the `wallet-android` application to the connected Android device.

## User Review Required

> [!IMPORTANT]
> A physical device with serial **R92X30JKZTY** has been detected and will be used for deployment.
>
> The build process requires a Java 17+ runtime. I will use the Java runtime bundled with Android Studio (`C:\Program Files\Android\Android Studio2\jbr`).

## Proposed Changes

No changes to the source code are required for deployment. The focus is on the build and release pipeline.

### Environment Setup
- **JAVA_HOME**: Set to `C:\Program Files\Android\Android Studio2\jbr`.
- **PATH**: Include `platform-tools` for `adb` access.

### Build Process
- Run `./gradlew assembleDebug` in the `wallet-android` directory.
- This will compile both Kotlin code and the native C++ components (`mldsa_jni`).

### Deployment Process
- Use `adb install -r` to install the generated APK on the device.
- The target APK path is `wallet-android/app/build/outputs/apk/debug/app-debug.apk`.

### Execution
- Launch the app using `adb shell am start -n com.btq.wallet/.MainActivity`.

## Verification Plan

### Automated Verification
- Check the exit code of the Gradle build.
- Verify `adb install` returns "Success".

### Manual Verification
- The user should observe the Bitcoin-Quantum wallet dashboard on the device.
- Verify that the "Main Balance" (BTQ) and "L2 Assets" are displayed correctly.
