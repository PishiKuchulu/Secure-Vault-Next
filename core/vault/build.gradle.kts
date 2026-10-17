plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// -----------------------------------------------------------------------
// core:vault — Part 2 (Vault Module): responsible only for vault
// structure, serialization, deserialization, encryption orchestration,
// versioning, integrity and import/export. It depends on the *contracts*
// exposed by core:domain (and, once defined, a crypto capability
// interface) rather than on core:crypto's Android implementation
// directly — keeping this module pure Kotlin/JVM preserves the "shared
// platform-independent business core" goal from Part 1 for a future
// Windows client.
//
// Implementation begins in Milestone 3/4 per the project's
// Implementation Order. Deliberately empty in Milestone 1.
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:contracts"))
    implementation(project(":core:domain"))
    // core:security provides the pure-JVM session abstractions (SessionKeyStore,
    // VaultSessionManager, SessionClock) that the unlock/lock use cases orchestrate.
    implementation(project(":core:security"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing.unit)
}
