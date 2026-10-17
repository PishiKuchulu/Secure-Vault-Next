package com.securevault.next.core.crypto.vault

import com.securevault.next.core.crypto.format.EncryptedVaultEnvelope
import com.securevault.next.core.crypto.kdf.KdfParameters
import com.securevault.next.core.crypto.key.VaultKey
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult

/**
 * Creates and unlocks whole encrypted vaults, orchestrating the key
 * derivation, cipher and integrity layers into the two operations that
 * actually matter to a caller: "make a new encrypted vault from a
 * password and some plaintext" and "given an encrypted vault and a
 * password, get the plaintext back (or find out the password was
 * wrong)".
 *
 * See `core.crypto.key.Keys.kt` for the master-key/vault-key indirection
 * this engine implements, and `core.crypto.integrity.IntegrityVerifier`
 * for why the envelope's metadata carries a MAC.
 *
 * **Ownership**: [createVault] and [unlockVault] both take ownership of
 * their `masterPassword` argument and wipe it before returning, on every
 * code path. Callers must not read from or reuse a `SecureBytes` passed
 * as `masterPassword` after either call returns.
 */
interface VaultEncryptionEngine {

    /**
     * Creates a brand-new encrypted vault: derives a master key from
     * [masterPassword] under [kdfParameters], generates a fresh random
     * vault key, encrypts [plaintextContent] under it, and wraps the
     * vault key under the derived master key.
     *
     * Returns both the resulting [EncryptedVaultEnvelope] (to persist)
     * and the freshly generated [VaultKey] (so the caller can install it
     * as the active session key immediately, without needing to
     * separately unlock the vault it just created).
     */
    fun createVault(
        masterPassword: SecureBytes,
        plaintextContent: SecureBytes,
        kdfParameters: KdfParameters = KdfParameters.CURRENT,
    ): CryptoResult<VaultCreationResult>

    /**
     * Attempts to unlock [envelope] with [masterPassword]: re-derives the
     * master key using the envelope's own recorded KDF parameters,
     * verifies the envelope's integrity MAC (failing closed as
     * `CryptoError.InvalidPassword` — see that error case's own
     * documentation for why a MAC mismatch here is reported as a wrong
     * password rather than a tampering alert), and unwraps the vault
     * key.
     *
     * Does *not* decrypt [EncryptedVaultEnvelope.encryptedContent] —
     * call [decryptContent] with the returned [VaultKey] once unlock
     * succeeds. Keeping these separate means a caller can validate a
     * password (e.g. before a destructive action) without paying the
     * cost of decrypting the entire vault content.
     */
    fun unlockVault(envelope: EncryptedVaultEnvelope, masterPassword: SecureBytes): CryptoResult<VaultKey>

    /** Decrypts [envelope]'s content using an already-unwrapped [vaultKey] (from [unlockVault]). */
    fun decryptContent(envelope: EncryptedVaultEnvelope, vaultKey: VaultKey): CryptoResult<SecureBytes>

    /**
     * Re-encrypts [plaintextContent] under [vaultKey] and produces an
     * updated [EncryptedVaultEnvelope] that reuses [envelope]'s existing
     * key wrapping and KDF metadata unchanged — the operation a save
     * after editing vault contents performs. Never re-derives the master
     * key or re-wraps the vault key; use `core.crypto.key.KeyRotationManager`
     * for that.
     */
    fun reEncryptContent(
        envelope: EncryptedVaultEnvelope,
        vaultKey: VaultKey,
        plaintextContent: SecureBytes,
    ): CryptoResult<EncryptedVaultEnvelope>
}

/**
 * The result of [VaultEncryptionEngine.createVault]: the persistable
 * envelope, plus the vault key ready for immediate use.
 */
data class VaultCreationResult(
    val envelope: EncryptedVaultEnvelope,
    val vaultKey: VaultKey,
)
