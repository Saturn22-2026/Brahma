# Fix AGP 9.0 Compatibility Issue with Kotlin Multiplatform

The project is encountering a build error because Android Gradle Plugin (AGP) 9.0+ enables "built-in Kotlin" by default, which is currently incompatible with the `org.jetbrains.kotlin.multiplatform` plugin when used alongside `com.android.application` or `com.android.library`.

## Proposed Changes

The immediate and least intrusive fix to bypass this compatibility check (as suggested by the error message) is to disable the new built-in Kotlin support and the new DSL in the project's `gradle.properties`.

### Project Root

#### [MODIFY] [gradle.properties](file:///C:/GitHub/Bitcoin-Quantum/gradle.properties)
Add the following properties to bypass the AGP 9.0 compatibility check:
```properties
android.builtInKotlin=false
android.newDsl=false
```

## Verification Plan

### Automated Tests
- Run `./gradlew sync` or a build task to verify the `IllegalStateException` is gone.
