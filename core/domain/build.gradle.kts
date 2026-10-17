plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// -----------------------------------------------------------------------
// core:domain — Part 2 (Domain Layer): "No Android code. No Room. No
// Compose. No Context. No Activity. No Fragment. No ViewModel. Pure
// Kotlin only."
//
// Populated starting Milestone 2 (Domain Layer, Business Models & Core
// Contracts): domain models, value objects, enums, the error hierarchy,
// the validation framework, repository interfaces and use case contracts.
// Still zero Android/Compose/Room/Hilt dependency, and still zero
// cryptography — crypto, persistence, and every concrete implementation
// remain Milestone 3+ work.
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    // kotlinx.serialization: List<CustomField> is serialized by data:repository's
    // ItemMapper and List<ItemId> is decoded by its AuditRepositoryImpl, so the
    // model types themselves must carry generated serializers (Finding C3).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.testing.unit)
}
