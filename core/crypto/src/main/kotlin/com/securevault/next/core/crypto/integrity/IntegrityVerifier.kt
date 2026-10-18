package com.securevault.next.core.crypto.integrity

import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.flatMap
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import com.securevault.next.core.crypto.result.asCryptoSuccess
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Computes and verifies HMAC-SHA256 message authentication codes.
 *
 * The vault's *encrypted content* is already authenticated by AES-GCM's
 * own tag — this exists for the narrower, separate problem of protecting
 * the *unencrypted* header metadata that must remain readable before a
 * vault is unlocked (its format version, KDF parameters, cipher
 * algorithm — see `core.crypto.format`). Those fields can't be
 * encrypted (the lock screen needs to read them to know how to attempt
 * decryption at all), but they can be authenticated: this MAC lets the
 * app detect if that metadata was tampered with — most importantly a
 * *KDF parameter downgrade attack*, where an attacker weakens the
 * recorded Argon2id cost parameters to make offline brute-forcing
 * cheaper. See `core.crypto.vault.VaultEncryptionEngine` for how the MAC
 * key itself is derived so that forging a valid MAC over tampered
 * parameters still requires knowing the master password.
 *
 * Note on the threat model: this is a corruption/tamper-*detection*
 * mechanism, not a guarantee against a fully compromised device — an
 * attacker with arbitrary local code execution while the vault is
 * unlocked has bigger problems available to them than forging a header.
 * Its value is in detecting accidental corruption and in making a
 * password-oracle-free KDF downgrade attack cryptographically
 * infeasible for an attacker who does not know the master password.
 */
interface IntegrityVerifier {
    /** Computes an HMAC-SHA256 over [data] using [macKey]. */
    fun computeMac(macKey: SecureBytes, data: ByteArray): CryptoResult<ByteArray>

    /**
     * Recomputes the HMAC over [data] with [macKey] and compares it to
     * [expectedMac] in constant time. Returns [CryptoError.IntegrityCheckFailed]
     * rather than a bare boolean specifically so a caller cannot
     * accidentally ignore a failed check the way a discarded `Boolean`
     * return value can be.
     */
    fun verifyMac(macKey: SecureBytes, data: ByteArray, expectedMac: ByteArray, subject: String): CryptoResult<Unit>
}

/** The production [IntegrityVerifier], backed by `javax.crypto.Mac` with the standard `"HmacSHA256"` algorithm. */
class HmacSha256IntegrityVerifier
@Inject
constructor() : IntegrityVerifier {
    override fun computeMac(macKey: SecureBytes, data: ByteArray): CryptoResult<ByteArray> = try {
        val mac =
            macKey.peek { keyBytes ->
                Mac.getInstance(ALGORITHM).apply {
                    init(SecretKeySpec(keyBytes, ALGORITHM))
                }
            }
        mac.doFinal(data).asCryptoSuccess()
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        CryptoError.CryptoFailure("HMAC-SHA256 computation failed", t).asCryptoFailure()
    }

    override fun verifyMac(
        macKey: SecureBytes,
        data: ByteArray,
        expectedMac: ByteArray,
        subject: String
    ): CryptoResult<Unit> = computeMac(macKey, data).flatMap { computed ->
        // MessageDigest.isEqual performs a constant-time comparison,
        // which prevents a timing side-channel from leaking how many
        // leading bytes of a forged MAC happened to match.
        if (MessageDigest.isEqual(computed, expectedMac)) {
            Result.success(Unit)
        } else {
            Result.failure(CryptoError.IntegrityCheckFailed(subject))
        }
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}
