plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// -----------------------------------------------------------------------
// core:crypto — Part 2 (Crypto Module): "Independent module. No UI
// dependency. No Android UI dependency." Note this forbids *UI*
// dependencies specifically, not Android as a whole: Argon2id/AES-GCM
// key wrapping is expected to integrate with Android Keystore (Part 3),
// which requires the Android SDK. That is why this is an Android
// library module rather than pure Kotlin/JVM, unlike core:domain and
// core:vault.
//
// Populated starting Milestone 3 (Cryptography Engine & Security
// Infrastructure): key derivation, AES-256-GCM encryption, Android
// Keystore integration, key management, integrity verification, secure
// memory handling, and the versioned vault format.
// -----------------------------------------------------------------------

android {
    namespace = "com.securevault.next.core.crypto"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
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
    // core:contracts holds the VaultKeyEncryptionService seam this module's
    // DefaultVaultKeyEncryptionService implements (dependency inversion).
    implementation(project(":core:contracts"))
    implementation(project(":core:domain"))
    // core:security provides the SessionKeyStore abstraction that the
    // Android-side SessionKeyHolderStore adapter (Milestone 2) implements,
    // letting the pure-JVM session manager depend only on the interface.
    implementation(project(":core:security"))
    implementation(libs.kotlinx.coroutines.core)

    // Serialization for the EncryptedVaultEnvelope and its metadata types,
    // so the envelope can be persisted as a single JSON column (v1.6/M3).
    // Field encryption stays separate; EncryptedField itself is not
    // serialized in this milestone.
    implementation(libs.kotlinx.serialization.json)

    // Argon2id has no implementation in the JDK or Android standard
    // library. Bouncy Castle's Argon2BytesGenerator is a well-established,
    // widely audited, pure-JVM (no native/JNI) implementation used by
    // many production Android apps for exactly this purpose. It is used
    // here by direct instantiation (Argon2BytesGenerator/Argon2Parameters)
    // rather than through JCA Provider registration, which avoids any
    // conflict with Android's own built-in, stripped-down BC provider.
    implementation(libs.bouncycastle.provider)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.testing.unit)
}
