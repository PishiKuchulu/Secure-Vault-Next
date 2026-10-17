package com.securevault.next.core.crypto.random

import com.securevault.next.core.crypto.memory.SecureBytes
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Generates cryptographically secure random bytes.
 *
 * Every salt, nonce/IV, and randomly generated key in this module goes
 * through this interface — there is no second source of randomness
 * anywhere in `core:crypto`. Per the Milestone 3 security requirements
 * ("Never use pseudo-random generators"), nothing in this module ever
 * calls `kotlin.random.Random` or `java.util.Random`, both of which are
 * statistically-random but *not* cryptographically secure (predictable
 * given enough output).
 */
interface SecureRandomProvider {

    /** Generates [size] cryptographically secure random bytes. */
    fun nextBytes(size: Int): SecureBytes

    /**
     * Generates a random nonce of [size] bytes for use with an AEAD
     * cipher. Semantically distinct from [nextBytes] for call-site
     * clarity; same underlying source.
     */
    fun nextNonce(size: Int): SecureBytes = nextBytes(size)

    /**
     * Generates a random salt of [size] bytes for use with a KDF.
     * Semantically distinct from [nextBytes] for call-site clarity; same
     * underlying source.
     */
    fun nextSalt(size: Int): SecureBytes = nextBytes(size)
}

/**
 * The production [SecureRandomProvider], backed by [java.security.SecureRandom].
 *
 * [SecureRandom] is seeded from the platform's entropy source (on
 * Android, ultimately `/dev/urandom` via the `NativePRNG`/`Crypto`
 * provider) and reseeds periodically — this is the standard, correct
 * choice for cryptographic randomness on the JVM/Android and requires no
 * additional configuration to be secure.
 */
class SystemSecureRandomProvider @Inject constructor() : SecureRandomProvider {

    private val secureRandom = SecureRandom()

    override fun nextBytes(size: Int): SecureBytes {
        require(size > 0) { "SecureRandomProvider.nextBytes size must be positive, was $size" }
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return SecureBytes.wrap(bytes)
    }
}
