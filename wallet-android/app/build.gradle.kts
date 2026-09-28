plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun usablePublicRpc(url: String): String {
    val cleaned = url.trim().trim('"')
    if (cleaned.isEmpty()) return ""
    val low = cleaned.lowercase()
    if (low.contains("trycloudflare.com")) return ""
    if (low.contains("dash.cloudflare.com") || low.contains("that-host")) return ""
    if (low.contains("drpc") || low.contains("/lambda/")) return ""
    if (low.contains("127.0.0.1") || low.contains("localhost")) return ""
    return cleaned
}

fun resolvePublicRpc(): String {
    val local = rootProject.file("local.properties")
    if (local.exists()) {
        local.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("brah.publicRpc=") || trimmed.startsWith("btq.publicRpc=")) {
                val value = usablePublicRpc(trimmed.substringAfter("="))
                if (value.isNotEmpty()) return value
            }
        }
    }
    val fromEnv = usablePublicRpc(
        System.getenv("BRAH_PUBLIC_RPC").orEmpty().ifBlank { System.getenv("BTQ_PUBLIC_RPC").orEmpty() }
    )
    if (fromEnv.isNotEmpty()) return fromEnv
    val fromFile = rootProject.file("../btq-data/public_rpc.txt")
    if (fromFile.exists()) {
        val value = usablePublicRpc(fromFile.readText().lineSequence().firstOrNull().orEmpty())
        if (value.isNotEmpty()) return value
    }
    return ""
}

val publicRpcForBuild = resolvePublicRpc()
val publicRpcInputFiles = listOf(
    rootProject.file("local.properties"),
    rootProject.file("../btq-data/public_rpc.txt"),
)

android {
    namespace = "com.brahmnetwork.wallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.brahmnetwork.wallet"
        minSdk = 26
        targetSdk = 34
        versionCode = 41
        versionName = "1.0.35"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        val publicRpc = publicRpcForBuild.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "PUBLIC_RPC_URL", "\"$publicRpc\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Security & Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Network
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

afterEvaluate {
    tasks.matching { it.name.contains("BuildConfig", ignoreCase = true) }.configureEach {
        publicRpcInputFiles.filter { it.exists() }.forEach { inputs.file(it) }
    }
}
