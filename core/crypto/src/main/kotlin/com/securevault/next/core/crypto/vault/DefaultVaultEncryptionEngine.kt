package com.securevault.next.core.crypto.vault

import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.flatMap
import com.securevault.next.core.common.result.map
import com.securevault.next.core.crypto.cipher.CipherEngine
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.format.CipherMetadata
import com.securevault.next.core.crypto.format.EncryptedVaultEnvelope
import com.securevault.next.core.crypto.format.EncryptionMetadata
import com.securevault.next.core.crypto.format.IntegrityMetadata
import com.securevault.next.core.crypto.format.KdfMetadata
import com.securevault.next.core.crypto.format.VaultFormatVersion
import com.securevault.next.core.crypto.integrity.IntegrityVerifier
import com.securevault.next.core.crypto.kdf.DerivedKeySplitter
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.kdf.KeyDerivationEngine
import com.securevault.next.core.crypto.key.VaultKey
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import java.util.Base64
import javax.inject.Inject

/**
 * The production [VaultEncryptionEngine].
 *
 * Every derived key here follows the same split, performed by
 * [DerivedKeySplitter]: [KeyDerivationEngine.derive] is asked for
 * [KdfParameters.outputLengthBytes] (64 by default) bytes, the first 32
 * become the AES-256 master key that wraps the vault key, and the last
 * 32 become the HMAC-SHA256 key that authenticates the envelope's
 * metadata. Deriving both from the *same* Argon2id call (with the same
 * salt and cost parameters) rather than two separate derivations is what
 * makes the integrity check double as a password check: see
 * [unlockVault] and the module-level docs on
 * `core.crypto.integrity.IntegrityVerifier` for why that matters against
 * a KDF-downgrade attack.
 *
 * **Ownership of [SecureBytes] parameters**: [createVault] and
 * [unlockVault] take ownership of and wipe their `masterPassword`
 * parameter before returning, on every code path (success, expected
 * failure, or a thrown exception) — the master password's job is done
 * the moment this call returns, so its lifetime is minimized rather than
 * left to the caller's discretion. [reEncryptContent] and
 * [decryptContent] do *not* take ownership of the [VaultKey] they are
 * given, since a session key is expected to be reused across many calls
 * for the lifetime of an unlocked session (see
 * `core.crypto.key.SessionKeyHolder`).
 */
class DefaultVaultEncryptionEngine @Inject constructor(
    private val keyDerivationEngine: KeyDerivationEngine,
    private val cipherEngine: CipherEngine,
    private val integrityVerifier: IntegrityVerifier,
    private val secureRandomProvider: SecureRandomProvider,
) : VaultEncryptionEngine {

    override fun createVault(
        masterPassword: SecureBytes,
        plaintextContent: SecureBytes,
        kdfParameters: KdfParameters,
    ): CryptoResult<VaultCreationResult> = masterPassword.use {
        val salt = secureRandomProvider.nextSalt(SALT_LENGTH_BYTES)
        salt.use {
            keyDerivationEngine.derive(masterPassword, salt, kdfParameters)
                .flatMap { derivedMaterial -> DerivedKeySplitter.split(derivedMaterial) }
                .flatMap { (masterKeyBytes, macKeyBytes) ->
                    buildNewVault(
                        masterKeyBytes = masterKeyBytes,
                        macKeyBytes = macKeyBytes,
                        salt = salt,
                        kdfParameters = kdfParameters,
                        plaintextContent = plaintextContent,
                    )
                }
        }
    }

    private fun buildNewVault(
        masterKeyBytes: SecureBytes,
        macKeyBytes: SecureBytes,
        salt: SecureBytes,
        kdfParameters: KdfParameters,
        plaintextContent: SecureBytes,
    ): CryptoResult<VaultCreationResult> = masterKeyBytes.use {
        macKeyBytes.use {
            val vaultKeyBytes = secureRandomProvider.nextBytes(VAULT_KEY_LENGTH_BYTES)

            // Finding M2: encrypt() only peeks at its plaintext argument, so
            // the defensive key copies below used to linger unwiped in
            // memory. Each copy is now closed (wiped) via use{} immediately
            // after encrypt() has consumed it; the original vaultKeyBytes
            // stays live and is handed to the session inside VaultKey below.
            vaultKeyBytes.copy().use { wrapKeyCopy ->
                cipherEngine.encrypt(wrapKeyCopy, masterKeyBytes)
                    .flatMap { wrappedVaultKey ->
                        vaultKeyBytes.copy().use { contentKeyCopy ->
                            cipherEngine.encrypt(plaintextContent, contentKeyCopy)
                                .map { encryptedContent -> wrappedVaultKey to encryptedContent }
                        }
                    }
                    .flatMap { (wrappedVaultKey, encryptedContent) ->
                        val saltBase64 = salt.peek { Base64.getEncoder().encodeToString(it) }
                        val envelopeWithoutMac = EncryptedVaultEnvelope(
                            formatVersion = VaultFormatVersion.CURRENT,
                            kdfMetadata = KdfMetadata.from(kdfParameters, saltBase64),
                            cipherMetadata = CipherMetadata(),
                            encryptionMetadata = EncryptionMetadata(wrappedVaultKey),
                            integrityMetadata = IntegrityMetadata(mac = PLACEHOLDER_MAC),
                            encryptedContent = encryptedContent,
                        )

                        integrityVerifier.computeMac(macKeyBytes, envelopeWithoutMac.canonicalIntegrityPayload())
                            .map { mac ->
                                val envelope = envelopeWithoutMac.copy(
                                    integrityMetadata = IntegrityMetadata(Base64.getEncoder().encodeToString(mac)),
                                )
                                VaultCreationResult(envelope = envelope, vaultKey = VaultKey(vaultKeyBytes))
                            }
                    }
            }
        }
    }

    override fun unlockVault(envelope: EncryptedVaultEnvelope, masterPassword: SecureBytes): CryptoResult<VaultKey> =
        masterPassword.use {
            if (envelope.formatVersion.value !in VaultFormatVersion.SUPPORTED_RANGE) {
                return@use CryptoError.UnsupportedVersion(
                    found = envelope.formatVersion.value,
                    supportedRange = VaultFormatVersion.SUPPORTED_RANGE,
                ).asCryptoFailure()
            }

            val saltBytes = try {
                SecureBytes.wrap(Base64.getDecoder().decode(envelope.kdfMetadata.salt))
            } catch (t: IllegalArgumentException) {
                return@use CryptoError.VaultCorrupted("KDF salt is not valid Base64", t).asCryptoFailure()
            }

            saltBytes.use {
                // Finding M3: toParameters() runs require() checks that throw
                // a raw IllegalArgumentException on tampered or impossible
                // KDF metadata. Map that to a typed failure instead of
                // letting the raw exception escape — and do it BEFORE the
                // Argon2id derivation, so a tampered envelope fails fast
                // instead of burning a full KDF run first.
                val kdfParameters = try {
                    envelope.kdfMetadata.toParameters()
                } catch (t: IllegalArgumentException) {
                    return@use CryptoError.VaultCorrupted(
                        "KDF parameters are invalid or have been tampered with",
                        t,
                    ).asCryptoFailure()
                }

                keyDerivationEngine.derive(masterPassword, saltBytes, kdfParameters)
                    .flatMap { derivedMaterial -> DerivedKeySplitter.split(derivedMaterial) }
                    .flatMap { (masterKeyBytes, macKeyBytes) ->
                        masterKeyBytes.use {
                            macKeyBytes.use {
                                verifyAndUnwrap(envelope, masterKeyBytes, macKeyBytes)
                            }
                        }
                    }
            }
        }

    private fun verifyAndUnwrap(
        envelope: EncryptedVaultEnvelope,
        masterKeyBytes: SecureBytes,
        macKeyBytes: SecureBytes,
    ): CryptoResult<VaultKey> {
        val expectedMac = try {
            Base64.getDecoder().decode(envelope.integrityMetadata.mac)
        } catch (t: IllegalArgumentException) {
            return CryptoError.VaultCorrupted("Integrity MAC is not valid Base64", t).asCryptoFailure()
        }

        val macResult = integrityVerifier.verifyMac(
            macKeyBytes,
            envelope.canonicalIntegrityPayload(),
            expectedMac,
            subject = "vault envelope metadata",
        )

        if (macResult is Result.Failure) {
            // A MAC mismatch here is, overwhelmingly, a wrong password: the
            // MAC key was derived from the same Argon2id call as the master
            // key, so a wrong password produces a wrong MAC key and this
            // check fails before we ever attempt to unwrap the vault key.
            // Reporting it as InvalidPassword rather than IntegrityCheckFailed
            // avoids telling an attacker which of the two actually occurred.
            return CryptoError.InvalidPassword.asCryptoFailure()
        }

        val unwrapResult = cipherEngine.decrypt(envelope.encryptionMetadata.wrappedVaultKey, masterKeyBytes)
            .map { VaultKey(it) }

        // If the MAC verified, the master key is correct, so unwrap failure
        // here is not a password problem — it means the wrapped key itself
        // is corrupted independently of the metadata the MAC covers.
        return when (unwrapResult) {
            is Result.Success -> unwrapResult
            is Result.Failure -> CryptoError.VaultCorrupted(
                "Vault key could not be unwrapped despite a verified password",
            ).asCryptoFailure()
        }
    }

    override fun decryptContent(envelope: EncryptedVaultEnvelope, vaultKey: VaultKey): CryptoResult<SecureBytes> =
        cipherEngine.decrypt(envelope.encryptedContent, vaultKey.bytes)

    override fun reEncryptContent(
        envelope: EncryptedVaultEnvelope,
        vaultKey: VaultKey,
        plaintextContent: SecureBytes,
    ): CryptoResult<EncryptedVaultEnvelope> =
        cipherEngine.encrypt(plaintextContent, vaultKey.bytes)
            .map { encryptedContent -> envelope.copy(encryptedContent = encryptedContent) }

    private companion object {
        const val SALT_LENGTH_BYTES = 16
        const val VAULT_KEY_LENGTH_BYTES = 32
        const val PLACEHOLDER_MAC = "pending"
    }
}
