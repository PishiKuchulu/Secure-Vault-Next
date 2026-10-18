package com.securevault.next.core.crypto.key

import com.securevault.next.core.crypto.memory.SecureBytes

/**
 * The four distinct key roles in SecureVault Next's key hierarchy, each
 * its own type specifically so the compiler — not just documentation —
 * stops a [MasterKey] from being passed somewhere a [VaultKey] is
 * required, or vice versa.
 *
 * The hierarchy, and why it has this shape (Milestone 3: "Master Key,
 * Vault Key, Session Key, Backup Key"):
 *
 * - [MasterKey]: derived directly from the user's password via Argon2id
 *   (`core.crypto.kdf.KeyDerivationEngine`). Used *only* to wrap/unwrap
 *   a [VaultKey] — it never touches actual vault content. This
 *   indirection is what makes changing the master password cheap: only
 *   the [VaultKey]'s wrapping needs to change, not every encrypted field
 *   in the vault (see `core.crypto.key.KeyRotationManager`).
 *
 * - [VaultKey]: a randomly generated AES-256 key (never derived from a
 *   password) that actually encrypts and decrypts vault content. Stored
 *   at rest only in its [MasterKey]-wrapped form; never written to disk
 *   in the clear.
 *
 * - [SessionKey]: the *specific in-memory instance* of a [VaultKey] held
 *   for the duration of one unlocked session (see [SessionKeyHolder]).
 *   Distinguishing this from [VaultKey] at the type level means a
 *   function requiring "the vault must currently be unlocked" can
 *   require a [SessionKey] parameter and have that requirement enforced
 *   by the type system, rather than by a runtime null-check on a
 *   [VaultKey] that might or might not currently be in memory.
 *
 * - [BackupKey]: structurally identical to a [VaultKey] but semantically
 *   distinct — it protects a specific backup artifact, which may be
 *   encrypted under a different passphrase than the live vault's master
 *   password (Part 4: Backup Center). Keeping it a separate type
 *   prevents a backup key from accidentally being reused to decrypt live
 *   vault content, or vice versa.
 */
@JvmInline
value class MasterKey(
    val bytes: SecureBytes,
) {
    fun destroy() = bytes.close()
}

@JvmInline
value class VaultKey(
    val bytes: SecureBytes,
) {
    fun destroy() = bytes.close()
}

@JvmInline
value class SessionKey(
    val bytes: SecureBytes,
) {
    fun destroy() = bytes.close()
}

@JvmInline
value class BackupKey(
    val bytes: SecureBytes,
) {
    fun destroy() = bytes.close()
}

/**
 * Wraps this [VaultKey]'s bytes as a [SessionKey] for the duration of an
 * unlocked session. Does not copy — the two share the same underlying
 * [SecureBytes].
 */
fun VaultKey.asSessionKey(): SessionKey = SessionKey(bytes)
