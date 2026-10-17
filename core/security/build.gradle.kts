plugins {
    alias(libs.plugins.kotlin.jvm)
}

// -----------------------------------------------------------------------
// core:security — Part 2 (Security Module): owns authentication, auto
// lock, session management, clipboard timeout *policy* and security
// event modeling. This is business-rule/state-machine logic (e.g. "has
// N minutes of inactivity elapsed"), which is platform-agnostic and
// therefore pure Kotlin/JVM.
//
// It is distinct from core:platform, which will hold the Android-specific
// *mechanisms* (BiometricPrompt, ClipboardManager) that fulfil the
// policies defined here, and from core:crypto, which holds the session
// *key* (as opposed to session *policy*) — see core.crypto.key.SessionKeyHolder.
//
// Populated starting Milestone 3 with AutoLockEvaluator (pure, clock-
// injectable auto-lock timing) and FailedAttemptTracker (brute-force
// lockout policy).
// -----------------------------------------------------------------------

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.testing.unit)
}
