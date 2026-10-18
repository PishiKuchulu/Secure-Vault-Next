package com.securevault.next.core.crypto.key

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.crypto.cipher.AesGcmCipherEngine
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.integrity.HmacSha256IntegrityVerifier
import com.securevault.next.core.crypto.kdf.Argon2idKeyDerivationEngine
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.vault.DefaultVaultEncryptionEngine
import org.junit.Before
import org.junit.Test

class DefaultKeyRotationManagerTest {
    private lateinit var vaultEngine: DefaultVaultEncryptionEngine
    private lateinit var rotationManager: DefaultKeyRotationManager

    private val fastKdfParameters =
        KdfParameters(
            memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
            iterations = 1,
            parallelism = 1,
            outputLengthBytes = 64,
        )

    @Before
    fun setUp() {
        val randomProvider = SystemSecureRandomProvider()
        val kdf = Argon2idKeyDerivationEngine()
        val cipher = AesGcmCipherEngine(randomProvider)
        val integrity = HmacSha256IntegrityVerifier()

        vaultEngine = DefaultVaultEncryptionEngine(kdf, cipher, integrity, randomProvider)
        rotationManager = DefaultKeyRotationManager(kdf, cipher, integrity, randomProvider)
    }

    private fun bytesOf(text: String) = SecureBytes.wrap(text.toByteArray(Charsets.UTF_8))

    private fun <T> CryptoResult<T>.successValue(): T {
        check(this is Result.Success) { "Expected success but was $this" }
        return value
    }

    @Test
    fun `after rotating the master password, the old password no longer unlocks the vault`() {
        val creation =
            vaultEngine
                .createVault(bytesOf("old-password"), bytesOf("contents"), fastKdfParameters)
                .successValue()

        val rotated =
            rotationManager
                .rotateMasterKey(
                    creation.envelope,
                    creation.vaultKey,
                    bytesOf("new-password"),
                    fastKdfParameters,
                ).successValue()

        val result = vaultEngine.unlockVault(rotated, bytesOf("old-password"))

        check(result is Result.Failure)
        assertThat(result.error).isEqualTo(CryptoError.InvalidPassword)
    }

    @Test
    fun `after rotating the master password, the new password unlocks the vault and recovers the same content`() {
        val creation =
            vaultEngine
                .createVault(bytesOf("old-password"), bytesOf("original contents"), fastKdfParameters)
                .successValue()

        val rotated =
            rotationManager
                .rotateMasterKey(
                    creation.envelope,
                    creation.vaultKey,
                    bytesOf("new-password"),
                    fastKdfParameters,
                ).successValue()

        val vaultKeyAfterRotation = vaultEngine.unlockVault(rotated, bytesOf("new-password")).successValue()
        val decrypted = vaultEngine.decryptContent(rotated, vaultKeyAfterRotation).successValue()

        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("original contents")
    }

    @Test
    fun `rotation changes the salt but not the encrypted content`() {
        val creation =
            vaultEngine
                .createVault(bytesOf("old-password"), bytesOf("contents"), fastKdfParameters)
                .successValue()

        val rotated =
            rotationManager
                .rotateMasterKey(
                    creation.envelope,
                    creation.vaultKey,
                    bytesOf("new-password"),
                    fastKdfParameters,
                ).successValue()

        assertThat(rotated.kdfMetadata.salt).isNotEqualTo(creation.envelope.kdfMetadata.salt)
        // The content ciphertext is untouched — rotation never re-encrypts vault content.
        assertThat(rotated.encryptedContent).isEqualTo(creation.envelope.encryptedContent)
    }

    @Test
    fun `the original vault key held by the caller remains usable after rotation`() {
        val creation =
            vaultEngine
                .createVault(bytesOf("old-password"), bytesOf("contents"), fastKdfParameters)
                .successValue()

        rotationManager
            .rotateMasterKey(
                creation.envelope,
                creation.vaultKey,
                bytesOf("new-password"),
                fastKdfParameters,
            ).successValue()

        // Rotation must not have wiped the caller's own copy of the vault key.
        val decrypted = vaultEngine.decryptContent(creation.envelope, creation.vaultKey).successValue()
        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("contents")
    }
}
