plugins {
    alias(libs.plugins.kotlin.jvm)
}

// -----------------------------------------------------------------------
// core:testing holds shared test infrastructure (JUnit rules, fakes)
// consumed by every other module's unit tests. It is not part of the
// Clean Architecture layer stack described in Part 2 — it exists purely
// to keep test setup (e.g. swapping the Main dispatcher) from being
// duplicated across modules.
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
