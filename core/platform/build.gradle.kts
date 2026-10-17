plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// -----------------------------------------------------------------------
// core:platform — Part 2 (Platform Services): "Abstract platform-specific
// features... Every service must have an interface." This module holds
// the Android implementations of capabilities such as Biometric prompts,
// Clipboard access, Notifications, the system File Picker and secure
// Keystore-backed storage.
//
// It is an Android library because these are genuinely Android APIs —
// but every capability it exposes is defined as an interface consumable
// from platform-agnostic modules (core:security, core:vault), keeping
// the Domain layer itself unaware of the Android implementation (Part
// 11: Platform Service Contracts).
//
// Deliberately empty in Milestone 1; implementation begins in Milestone 3.
// -----------------------------------------------------------------------

android {
    namespace = "com.securevault.next.core.platform"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.bundles.testing.unit)
}
