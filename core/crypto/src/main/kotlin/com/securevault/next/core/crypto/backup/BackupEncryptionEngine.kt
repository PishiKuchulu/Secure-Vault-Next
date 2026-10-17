package com.securevault.next.core.crypto.backup

import com.securevault.next.core.common.result.map
import com.securevault.next.core.crypto.format.EncryptedVaultEnvelope
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.key.BackupKey
import com.securevault.next.core.crypto.key.VaultKey
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.vault.VaultEncryptionEngine

/**
 * Creates and restores encrypted backup artifacts, protected by a
 * [BackupKey] rather than a live vault's [VaultKey].
 *
 * A backup reuses exactly the same on-disk shape as a live vault
 * ([EncryptedVaultEnvelope]: versioned KDF metadata, wrapped key,
 * integrity-protected header, encrypted content) — the two are
 * cryptographically identical constructions. What's different is the
 * *passphrase* protecting it: Part 4 (Backup Center) allows a backup to
 * be protected by a separate export passphrase rather than the vault's
 * live master password, and [BackupKey] exists as its own type
 * specifically so a backup key can never be accidentally substituted for
 * a live [VaultKey] at a call site, or vice versa, even though the two
 * are structurally identical `SecureBytes` wrappers underneath.
 *
 * **Ownership**: [createBackup] and [restoreBackup] both take ownership
 * of and wipe their `backupPassword` argument, matching
 * [VaultEncryptionEngine.createVault] / [VaultEncryptionEngine.unlockVault].
 */
interface BackupEncryptionEngine {

    /** Creates a new encrypted backup envelope protected by [backupPassword], containing [plaintextContent]. */
    fun createBackup(
        backupPassword: SecureBytes,
        plaintextContent: SecureBytes,
        kdfParameters: KdfParameters = KdfParameters.CURRENT,
    ): CryptoResult<BackupCreationResult>

    /**
     * Authenticates against a backup [envelope] with [backupPassword]
     * and unwraps its [BackupKey], without decrypting the content yet.
     */
    fun restoreBackup(envelope: EncryptedVaultEnvelope, backupPassword: SecureBytes): CryptoResult<BackupKey>

    /** Decrypts a backup [envelope]'s content using an already-unwrapped [backupKey] (from [restoreBackup]). */
    fun decryptBackupContent(envelope: EncryptedVaultEnvelope, backupKey: BackupKey): CryptoResult<SecureBytes>
}

/**
 * The result of [BackupEncryptionEngine.createBackup]: the persistable
 * envelope, plus the backup key ready for immediate use (e.g. to verify
 * the backup by decrypting it right after creation).
 */
data class BackupCreationResult(
    val envelope: EncryptedVaultEnvelope,
    val backupKey: BackupKey,
)

/**
 * The production [BackupEncryptionEngine].
 *
 * Deliberately implemented by *delegating* to a [VaultEncryptionEngine]
 * rather than duplicating its KDF/cipher/integrity orchestration a
 * second time — a backup and a live vault use identical cryptographic
 * construction, so the only thing this class does is translate between
 * [VaultKey] and [BackupKey] at the boundary, keeping one single,
 * well-tested implementation of the actual envelope create/unlock logic.
 */
class DefaultBackupEncryptionEngine(
    private val vaultEncryptionEngine: VaultEncryptionEngine,
) : BackupEncryptionEngine {

    override fun createBackup(
        backupPassword: SecureBytes,
        plaintextContent: SecureBytes,
        kdfParameters: KdfParameters,
    ): CryptoResult<BackupCreationResult> =
        vaultEncryptionEngine.createVault(backupPassword, plaintextContent, kdfParameters)
            .map { result ->
                BackupCreationResult(envelope = result.envelope, backupKey = BackupKey(result.vaultKey.bytes))
            }

    override fun restoreBackup(envelope: EncryptedVaultEnvelope, backupPassword: SecureBytes): CryptoResult<BackupKey> =
        vaultEncryptionEngine.unlockVault(envelope, backupPassword).map { BackupKey(it.bytes) }

    override fun decryptBackupContent(
        envelope: EncryptedVaultEnvelope,
        backupKey: BackupKey,
    ): CryptoResult<SecureBytes> =
        vaultEncryptionEngine.decryptContent(envelope, VaultKey(backupKey.bytes))
}
