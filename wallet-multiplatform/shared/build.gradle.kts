plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    jvm("desktop")
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.8.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
            }
        }
        val iosMain by creating {
            dependsOn(commonMain)
        }
        val desktopMain by getting {
            dependsOn(commonMain)
        }
        val wasmJsMain by getting {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.btq.wallet.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
}
