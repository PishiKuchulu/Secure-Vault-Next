package com.securevault.next.core.crypto.cipher

import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import com.securevault.next.core.crypto.result.asCryptoSuccess
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import kotlinx.coroutines.CancellationException
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * The production [CipherEngine], implementing AES-256-GCM via
 * `javax.crypto` — the standard JCA transformation `"AES/GCM/NoPadding"`
 * available on every Android API level this project targets (minSdk 29;
 * `AES/GCM/NoPadding` has been supported by the platform's default
 * provider since API 19).
 *
 * Every encryption call generates a fresh random nonce via
 * [secureRandomProvider] — nonces are never reused (Milestone 3 security
 * requirement: "Never reuse IVs"), which is essential for GCM: reusing a
 * nonce under the same key catastrophically breaks GCM's confidentiality
 * and authenticity guarantees.
 *
 * [key] is always read via [SecureBytes.peek], never wiped by this
 * class: a session key is expected to encrypt or decrypt many fields
 * over the lifetime of an unlocked vault, so this engine must not
 * silently destroy it after a single call. Wiping the key is the
 * responsibility of whatever holds it for the session (see
 * `core.crypto.key.SessionKeyHolder`).
 */
class AesGcmCipherEngine @Inject constructor(
    private val secureRandomProvider: SecureRandomProvider,
) : CipherEngine {

    override fun encrypt(
        plaintext: SecureBytes,
        key: SecureBytes,
        associatedData: ByteArray,
    ): CryptoResult<EncryptedField> {
        return try {
            key.peek { keyBytes ->
                require(keyBytes.size == KEY_LENGTH_BYTES) {
                    "AesGcmCipherEngine requires a $KEY_LENGTH_BYTES-byte (AES-256) key, got ${keyBytes.size} bytes"
                }

                secureRandomProvider.nextNonce(NONCE_LENGTH_BYTES).consume { nonceBytes ->
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    val secretKey = SecretKeySpec(keyBytes, "AES")
                    val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, nonceBytes)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
                    if (associatedData.isNotEmpty()) {
                        cipher.updateAAD(associatedData)
                    }

                    val ciphertextAndTag = plaintext.peek { cipher.doFinal(it) }
                    val encodedNonce = Base64.getEncoder().encodeToString(nonceBytes)

                    EncryptedField(
                        ciphertext = Base64.getEncoder().encodeToString(ciphertextAndTag),
                        nonce = encodedNonce,
                        algorithm = EncryptedField.DEFAULT_ALGORITHM,
                    ).asCryptoSuccess()
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            CryptoError.CryptoFailure("AES-256-GCM encryption failed", t).asCryptoFailure()
        }
    }

    override fun decrypt(
        encryptedField: EncryptedField,
        key: SecureBytes,
        associatedData: ByteArray,
    ): CryptoResult<SecureBytes> {
        if (encryptedField.algorithm != EncryptedField.DEFAULT_ALGORITHM) {
            return CryptoError.CryptoFailure(
                "AesGcmCipherEngine only supports ${EncryptedField.DEFAULT_ALGORITHM}, was ${encryptedField.algorithm}",
            ).asCryptoFailure()
        }

        return try {
            val nonceBytes = Base64.getDecoder().decode(encryptedField.nonce)
            val ciphertextAndTag = Base64.getDecoder().decode(encryptedField.ciphertext)

            key.peek { keyBytes ->
                require(keyBytes.size == KEY_LENGTH_BYTES) {
                    "AesGcmCipherEngine requires a $KEY_LENGTH_BYTES-byte (AES-256) key, got ${keyBytes.size} bytes"
                }

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, nonceBytes)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                if (associatedData.isNotEmpty()) {
                    cipher.updateAAD(associatedData)
                }

                val plaintext = cipher.doFinal(ciphertextAndTag)
                SecureBytes.wrap(plaintext).asCryptoSuccess()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (a: AEADBadTagException) {
            // The GCM authentication tag did not verify: either the key is
            // wrong or the ciphertext/AAD was modified. AES-GCM cannot
            // distinguish these two cases by design (see CryptoError.InvalidCiphertext's
            // documentation) — deliberately not caught as a bare CryptoFailure,
            // since callers need to be able to react to "authentication failed"
            // specifically (e.g. to increment a failed-attempt counter).
            CryptoError.InvalidCiphertext(a).asCryptoFailure()
        } catch (t: Throwable) {
            CryptoError.CryptoFailure("AES-256-GCM decryption failed", t).asCryptoFailure()
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_LENGTH_BYTES = 32 // AES-256
        const val NONCE_LENGTH_BYTES = 12 // 96 bits — the NIST SP 800-38D recommended GCM nonce size
        const val TAG_LENGTH_BITS = 128 // the standard, maximum-strength GCM authentication tag size
    }
}
