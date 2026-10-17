plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// -----------------------------------------------------------------------
// data:persistence — Part 2 (Database Layer): "Database implementation
// must remain isolated. No SQL outside persistence. No Room annotations
// outside persistence." This module owns Room entities, DAOs and
// migrations exclusively; nothing outside this module may reference a
// Room type directly (Part 11: Repository Contracts).
//
// Deliberately empty in Milestone 1; implementation begins in Milestone 4
// per the project's Implementation Order (Part 10, Phase 4).
// -----------------------------------------------------------------------

android {
    namespace = "com.securevault.next.data.persistence"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
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

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.bundles.testing.unit)
}
