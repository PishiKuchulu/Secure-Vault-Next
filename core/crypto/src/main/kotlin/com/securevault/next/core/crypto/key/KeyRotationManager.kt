package com.securevault.next.core.crypto.key

import com.securevault.next.core.common.result.flatMap
import com.securevault.next.core.common.result.map
import com.securevault.next.core.crypto.cipher.CipherEngine
import com.securevault.next.core.crypto.format.EncryptedVaultEnvelope
import com.securevault.next.core.crypto.format.EncryptionMetadata
import com.securevault.next.core.crypto.format.IntegrityMetadata
import com.securevault.next.core.crypto.format.KdfMetadata
import com.securevault.next.core.crypto.integrity.IntegrityVerifier
import com.securevault.next.core.crypto.kdf.DerivedKeySplitter
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.kdf.KeyDerivationEngine
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import java.util.Base64

/**
 * Changes the password (and/or KDF parameters) protecting a vault
 * without re-encrypting the vault's content.
 *
 * This is the entire point of the master-key/vault-key indirection
 * documented on `Keys.kt`: rotating the master password only needs to
 * derive a new master key and re-wrap the *existing* vault key under it.
 * The (potentially large) encrypted content stays exactly as it is —
 * still encrypted under the same vault key it always was — which makes
 * password rotation an operation whose cost depends only on the KDF, not
 * on how much data is in the vault.
 *
 * The same mechanism also implements "support migration to stronger
 * parameters" (Milestone 3): rotating to *the same* password but a
 * stronger [KdfParameters] is exactly the same operation, just with an
 * unchanged password on both sides.
 *
 * **Ownership**: [rotateMasterKey] takes ownership of and wipes its
 * `newPassword` argument before returning, on every code path.
 * [currentVaultKey] is *not* consumed — the caller's active session
 * needs it to keep working with vault content after rotation.
 */
interface KeyRotationManager {
    /**
     * Re-derives a master key from [newPassword] under [newKdfParameters]
     * and re-wraps [currentVaultKey] under it, producing an updated
     * envelope. [envelope]'s [EncryptedVaultEnvelope.encryptedContent] is
     * carried through unchanged.
     */
    fun rotateMasterKey(
        envelope: EncryptedVaultEnvelope,
        currentVaultKey: VaultKey,
        newPassword: SecureBytes,
        newKdfParameters: KdfParameters = KdfParameters.CURRENT
    ): CryptoResult<EncryptedVaultEnvelope>
}

/** The production [KeyRotationManager]. */
class DefaultKeyRotationManager(
    private val keyDerivationEngine: KeyDerivationEngine,
    private val cipherEngine: CipherEngine,
    private val integrityVerifier: IntegrityVerifier,
    private val secureRandomProvider: SecureRandomProvider
) : KeyRotationManager {
    override fun rotateMasterKey(
        envelope: EncryptedVaultEnvelope,
        currentVaultKey: VaultKey,
        newPassword: SecureBytes,
        newKdfParameters: KdfParameters
    ): CryptoResult<EncryptedVaultEnvelope> = newPassword.use {
        val newSalt = secureRandomProvider.nextSalt(SALT_LENGTH_BYTES)

        newSalt.use {
            keyDerivationEngine
                .derive(newPassword, newSalt, newKdfParameters)
                .flatMap { derived -> DerivedKeySplitter.split(derived) }
                .flatMap { (newMasterKeyBytes, newMacKeyBytes) ->
                    newMasterKeyBytes.use {
                        newMacKeyBytes.use {
                            buildRotatedEnvelope(
                                envelope = envelope,
                                currentVaultKey = currentVaultKey,
                                newMasterKeyBytes = newMasterKeyBytes,
                                newMacKeyBytes = newMacKeyBytes,
                                newSalt = newSalt,
                                newKdfParameters = newKdfParameters
                            )
                        }
                    }
                }
        }
    }

    private fun buildRotatedEnvelope(
        envelope: EncryptedVaultEnvelope,
        currentVaultKey: VaultKey,
        newMasterKeyBytes: SecureBytes,
        newMacKeyBytes: SecureBytes,
        newSalt: SecureBytes,
        newKdfParameters: KdfParameters
    ): CryptoResult<EncryptedVaultEnvelope> {
        // currentVaultKey.bytes is peeked, not consumed: the caller's
        // active session still needs this exact key afterward to keep
        // encrypting/decrypting content in the same session. The defensive
        // copy handed to encrypt() is wiped via use{} as soon as encrypt()
        // has consumed it (Finding M2 — encrypt only peeks, never wipes).
        return currentVaultKey.bytes.copy().use { vaultKeyCopy ->
            cipherEngine
                .encrypt(vaultKeyCopy, newMasterKeyBytes)
                .flatMap { newWrappedVaultKey ->
                    val newSaltBase64 = newSalt.peek { Base64.getEncoder().encodeToString(it) }
                    val envelopeWithoutMac =
                        envelope.copy(
                            kdfMetadata = KdfMetadata.from(newKdfParameters, newSaltBase64),
                            encryptionMetadata = EncryptionMetadata(newWrappedVaultKey)
                        )

                    integrityVerifier
                        .computeMac(newMacKeyBytes, envelopeWithoutMac.canonicalIntegrityPayload())
                        .map { mac ->
                            envelopeWithoutMac.copy(
                                integrityMetadata = IntegrityMetadata(Base64.getEncoder().encodeToString(mac))
                            )
                        }
                }
        }
    }

    private companion object {
        const val SALT_LENGTH_BYTES = 16
    }
}
