package com.securevault.next.core.crypto.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.security.keystore.UserNotAuthenticatedException
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import com.securevault.next.core.crypto.result.asCryptoSuccess
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import java.security.KeyStore
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CancellationException

/**
 * Generates, stores and uses hardware-backed AES keys via the Android
 * Keystore system.
 *
 * Unlike the software keys produced by [com.securevault.next.core.crypto.kdf.KeyDerivationEngine],
 * a Keystore-backed key's raw bytes are never extractable — key
 * material generated here never leaves the device's secure hardware
 * (TEE or, when available, a dedicated StrongBox security chip). That
 * makes this provider the mechanism behind biometric-gated "quick
 * unlock" (Milestone 3: "Support biometric protection integration"): a
 * key created with [requireUserAuthentication] set can only be used
 * *after* the user has satisfied a biometric prompt, enforced by the
 * hardware itself rather than by application code.
 */
interface AndroidKeystoreKeyProvider {
    /**
     * Generates a new AES-256 key under [alias], replacing any existing
     * key with that alias. Requests StrongBox backing first and falls
     * back to the TEE-backed (non-StrongBox) hardware keystore if the
     * device has no StrongBox module — see [KeystoreCapabilities] for
     * how a caller learns which one it actually got.
     *
     * When [requireUserAuthentication] is `true`, the key can only be
     * used to encrypt/decrypt after the user authenticates via any
     * enrolled biometric within [userAuthenticationValiditySeconds] of
     * the operation.
     */
    fun generateKey(
        alias: String,
        requireUserAuthentication: Boolean,
        userAuthenticationValiditySeconds: Int = DEFAULT_AUTH_VALIDITY_SECONDS
    ): CryptoResult<KeystoreCapabilities>

    /**
     * Whether a key exists under [alias]. A keystore failure is reported
     * as a typed [CryptoError.KeystoreFailure] rather than collapsing to
     * `false` — "the keystore broke" is not "the key is absent"
     * (Finding L8).
     */
    fun hasKey(alias: String): CryptoResult<Boolean>

    /** Permanently removes the key. Used both for explicit key invalidation and to force regeneration. */
    fun deleteKey(alias: String): CryptoResult<Unit>

    fun capabilities(alias: String): CryptoResult<KeystoreCapabilities>

    /**
     * Encrypts [plaintext] under the Keystore key at [alias]. Fails with
     * [CryptoError.AuthenticationFailed] if the key requires
     * authentication that has not been satisfied.
     */
    fun encrypt(alias: String, plaintext: SecureBytes): CryptoResult<EncryptedField>

    /**
     * Decrypts [encryptedField] using the Keystore key at [alias]. Fails
     * with [CryptoError.AuthenticationFailed] if the key requires
     * authentication that has not been satisfied.
     */
    fun decrypt(alias: String, encryptedField: EncryptedField): CryptoResult<SecureBytes>

    companion object {
        const val DEFAULT_AUTH_VALIDITY_SECONDS = 30
    }
}

/**
 * The production [AndroidKeystoreKeyProvider], backed by
 * `java.security.KeyStore` with the `"AndroidKeyStore"` provider.
 *
 * **Thread safety**: a pre-Milestone-4 security audit noted that
 * `generateKey` and `deleteKey` mutate keystore state shared across every
 * alias, while `encrypt`/`decrypt`/`capabilities`/`hasKey` read it. Two
 * threads racing — e.g. one regenerating a key while another encrypts
 * with the alias mid-regeneration — could observe a half-updated key or
 * throw an unexpected exception instead of a clean [CryptoError]. Every
 * public operation here is synchronized on a single monitor to eliminate
 * that race entirely. This trades a small amount of parallelism (two
 * unrelated aliases cannot be touched concurrently) for the guarantee
 * that no keystore operation ever observes another one mid-flight —
 * the right tradeoff for a security-critical component, and in practice
 * a small cost given `AndroidKeyStore` operations already cross a Binder
 * IPC boundary to a system service.
 */
class SystemAndroidKeystoreKeyProvider : AndroidKeystoreKeyProvider {
    private val lock = Any()

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    override fun generateKey(
        alias: String,
        requireUserAuthentication: Boolean,
        userAuthenticationValiditySeconds: Int
    ): CryptoResult<KeystoreCapabilities> = synchronized(lock) {
        try {
            val strongBoxResult =
                tryGenerateKey(
                    alias = alias,
                    requireUserAuthentication = requireUserAuthentication,
                    userAuthenticationValiditySeconds = userAuthenticationValiditySeconds,
                    requestStrongBox = true
                )

            val generated =
                strongBoxResult ?: tryGenerateKey(
                    alias = alias,
                    requireUserAuthentication = requireUserAuthentication,
                    userAuthenticationValiditySeconds = userAuthenticationValiditySeconds,
                    requestStrongBox = false
                ) ?: return@synchronized CryptoError
                    .KeystoreFailure(
                        operation = "generateKey",
                        cause = IllegalStateException("Key generation failed with and without StrongBox")
                    ).asCryptoFailure()

            generated.asCryptoSuccess()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            CryptoError.KeystoreFailure("generateKey", t).asCryptoFailure()
        }
    }

    /**
     * Returns `null` (rather than throwing) specifically for
     * [StrongBoxUnavailableException], so the caller can fall back.
     * Only ever called while already holding [lock].
     */
    private fun tryGenerateKey(
        alias: String,
        requireUserAuthentication: Boolean,
        userAuthenticationValiditySeconds: Int,
        requestStrongBox: Boolean
    ): KeystoreCapabilities? {
        val specBuilder =
            KeyGenParameterSpec
                .Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setIsStrongBoxBacked(requestStrongBox)

        if (requireUserAuthentication) {
            specBuilder.setUserAuthenticationRequired(true)

            // setUserAuthenticationParameters (which lets us pin the
            // requirement to AUTH_BIOMETRIC_STRONG specifically) was added
            // in API 30. This project's minSdk is 29, so API 29 falls back
            // to the older, coarser API: it still requires *some* prior
            // authentication within the validity window, just without the
            // ability to require specifically a strong biometric.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    userAuthenticationValiditySeconds,
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                @Suppress("DEPRECATION")
                specBuilder.setUserAuthenticationValidityDurationSeconds(userAuthenticationValiditySeconds)
            }

            // A key that survives biometric re-enrollment (someone else's
            // fingerprint being added) would defeat the point of gating it
            // on biometrics at all.
            specBuilder.setInvalidatedByBiometricEnrollment(true)
        }

        return try {
            KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                .apply {
                    init(specBuilder.build())
                }.generateKey()
            readCapabilities(alias)
        } catch (_: StrongBoxUnavailableException) {
            null
        }
    }

    override fun hasKey(alias: String): CryptoResult<Boolean> = synchronized(lock) {
        try {
            keyStore.containsAlias(alias).asCryptoSuccess()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            CryptoError.KeystoreFailure("hasKey", t).asCryptoFailure()
        }
    }

    override fun deleteKey(alias: String): CryptoResult<Unit> = synchronized(lock) {
        try {
            keyStore.deleteEntry(alias)
            Unit.asCryptoSuccess()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            CryptoError.KeystoreFailure("deleteKey", t).asCryptoFailure()
        }
    }

    override fun capabilities(alias: String): CryptoResult<KeystoreCapabilities> = synchronized(lock) {
        try {
            if (!keyStore.containsAlias(alias)) {
                return@synchronized CryptoError.KeyUnavailable(alias).asCryptoFailure()
            }
            readCapabilities(alias).asCryptoSuccess()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            CryptoError.KeystoreFailure("capabilities", t).asCryptoFailure()
        }
    }

    /**
     * Reads back the security properties of an existing key. Callers
     * must have already confirmed the alias exists (both call sites —
     * right after [tryGenerateKey] generates one, and [capabilities]
     * after its own `containsAlias` check — do). Deliberately throws
     * rather than returning a [CryptoResult] itself: both call sites
     * already run inside their own `try`/`catch` that maps any
     * [Throwable] to a [CryptoError], so a second layer of result
     * wrapping here would just be dead code.
     */
    private fun readCapabilities(alias: String): KeystoreCapabilities {
        val key =
            keyStore.getKey(alias, null) as? SecretKey
                ?: error("Keystore alias '$alias' was expected to exist but its key was null or not a SecretKey")
        val factory = SecretKeyFactory.getInstance(key.algorithm, PROVIDER)
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return KeystoreCapabilities(
            isInsideSecureHardware = keyInfo.isInsideSecureHardware,
            isStrongBoxBacked = isStrongBoxBacked(keyInfo)
        )
    }

    /**
     * [KeyInfo.getSecurityLevel] (StrongBox detection) requires API 31+;
     * older API levels can only report TEE-vs-not.
     */
    private fun isStrongBoxBacked(keyInfo: KeyInfo): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
    } else {
        false
    }

    override fun encrypt(alias: String, plaintext: SecureBytes): CryptoResult<EncryptedField> = synchronized(lock) {
        try {
            val key =
                keyStore.getKey(alias, null) as? SecretKey
                    ?: return@synchronized CryptoError.KeyUnavailable(alias).asCryptoFailure()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val ciphertextAndTag = plaintext.peek { cipher.doFinal(it) }

            EncryptedField(
                ciphertext = Base64.getEncoder().encodeToString(ciphertextAndTag),
                nonce = Base64.getEncoder().encodeToString(cipher.iv),
                algorithm = EncryptedField.DEFAULT_ALGORITHM
            ).asCryptoSuccess()
        } catch (c: CancellationException) {
            throw c
        } catch (u: UserNotAuthenticatedException) {
            CryptoError.AuthenticationFailed("biometric", u).asCryptoFailure()
        } catch (t: Throwable) {
            CryptoError.KeystoreFailure("encrypt", t).asCryptoFailure()
        }
    }

    override fun decrypt(alias: String, encryptedField: EncryptedField): CryptoResult<SecureBytes> =
        synchronized(lock) {
            try {
                val key =
                    keyStore.getKey(alias, null) as? SecretKey
                        ?: return@synchronized CryptoError.KeyUnavailable(alias).asCryptoFailure()

                val nonceBytes = Base64.getDecoder().decode(encryptedField.nonce)
                val ciphertextAndTag = Base64.getDecoder().decode(encryptedField.ciphertext)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonceBytes))
                val plaintext = cipher.doFinal(ciphertextAndTag)

                SecureBytes.wrap(plaintext).asCryptoSuccess()
            } catch (c: CancellationException) {
                throw c
            } catch (u: UserNotAuthenticatedException) {
                CryptoError.AuthenticationFailed("biometric", u).asCryptoFailure()
            } catch (a: AEADBadTagException) {
                CryptoError.InvalidCiphertext(a).asCryptoFailure()
            } catch (t: Throwable) {
                CryptoError.KeystoreFailure("decrypt", t).asCryptoFailure()
            }
        }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_LENGTH_BITS = 128
    }
}
