plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.btq.wallet.compose"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.btq.wallet.compose"
        minSdk = 26
        targetSdk = 34
    }
}

dependencies {
    implementation(project(":app"))
}
