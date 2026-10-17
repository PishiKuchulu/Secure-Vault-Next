plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// -----------------------------------------------------------------------
// core:contracts — pure Kotlin/JVM shared abstraction module.
//
// Holds the cross-layer seams that both the pure business core
// (core:vault) and the crypto implementation (core:crypto) must agree
// on, WITHOUT either depending on the other. The classic dependency
// inversion: business use cases depend only on these abstractions, and
// the concrete crypto implementation implements them — but never the
// reverse.
//
// It depends only on core:common (and core:domain, for the DomainResult
// alias that its contracts return). Both are pure, low, acyclic layers,
// so this module stays a strict DAG leaf: nothing above it, no Android,
// no Compose, no Room. A future Windows client can reuse it unchanged.
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:common"))
    // DomainResult is defined in core:domain (an alias over core:common's
    // Result + core:domain's DomainError). The contracts return DomainResult,
    // so this module needs core:domain — still a pure, low layer.
    api(project(":core:domain"))

    testImplementation(libs.bundles.testing.unit)
}
