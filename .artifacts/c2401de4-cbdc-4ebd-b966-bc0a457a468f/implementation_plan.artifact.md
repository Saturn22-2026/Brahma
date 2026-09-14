# Stress Test Implementation Plan

This plan outlines the steps to perform a comprehensive stress test of all core functions in the `Bitcoin-Quantum` wallet application, focusing on cryptographic operations, hardware security, and ViewModel logic.

## User Review Required

> [!IMPORTANT]
> The stress tests will be implemented as Android Instrumentation Tests (`androidTest`) because the core logic depends on JNI libraries (`mldsa_jni`) and Android-specific APIs like `AndroidKeyStore`. A connected device or emulator is required for execution.

## Proposed Changes

### [Android App]

#### [MODIFY] [build.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/build.gradle.kts)
- Add testing dependencies: `androidx.test.ext:junit`, `androidx.test:runner`, `androidx.test:rules`, and `kotlinx-coroutines-test`.

#### [NEW] [CryptoStressTest.kt](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/src/androidTest/java/com/btq/wallet/CryptoStressTest.kt)
- Implement stress tests for `CryptoManager`:
    - `generateMnemonic`: 1000 iterations to check for entropy and stability.
    - `deriveKeyPairFromMnemonic`: 1000 iterations to verify consistency.
    - `deriveAddress`: 1000 iterations.
    - `signMessage`: 1000 iterations with varying message sizes.

#### [NEW] [HardwareSecurityStressTest.kt](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/src/androidTest/java/com/btq/wallet/HardwareSecurityStressTest.kt)
- Implement stress tests for `HardwareSecurityManager`:
    - `signWithHardware`: 100 iterations (slower due to hardware interactions).
    - Rapid key generation and deletion cycles.

#### [NEW] [ViewModelStressTest.kt](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/src/androidTest/java/com/btq/wallet/ViewModelStressTest.kt)
- Implement stress tests for `WalletViewModel`:
    - Rapid state updates (StateFlow).
    - Simulated network interaction loops.
    - Wallet creation and recovery stress.

## Verification Plan

### Automated Tests
1.  Run the newly created instrumentation tests using Gradle:
    ```bash
    ./gradlew :app:connectedAndroidTest
    ```
2.  Monitor logcat for any `UnsatisfiedLinkError`, `OutOfMemoryError`, or `SecurityException`.

### Manual Verification
- Deploy the app to the device and verify that the UI remains responsive during background stress operations.
