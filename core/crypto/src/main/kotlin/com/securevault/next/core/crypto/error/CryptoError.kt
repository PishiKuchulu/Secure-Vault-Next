package com.securevault.next.core.crypto.error

/**
 * The complete typed error hierarchy for `core:crypto`.
 *
 * This is deliberately a *separate* hierarchy from
 * `core.domain.error.DomainError` rather than a reuse of
 * `DomainError.CryptoRequiredError` (which only models "the domain layer
 * needed crypto but didn't have it" — a much narrower case). Crypto
 * failures are lower-level and more specific than anything the domain
 * layer should need to know the internal shape of; a future
 * `data:repository` implementation is where a [CryptoError] gets mapped
 * onto the appropriate `DomainError` (e.g. [InvalidPassword] →
 * `DomainError.AuthenticationError.InvalidCredentials`).
 *
 * Per the Milestone 3 security requirements ("No silent security
 * failures"), every failure mode below is a distinct, named case rather
 * than a single generic "decryption failed" — callers (and future audit
 * logging) can distinguish *why* a cryptographic operation failed.
 */
sealed class CryptoError(
    open val message: String,
    open val cause: Throwable? = null,
) {

    /**
     * The supplied master password did not unwrap the vault key.
     * Indistinguishable, by design, from [VaultCorrupted].
     */
    data object InvalidPassword :
        CryptoError("The provided password is incorrect")

    /** Authentication via a non-password method (biometric, PIN, hardware key) failed. */
    data class AuthenticationFailed(val method: String, override val cause: Throwable? = null) :
        CryptoError("Authentication via $method failed", cause)

    /**
     * The vault's encrypted structure is malformed in a way that isn't
     * simply "wrong password" — a field is missing, a length is
     * impossible, or decoding itself threw. Deliberately produces the
     * *same* user-facing outcome as [InvalidPassword] wherever a
     * distinction would leak information to an attacker about which
     * failure occurred; see [InvalidCiphertext] for where that
     * indistinguishability is enforced.
     */
    data class VaultCorrupted(val details: String, override val cause: Throwable? = null) :
        CryptoError("Vault data is corrupted: $details", cause)

    /**
     * Decryption failed — either the key was wrong or the ciphertext
     * (or its GCM authentication tag) was modified. AES-GCM
     * authenticated decryption cannot distinguish "wrong key" from
     * "tampered ciphertext" by design, and this error type preserves
     * that ambiguity rather than trying to guess which occurred.
     */
    data class InvalidCiphertext(override val cause: Throwable? = null) :
        CryptoError("The ciphertext could not be authenticated and decrypted", cause)

    /**
     * A stored integrity value (HMAC) did not match the recomputed value
     * — see `core.crypto.integrity.IntegrityVerifier`.
     */
    data class IntegrityCheckFailed(val subject: String) :
        CryptoError("Integrity check failed for: $subject")

    /** The vault format, KDF metadata, or cipher metadata version is not one this build knows how to handle. */
    data class UnsupportedVersion(val found: Int, val supportedRange: IntRange) :
        CryptoError("Version $found is not supported (supported: $supportedRange)")

    /** A required cryptographic key was not available in memory (e.g. the vault is locked) or in the Keystore. */
    data class KeyUnavailable(val keyAlias: String) :
        CryptoError("Key '$keyAlias' is not available")

    /**
     * Android Keystore itself failed — key generation, retrieval, or a
     * Keystore-backed [java.security.KeyStore] operation threw.
     */
    data class KeystoreFailure(val operation: String, override val cause: Throwable? = null) :
        CryptoError("Keystore operation '$operation' failed", cause)

    /**
     * A failure that does not fit any category above. Always carries
     * [cause] — this exists as an honest escape hatch for exceptions
     * thrown by the underlying `javax.crypto`/Keystore APIs that don't
     * map to a more specific case, never as a substitute for modeling a
     * real failure mode.
     */
    data class CryptoFailure(override val message: String, override val cause: Throwable? = null) :
        CryptoError(message, cause)
}
