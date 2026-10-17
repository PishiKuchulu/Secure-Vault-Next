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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SecureVaultNext"

// ---------------------------------------------------------------------
// Presentation
// ---------------------------------------------------------------------
include(":app")

// ---------------------------------------------------------------------
// Core layers (Part 2: Architecture Layers)
//
// core:common, core:domain, core:vault and core:security are pure
// Kotlin/JVM modules — no Android, Compose or Room dependency is
// permitted here, so this business core stays testable on a plain JVM
// and reusable by a future Windows client (Part 1: Long Term Goal).
//
// core:crypto and core:platform are Android library modules because
// they wrap genuinely Android-specific APIs (Keystore, BiometricPrompt,
// ClipboardManager). core:ui is Android-aware for Compose. core:testing
// stays pure Kotlin/JVM, since JUnit rules need no Android APIs.
// ---------------------------------------------------------------------
include(":core:common")
include(":core:contracts")
include(":core:domain")
include(":core:crypto")
include(":core:vault")
include(":core:security")
include(":core:platform")
include(":core:ui")
include(":core:testing")

// ---------------------------------------------------------------------
// Data layer (Part 8: Database Schema & Persistence)
// ---------------------------------------------------------------------
include(":data:persistence")
include(":data:repository")
