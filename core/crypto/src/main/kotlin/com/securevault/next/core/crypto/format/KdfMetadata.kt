package com.securevault.next.core.crypto.format

import com.securevault.next.core.crypto.kdf.KdfAlgorithm
import com.securevault.next.core.crypto.kdf.KdfParameters
import kotlinx.serialization.Serializable

/**
 * The on-disk record of exactly which KDF algorithm, parameters and salt
 * were used to derive this vault's master key.
 *
 * This is [KdfParameters] plus the one thing [KdfParameters] itself
 * deliberately does *not* carry — the salt (a [KdfParameters] instance
 * describes *how* to derive a key; it says nothing about *which* vault
 * that derivation is for). Every field here is required to reproduce
 * the exact same derivation later, which is the whole point of
 * persisting it: unlocking a vault means deriving with *these exact*
 * parameters, not today's defaults.
 */
@Serializable
data class KdfMetadata(
    val algorithm: KdfAlgorithm,
    val parameterVersion: Int,
    val memoryCostKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val outputLengthBytes: Int,
    /**
     * Base64-encoded salt. Not secret — salts exist to defeat rainbow
     * tables and multi-target attacks, not to be hidden.
     */
    val salt: String,
) {
    init {
        require(salt.isNotBlank()) { "KdfMetadata.salt must not be blank" }
    }

    /**
     * Reconstructs the [KdfParameters] this metadata was recorded from
     * (everything except the salt, which callers read from [salt]
     * directly).
     */
    fun toParameters(): KdfParameters = KdfParameters(
        algorithm = algorithm,
        parameterVersion = parameterVersion,
        memoryCostKib = memoryCostKib,
        iterations = iterations,
        parallelism = parallelism,
        outputLengthBytes = outputLengthBytes,
    )

    companion object {
        /** Builds [KdfMetadata] from a derivation's [parameters] and the [saltBase64] used with it. */
        fun from(parameters: KdfParameters, saltBase64: String): KdfMetadata = KdfMetadata(
            algorithm = parameters.algorithm,
            parameterVersion = parameters.parameterVersion,
            memoryCostKib = parameters.memoryCostKib,
            iterations = parameters.iterations,
            parallelism = parameters.parallelism,
            outputLengthBytes = parameters.outputLengthBytes,
            salt = saltBase64,
        )
    }
}
