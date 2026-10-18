package com.securevault.next.core.crypto.kdf

import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import com.securevault.next.core.crypto.result.asCryptoSuccess

/**
 * The single place [KdfParameters.outputLengthBytes]-byte Argon2id
 * output is split into an AES-256 master key and an HMAC-SHA256 MAC
 * key.
 *
 * Originally duplicated, verbatim, inside both
 * `core.crypto.vault.DefaultVaultEncryptionEngine` and
 * `core.crypto.key.DefaultKeyRotationManager` — a pre-Milestone-4
 * security audit flagged the duplication, since a future change to the
 * split point (e.g. a larger MAC key) would have silently only applied
 * to whichever copy got edited. Both call sites now delegate here.
 */
object DerivedKeySplitter {
    const val MASTER_KEY_LENGTH_BYTES = 32
    const val MAC_KEY_LENGTH_BYTES = 32
    const val REQUIRED_TOTAL_LENGTH_BYTES = MASTER_KEY_LENGTH_BYTES + MAC_KEY_LENGTH_BYTES

    /**
     * Splits [derived] into (masterKeyBytes, macKeyBytes) and wipes the
     * combined input buffer — [derived] must not be used again after
     * this call, whether it succeeds or fails.
     */
    fun split(derived: SecureBytes): CryptoResult<Pair<SecureBytes, SecureBytes>> {
        if (derived.size < REQUIRED_TOTAL_LENGTH_BYTES) {
            derived.close()
            return CryptoError
                .CryptoFailure(
                    "Derived key material is too short to split into a master key and a MAC key " +
                        "(need $REQUIRED_TOTAL_LENGTH_BYTES bytes, got ${derived.size})",
                ).asCryptoFailure()
        }

        return derived.consume { bytes ->
            val masterKeyBytes = bytes.copyOfRange(0, MASTER_KEY_LENGTH_BYTES)
            val macKeyBytes =
                bytes.copyOfRange(
                    MASTER_KEY_LENGTH_BYTES,
                    REQUIRED_TOTAL_LENGTH_BYTES,
                )
            (SecureBytes.wrap(masterKeyBytes) to SecureBytes.wrap(macKeyBytes)).asCryptoSuccess()
        }
    }
}
