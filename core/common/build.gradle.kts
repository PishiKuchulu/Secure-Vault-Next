plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// -----------------------------------------------------------------------
// core:common is pure Kotlin/JVM — no Android Gradle plugin is applied.
//
// Part 1 (Long Term Goal): "Business logic should never depend on UI...
// A shared platform-independent business core." This module is where
// that promise is enforced at the build-system level: if it compiled
// against the Android SDK, nothing would stop an Android type from
// leaking into the shared business core, which would break reuse on a
// future Windows client.
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    // javax.inject provides @Qualifier / @Inject annotations only — it
    // is a plain JSR-330 artifact with no Android dependency, which lets
    // this module declare DI qualifiers (e.g. for coroutine dispatchers)
    // that the Hilt-aware app module can bind against, without this
    // module itself depending on Hilt or Android.
    api(libs.javax.inject)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.testing.unit)
}
