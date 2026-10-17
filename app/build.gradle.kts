plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.securevault.next"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.securevault.next"
        minSdk = 29 // Android 10+, per Part 1 (Primary Platform target)
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0-RC2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vector drawables only — no legacy raster icon variants needed.
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signing is deliberately left unconfigured here: production
            // signing credentials must never live in version control.
            // A release signing config is injected via a local, gitignored
            // keystore.properties file once the project is ready to ship.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // ---------------------------------------------------------------
    // Project modules — the app module is the only place allowed to
    // depend on every layer at once, since it is the composition root
    // (Part 11: Module Boundaries).
    // ---------------------------------------------------------------
    // core:contracts is declared explicitly (not leaked transitively):
    // app/.../di/CryptoModule.kt imports VaultKeyEncryptionService directly,
    // and core:crypto keeps contracts on a non-leaking `implementation`.
    implementation(project(":core:contracts"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:crypto"))
    implementation(project(":core:vault"))
    implementation(project(":core:security"))
    implementation(project(":core:platform"))
    implementation(project(":core:ui"))
    implementation(project(":data:persistence"))
    implementation(project(":data:repository"))

    // ---------------------------------------------------------------
    // AndroidX core
    // ---------------------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.biometric)

    // ---------------------------------------------------------------
    // Compose
    // ---------------------------------------------------------------
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // ---------------------------------------------------------------
    // Hilt
    // ---------------------------------------------------------------
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // ---------------------------------------------------------------
    // Coroutines
    // ---------------------------------------------------------------
    implementation(libs.bundles.coroutines)

    // ---------------------------------------------------------------
    // Testing
    // ---------------------------------------------------------------
    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
