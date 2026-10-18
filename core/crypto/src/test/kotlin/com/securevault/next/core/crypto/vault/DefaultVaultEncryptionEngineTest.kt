package com.securevault.next.core.crypto.vault

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.crypto.cipher.AesGcmCipherEngine
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.format.VaultFormatVersion
import com.securevault.next.core.crypto.integrity.HmacSha256IntegrityVerifier
import com.securevault.next.core.crypto.kdf.Argon2idKeyDerivationEngine
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import org.junit.Before
import org.junit.Test

class DefaultVaultEncryptionEngineTest {
    private lateinit var engine: DefaultVaultEncryptionEngine

    /**
     * Real Argon2id at the fastest cost this codebase allows itself to
     * accept (the OWASP floor, 1 iteration) so the suite runs quickly
     * without mocking anything. [strongerKdfParameters] is used
     * specifically by the downgrade-attack test below, where the test
     * needs an *original* set of parameters that a "weaker" tampered
     * value can be meaningfully different from.
     */
    private val fastKdfParameters =
        KdfParameters(
            memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
            iterations = 1,
            parallelism = 1,
            outputLengthBytes = 64
        )

    private val strongerKdfParameters = fastKdfParameters.copy(iterations = 2)

    @Before
    fun setUp() {
        val randomProvider = SystemSecureRandomProvider()
        engine =
            DefaultVaultEncryptionEngine(
                keyDerivationEngine = Argon2idKeyDerivationEngine(),
                cipherEngine = AesGcmCipherEngine(randomProvider),
                integrityVerifier = HmacSha256IntegrityVerifier(),
                secureRandomProvider = randomProvider
            )
    }

    private fun bytesOf(text: String) = SecureBytes.wrap(text.toByteArray(Charsets.UTF_8))

    private fun <T> CryptoResult<T>.successValue(): T {
        check(this is Result.Success) { "Expected success but was $this" }
        return value
    }

    private fun <T> CryptoResult<T>.failureError(): CryptoError {
        check(this is Result.Failure) { "Expected failure but was $this" }
        return error
    }

    @Test
    fun `creating and then unlocking a vault with the correct password recovers the original content`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("my secret vault contents"),
                    fastKdfParameters
                ).successValue()

        val vaultKey = engine.unlockVault(creation.envelope, bytesOf("correct horse battery staple")).successValue()
        val decrypted = engine.decryptContent(creation.envelope, vaultKey).successValue()

        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("my secret vault contents")
    }

    @Test
    fun `unlocking with the wrong password fails as InvalidPassword`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("contents"),
                    fastKdfParameters
                ).successValue()

        val result = engine.unlockVault(creation.envelope, bytesOf("wrong password entirely"))

        assertThat(result.failureError()).isEqualTo(CryptoError.InvalidPassword)
    }

    @Test
    fun `tampering with the KDF metadata (a downgrade attack) is detected and fails as InvalidPassword`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("contents"),
                    strongerKdfParameters
                ).successValue()
        check(creation.envelope.kdfMetadata.iterations == 2)

        // Simulate an attacker weakening the recorded cost parameters to
        // make offline brute-forcing cheaper, without touching the salt or
        // wrapped key.
        val tamperedEnvelope =
            creation.envelope.copy(
                kdfMetadata = creation.envelope.kdfMetadata.copy(iterations = 1)
            )

        val result = engine.unlockVault(tamperedEnvelope, bytesOf("correct horse battery staple"))

        assertThat(result.failureError()).isEqualTo(CryptoError.InvalidPassword)
    }

    @Test
    fun `tampering with the wrapped vault key metadata is detected and fails`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("contents"),
                    fastKdfParameters
                ).successValue()

        val tamperedEnvelope =
            creation.envelope.copy(
                encryptionMetadata =
                creation.envelope.encryptionMetadata.copy(
                    wrappedVaultKey =
                    creation.envelope.encryptionMetadata.wrappedVaultKey.copy(
                        ciphertext = "dGFtcGVyZWQtY2lwaGVydGV4dC1ieXRlcw=="
                    )
                )
            )

        val result = engine.unlockVault(tamperedEnvelope, bytesOf("correct horse battery staple"))

        assertThat(result.failureError()).isEqualTo(CryptoError.InvalidPassword)
    }

    @Test
    fun `tampering with the encrypted content ciphertext fails decryption after a successful unlock`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("contents"),
                    fastKdfParameters
                ).successValue()

        val vaultKey = engine.unlockVault(creation.envelope, bytesOf("correct horse battery staple")).successValue()

        val tamperedEnvelope =
            creation.envelope.copy(
                encryptedContent =
                creation.envelope.encryptedContent.copy(
                    ciphertext = "dGFtcGVyZWQtY29udGVudC1ieXRlcw=="
                )
            )

        val result = engine.decryptContent(tamperedEnvelope, vaultKey)

        assertThat(result.failureError()).isInstanceOf(CryptoError.InvalidCiphertext::class.java)
    }

    @Test
    fun `an unsupported format version is rejected before any derivation is attempted`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("contents"),
                    fastKdfParameters
                ).successValue()

        val futureVersionEnvelope = creation.envelope.copy(formatVersion = VaultFormatVersion(999))

        val result = engine.unlockVault(futureVersionEnvelope, bytesOf("correct horse battery staple"))

        assertThat(result.failureError()).isEqualTo(
            CryptoError.UnsupportedVersion(found = 999, supportedRange = VaultFormatVersion.SUPPORTED_RANGE)
        )
    }

    @Test
    fun `reEncryptContent updates the content under the same vault key without changing the key wrapping`() {
        val creation =
            engine
                .createVault(
                    bytesOf("correct horse battery staple"),
                    bytesOf("original contents"),
                    fastKdfParameters
                ).successValue()

        val updatedEnvelope =
            engine
                .reEncryptContent(
                    creation.envelope,
                    creation.vaultKey,
                    bytesOf("updated contents")
                ).successValue()

        // The key wrapping and KDF metadata are untouched by a content-only re-encryption.
        assertThat(updatedEnvelope.encryptionMetadata).isEqualTo(creation.envelope.encryptionMetadata)
        assertThat(updatedEnvelope.kdfMetadata).isEqualTo(creation.envelope.kdfMetadata)

        val decrypted = engine.decryptContent(updatedEnvelope, creation.vaultKey).successValue()
        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("updated contents")
    }

    @Test
    fun `each vault gets a unique random salt even with identical passwords and content`() {
        val first =
            engine
                .createVault(bytesOf("same-password"), bytesOf("same-content"), fastKdfParameters)
                .successValue()
        val second =
            engine
                .createVault(bytesOf("same-password"), bytesOf("same-content"), fastKdfParameters)
                .successValue()

        assertThat(first.envelope.kdfMetadata.salt).isNotEqualTo(second.envelope.kdfMetadata.salt)
        assertThat(first.envelope.encryptedContent.nonce).isNotEqualTo(second.envelope.encryptedContent.nonce)
    }
}
