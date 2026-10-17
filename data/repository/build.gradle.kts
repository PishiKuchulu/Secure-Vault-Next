plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// -----------------------------------------------------------------------
// data:repository — Part 2 (Repository Layer): "Repositories are
// interfaces [owned by core:domain]. Infrastructure provides
// implementations. Repositories never expose Room entities. Repositories
// expose domain models only."
//
// This module implements those interfaces by coordinating
// data:persistence (Room) and core:crypto (encryption at rest). It is
// an Android library because its dependencies (Room, Android Keystore
// backed crypto) are themselves Android-specific — the *interfaces* it
// implements remain defined in pure-Kotlin core:domain, which is what
// keeps callers (Application-layer use cases) platform-agnostic.
//
// Deliberately empty in Milestone 1; implementation begins in Milestone 4.
// -----------------------------------------------------------------------

android {
    namespace = "com.securevault.next.data.repository"
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
    implementation(project(":core:crypto"))
    implementation(project(":data:persistence"))

    implementation(libs.kotlinx.coroutines.android)

    // Serialization for the ItemMapper / MiscRepositoryImpls payloads,
    // which use kotlinx.serialization.json (Json, encodeToString).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing.unit)
}
