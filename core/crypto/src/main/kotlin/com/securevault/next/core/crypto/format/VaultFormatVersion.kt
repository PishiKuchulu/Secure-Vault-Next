package com.securevault.next.core.crypto.format

import kotlinx.serialization.Serializable

/**
 * The version of the overall [EncryptedVaultEnvelope] structure itself
 * — distinct from `KdfParameters.parameterVersion`, which tracks only
 * the KDF's *default* cost parameters. This version increments when the
 * *shape* of the envelope changes (a field is added, removed, or
 * reinterpreted), which is a rarer and more significant event than a
 * KDF parameter bump.
 */
@JvmInline
@Serializable
value class VaultFormatVersion(
    val value: Int,
) {
    init {
        require(value >= 1) { "VaultFormatVersion must be at least 1, was $value" }
    }

    companion object {
        /** The format version this build writes for newly created vaults. */
        val CURRENT = VaultFormatVersion(1)

        /**
         * The range of format versions this build can read. A vault
         * outside this range fails with `CryptoError.UnsupportedVersion`.
         */
        val SUPPORTED_RANGE = 1..1
    }
}
