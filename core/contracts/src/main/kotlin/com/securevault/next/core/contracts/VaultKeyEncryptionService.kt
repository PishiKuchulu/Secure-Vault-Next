package com.securevault.next.core.contracts

import com.securevault.next.core.domain.result.DomainResult

/**
 * The result of [VaultKeyEncryptionService.createEnvelope]: the serialized
 * encrypted envelope (an opaque [String], the same shape it is persisted in)
 * plus the freshly generated vault key bytes.
 *
 * Deliberately a plain value holder exposing only [String] and [ByteArray] —
 * never a crypto type. The caller (the create-vault use case) persists the
 * [envelopeJson] and hands the [vaultKeyBytes] to the session layer.
 */
data class EnvelopeAndKey(
    val envelopeJson: String,
    val vaultKeyBytes: ByteArray,
)

/**
 * The pure-JVM seam between vault *orchestration* (`core:vault`) and vault
 * *cryptography* (`core:crypto`).
 *
 * This abstraction lives in the shared `core:contracts` module so both the
 * business orchestration (`core:vault`) and the cryptographic implementation
 * (`core:crypto`) can depend on it without depending on each other — the
 * classic dependency-inversion arrangement. `core:vault` deliberately does
 * not depend on `core:crypto`, and `core:crypto` deliberately does not
 * depend on `core:vault`; they meet here.
 *
 * The interface exposes **only** [CharArray], [String], [ByteArray] and
 * [DomainResult]. No [com.securevault.next.core.crypto.key.VaultKey],
 * [com.securevault.next.core.crypto.format.EncryptedVaultEnvelope],
 * [com.securevault.next.core.crypto.result.CryptoResult],
 * [com.securevault.next.core.crypto.memory.SecureBytes] or
 * [com.securevault.next.core.crypto.error.CryptoError] type leaks across
 * this boundary. `core:crypto` details stay entirely inside the adapter
 * (`DefaultVaultKeyEncryptionService`).
 *
 * **Ownership:** both methods take ownership of [masterPassword] and wipe it
 * before returning, on every path. The caller must not read from or reuse the
 * [CharArray] after the call. The adapter also wipes any temporary key bytes
 * once transferred.
 */
interface VaultKeyEncryptionService {
    /**
     * Creates a new encrypted envelope protecting a freshly generated vault
     * key, derived from [masterPassword]. Returns the serialized envelope
     * plus the unwrapped vault key bytes.
     *
     * The caller persists [EnvelopeAndKey.envelopeJson] and hands
     * [EnvelopeAndKey.vaultKeyBytes] to the session layer. Ownership of the
     * returned key bytes transfers to the caller; the caller is responsible
     * for wiping them once installed.
     */
    fun createEnvelope(masterPassword: CharArray): DomainResult<EnvelopeAndKey>

    /**
     * Unwraps the vault key from an already-persisted [envelopeJson] using
     * [masterPassword]. Returns the vault key bytes on success, or a domain
     * `AuthenticationError.InvalidCredentials` when the password is wrong.
     */
    fun unwrapVaultKey(
        envelopeJson: String,
        masterPassword: CharArray,
    ): DomainResult<ByteArray>

    /**
     * Re-wraps the vault key under a new master password: verifies
     * [currentPassword] (returning `AuthenticationError.InvalidCredentials`
     * when it is wrong), then re-derives and re-wraps, returning the new
     * serialized envelope. The vault key itself is unchanged, so an active
     * session stays valid.
     *
     * Takes ownership of **both** password arrays and wipes them before
     * returning, on every path. The caller must not read from or reuse
     * either [CharArray] after the call.
     */
    fun rotateMasterPassword(
        envelopeJson: String,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): DomainResult<String>
}
