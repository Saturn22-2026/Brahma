pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Bitcoin-Quantum-Sovereign"

include(":app")
include(":shared")
include(":composeApp")

project(":app").projectDir = file("wallet-android/app")
project(":shared").projectDir = file("wallet-android/shared")
project(":composeApp").projectDir = file("wallet-android/composeApp")
