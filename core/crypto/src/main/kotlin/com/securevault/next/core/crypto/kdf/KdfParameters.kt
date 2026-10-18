package com.securevault.next.core.crypto.kdf

/**
 * The key derivation algorithms this module knows how to use. Currently
 * only Argon2id — the OWASP-recommended choice for password-based key
 * derivation as of this writing, combining Argon2i's resistance to
 * side-channel attacks with Argon2d's resistance to GPU cracking.
 */
enum class KdfAlgorithm {
    ARGON2ID
}

/**
 * Versioned, configurable parameters for a key derivation operation.
 *
 * Every field that affects the derived output is captured here and
 * persisted alongside the vault (see `core.crypto.format.KdfMetadata`),
 * never hardcoded into the derivation call site — this is what makes
 * "support future parameter upgrades" and "support migration to
 * stronger parameters" possible: a vault created under weaker
 * [KdfParameters] can still be opened (using the parameters recorded
 * with it) and then have its key re-derived and re-wrapped under
 * stronger parameters going forward, without the derivation *code*
 * needing to change.
 *
 * [parameterVersion] is independent of the overall vault format version
 * (`core.crypto.format.VaultFormatVersion`) — it increments only when
 * SecureVault Next's *default* KDF parameters change, so a client can
 * detect "this vault was created under an older default" and prompt for
 * a rehash, distinct from "this vault's on-disk structure changed".
 *
 * Defaults follow OWASP's current baseline recommendation for Argon2id
 * used for password hashing/key derivation: memory cost 19 MiB (19456
 * KiB) is OWASP's *absolute floor*; SecureVault Next defaults
 * substantially higher (64 MiB) since it is a security-first vault
 * rather than a generic web login, and a mobile device unlocking a
 * vault once per session can afford a slower derivation than a
 * high-throughput web server verifying logins per request.
 */
data class KdfParameters(
    val algorithm: KdfAlgorithm = KdfAlgorithm.ARGON2ID,
    val parameterVersion: Int = CURRENT_PARAMETER_VERSION,
    val memoryCostKib: Int = DEFAULT_MEMORY_COST_KIB,
    val iterations: Int = DEFAULT_ITERATIONS,
    val parallelism: Int = DEFAULT_PARALLELISM,
    /**
     * Derived output length in bytes. Defaults to 64 = 32 bytes for the
     * cipher key + 32 bytes for the integrity MAC key.
     */
    val outputLengthBytes: Int = DEFAULT_OUTPUT_LENGTH_BYTES
) {
    init {
        require(parameterVersion >= 1) { "KdfParameters.parameterVersion must be at least 1" }
        require(memoryCostKib >= MIN_MEMORY_COST_KIB) {
            "KdfParameters.memoryCostKib must be at least $MIN_MEMORY_COST_KIB (OWASP floor), was $memoryCostKib"
        }
        require(iterations >= 1) { "KdfParameters.iterations must be at least 1, was $iterations" }
        require(parallelism >= 1) { "KdfParameters.parallelism must be at least 1, was $parallelism" }
        require(outputLengthBytes >= MIN_OUTPUT_LENGTH_BYTES) {
            "KdfParameters.outputLengthBytes must be at least $MIN_OUTPUT_LENGTH_BYTES " +
                "(32 for the cipher key + 32 for the integrity MAC key), was $outputLengthBytes"
        }
    }

    /**
     * Whether this configuration meets OWASP's *combined* recommended
     * floor for Argon2id (not just each field's independent absolute
     * minimum).
     *
     * A pre-Milestone-4 security audit noted that [MIN_MEMORY_COST_KIB]
     * and the `iterations >= 1` check above are validated
     * *independently*, so the constructor alone cannot prevent
     * constructing, say, `memoryCostKib = MIN_MEMORY_COST_KIB` paired
     * with `iterations = 1` — a combination that is technically below
     * OWASP's documented recommendation of *19 MiB paired with at least
     * 2 iterations*. The constructor deliberately does not hard-block
     * this: test code in this codebase intentionally uses low-cost
     * parameters (including `iterations = 1` at the memory floor) to
     * keep the suite fast, and those parameters are never reachable from
     * any production code path. What matters is that the parameters
     * SecureVault Next actually *uses* by default ([CURRENT]) are
     * verified — see `KdfParametersTest` — to satisfy this method,
     * giving a real regression guard without penalizing test speed.
     */
    fun meetsOwaspMinimumRecommendation(): Boolean {
        // At or below the floor, OWASP requires the higher iteration count;
        // only strictly ABOVE the recommended memory floor is 1 iteration
        // acceptable. Using `<=` keeps the floor boundary itself (exactly
        // MIN_MEMORY_COST_KIB) on the strict side.
        if (memoryCostKib <= OWASP_RECOMMENDED_MEMORY_COST_KIB) {
            return iterations >= OWASP_RECOMMENDED_MIN_ITERATIONS_AT_FLOOR_MEMORY
        }
        return true
    }

    companion object {
        const val CURRENT_PARAMETER_VERSION = 1

        const val DEFAULT_MEMORY_COST_KIB = 65_536 // 64 MiB
        const val DEFAULT_ITERATIONS = 3
        const val DEFAULT_PARALLELISM = 4
        const val DEFAULT_OUTPUT_LENGTH_BYTES = 64

        const val MIN_MEMORY_COST_KIB = 19_456 // OWASP absolute floor for Argon2id
        const val MIN_OUTPUT_LENGTH_BYTES = 64

        /**
         * OWASP's documented *recommended* memory floor (19 MiB) — at or
         * above this, 1 iteration is acceptable. [MIN_MEMORY_COST_KIB]
         * shares this same value today; they are named separately because
         * they mean different things ("never allow below this" vs. "at or
         * above this, fewer iterations are fine").
         */
        const val OWASP_RECOMMENDED_MEMORY_COST_KIB = 19_456

        /** OWASP's minimum iteration count when memory is at [OWASP_RECOMMENDED_MEMORY_COST_KIB] rather than higher. */
        const val OWASP_RECOMMENDED_MIN_ITERATIONS_AT_FLOOR_MEMORY = 2

        /** SecureVault Next's current default parameters for newly created vaults. */
        val CURRENT = KdfParameters()
    }
}
