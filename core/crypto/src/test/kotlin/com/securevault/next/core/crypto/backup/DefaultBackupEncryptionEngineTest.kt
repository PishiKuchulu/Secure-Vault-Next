package com.securevault.next.core.crypto.backup

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.isSuccess
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

class DefaultBackupEncryptionEngineTest {
    private lateinit var backupEngine: DefaultBackupEncryptionEngine

    private val fastKdfParameters =
        KdfParameters(
            memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
            iterations = 1,
            parallelism = 1,
            outputLengthBytes = 64
        )

    @Before
    fun setUp() {
        val randomProvider = SystemSecureRandomProvider()
        val vaultEngine =
            DefaultVaultEncryptionEngine(
                keyDerivationEngine = Argon2idKeyDerivationEngine(),
                cipherEngine = AesGcmCipherEngine(randomProvider),
                integrityVerifier = HmacSha256IntegrityVerifier(),
                secureRandomProvider = randomProvider
            )
        backupEngine = DefaultBackupEncryptionEngine(vaultEngine)
    }

    private fun bytesOf(text: String) = SecureBytes.wrap(text.toByteArray(Charsets.UTF_8))

    private fun <T> CryptoResult<T>.successValue(): T {
        check(this is Result.Success) { "Expected success but was $this" }
        return value
    }

    @Test
    fun `creating a backup and restoring it with the correct passphrase recovers the content`() {
        val creation =
            backupEngine
                .createBackup(
                    bytesOf("backup-export-passphrase"),
                    bytesOf("vault contents to back up"),
                    fastKdfParameters
                ).successValue()

        val backupKey =
            backupEngine
                .restoreBackup(creation.envelope, bytesOf("backup-export-passphrase"))
                .successValue()
        val decrypted = backupEngine.decryptBackupContent(creation.envelope, backupKey).successValue()

        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("vault contents to back up")
    }

    @Test
    fun `a backup can use a different passphrase than the live vault's master password`() {
        // The whole point of a separate BackupKey type: the export
        // passphrase need not match — and here deliberately does not
        // match — anything used to protect the live vault.
        val creation =
            backupEngine
                .createBackup(
                    bytesOf("totally-different-export-passphrase"),
                    bytesOf("contents"),
                    fastKdfParameters
                ).successValue()

        val result = backupEngine.restoreBackup(creation.envelope, bytesOf("totally-different-export-passphrase"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `restoring a backup with the wrong passphrase fails as InvalidPassword`() {
        val creation =
            backupEngine
                .createBackup(bytesOf("correct-passphrase"), bytesOf("contents"), fastKdfParameters)
                .successValue()

        val result = backupEngine.restoreBackup(creation.envelope, bytesOf("wrong-passphrase"))

        check(result is Result.Failure)
        assertThat(result.error).isEqualTo(CryptoError.InvalidPassword)
    }

    @Test
    fun `a corrupted backup envelope fails restoration`() {
        val creation =
            backupEngine
                .createBackup(bytesOf("correct-passphrase"), bytesOf("contents"), fastKdfParameters)
                .successValue()

        val corrupted =
            creation.envelope.copy(
                integrityMetadata = creation.envelope.integrityMetadata.copy(mac = "not-valid-base64-!!!")
            )

        val result = backupEngine.restoreBackup(corrupted, bytesOf("correct-passphrase"))

        check(result is Result.Failure)
        assertThat(result.error).isInstanceOf(CryptoError.VaultCorrupted::class.java)
    }
}
