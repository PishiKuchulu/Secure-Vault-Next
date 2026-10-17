plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.securevault.next.core.ui"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 29
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    // core:ui is a pure design-system module: it may depend on Compose
    // and AndroidX, but never on core:domain, core:crypto, data:* or any
    // other module that would pull business logic into the UI layer
    // (Part 2: "No business logic inside UI").

    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.testing.unit)
}
